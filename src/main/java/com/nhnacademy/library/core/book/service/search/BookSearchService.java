package com.nhnacademy.library.core.book.service.search;

import com.nhnacademy.library.core.book.domain.SearchType;
import com.nhnacademy.library.core.book.dto.BookSearchRequest;
import com.nhnacademy.library.core.book.dto.BookSearchResponse;
import com.nhnacademy.library.core.book.dto.BookSearchResult;
import com.nhnacademy.library.core.book.dto.BookViewResponse;
import com.nhnacademy.library.core.book.exception.BookNotFoundException;
import com.nhnacademy.library.core.book.repository.BookRepository;
import com.nhnacademy.library.core.book.service.embedding.EmbeddingService;
import com.nhnacademy.library.core.book.service.personalization.PersonalizationService;
import com.nhnacademy.library.core.book.service.search.strategy.HybridSearchStrategy;
import com.nhnacademy.library.core.book.service.search.strategy.KeywordSearchStrategy;
import com.nhnacademy.library.core.book.service.search.strategy.RagSearchStrategy;
import com.nhnacademy.library.core.book.service.search.strategy.SearchStrategy;
import com.nhnacademy.library.core.book.service.search.strategy.VectorSearchStrategy;
import com.nhnacademy.library.core.review.domain.BookReview;
import com.nhnacademy.library.core.review.domain.BookReviewSummary;
import com.nhnacademy.library.core.review.repository.BookReviewRepository;
import com.nhnacademy.library.core.review.repository.BookReviewSummaryRepository;
import com.nhnacademy.library.core.review.service.ReviewSummarizer;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 도서 검색 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookSearchService {

    private final BookRepository bookRepository;
    private final EmbeddingService embeddingService;
    private final BookReviewRepository bookReviewRepository;
    private final BookReviewSummaryRepository bookReviewSummaryRepository;
    private final ReviewSummarizer reviewSummarizer;
    private final PersonalizationService personalizationService;

    // 전략 구현체들
    private final KeywordSearchStrategy keywordSearchStrategy;
    private final VectorSearchStrategy vectorSearchStrategy;
    private final HybridSearchStrategy hybridSearchStrategy;
    private final RagSearchStrategy ragSearchStrategy;

    /**
     * 조건에 맞는 도서를 검색하여 페이징된 결과를 반환합니다.
     *
     * @param pageable 페이징 정보
     * @param request  검색 조건
     * @return 페이징된 도서 검색 결과
     */
    @Transactional(readOnly = true)
    public BookSearchResult searchBooks(Pageable pageable, BookSearchRequest request) {
        log.info("Searching books with request: {}, pageable: {}", request, pageable);

        request = ensureEmbedding(request);

        SearchStrategy strategy = selectStrategy(request.searchType());
        return strategy.search(pageable, request);
    }

    /**
     * 조건에 맞는 도서를 검색하여 개인화된 순위로 반환합니다.
     *
     * <p>사용자의 피드백 데이터를 기반으로 선호도를 학습하고,
     * 검색 결과에 개인화된 순위를 적용합니다.</p>
     *
     * @param pageable 페이징 정보
     * @param request  검색 조건
     * @param chatId   사용자 chatId (개인화용)
     * @return 페이징된 도서 검색 결과 (개인화된 순위)
     */
    @Transactional(readOnly = true)
    public BookSearchResult searchBooks(Pageable pageable, BookSearchRequest request, Long chatId) {
        log.info("Searching books with personalization for chatId: {}", chatId);

        // 1. 기존 검색 실행
        BookSearchResult result = searchBooks(pageable, request);

        // 2. 개인화 적용
        List<BookSearchResponse> personalizedBooks = personalizationService.personalizedSearch(
            result.getBooks().getContent(),
            chatId
        );

        // 3. 결과 재구성
        return new BookSearchResult(
            new PageImpl<>(
                personalizedBooks,
                pageable,
                result.getBooks().getTotalElements()
            ),
            result.getAiResponse()
        );
    }

    private SearchStrategy selectStrategy(SearchType searchType) {
        return switch (searchType) {
            case KEYWORD -> keywordSearchStrategy;
            case VECTOR -> vectorSearchStrategy;
            case HYBRID -> hybridSearchStrategy;
            case RAG -> ragSearchStrategy;
        };
    }

    private BookSearchRequest ensureEmbedding(BookSearchRequest request) {
        if (shouldGenerateEmbedding(request)) {
            float[] vector = embeddingService.getEmbedding(request.keyword());
            return new BookSearchRequest(request.keyword(), request.isbn(), request.searchType(), vector, request.isWarmUp());
        }
        return request;
    }

    private boolean shouldGenerateEmbedding(BookSearchRequest request) {
        return (request.searchType() == SearchType.VECTOR || request.searchType() == SearchType.HYBRID || request.searchType() == SearchType.RAG)
                && request.keyword() != null && !request.keyword().isBlank() && request.vector() == null;
    }

    /**
     * 도서 상세 정보를 조회합니다.
     *
     * @param id 도서 ID
     * @return 도서 상세 정보
     * @throws BookNotFoundException 도서를 찾을 수 없는 경우
     */
    public BookViewResponse getBook(Long id) {
        log.info("Fetching book details for id: {}", id);
        return bookRepository.findById(id)
                .map(BookViewResponse::from)
                .orElseThrow(() -> new BookNotFoundException(id));
    }

    /**
     * 도서 리뷰 요약을 조회합니다.
     * DB에 저장된 요약을 즉시 반환하며, 요약이 없는 경우에만 대체 텍스트를 반환합니다.
     * (요약 생성은 리뷰 등록 시 이벤트 기반으로 비동기 처리됨)
     *
     * @param bookId 도서 ID
     * @return 요약된 리뷰 텍스트
     */
    public String getReviewSummary(Long bookId) {
        log.info("Fetching review summary for book id: {}", bookId);
        
        return bookReviewSummaryRepository.findById(bookId)
                .map(summary -> {
                    if (summary.getReviewSummary() != null) {
                        return summary.getReviewSummary();
                    }
                    return "리뷰가 모이고 있습니다. 곧 AI 요약이 제공될 예정입니다. (현재 " + summary.getReviewCount() + "개)";
                })
                .orElse("아직 등록된 리뷰가 없습니다. 첫 번째 리뷰를 남겨보세요!");
    }

    // 기존 동시성 대응 로직은 더 이상 필요하지 않으므로 제거 가능 (이벤트 핸들러에서 처리)
    // saveSummaryInNewTransaction 메서드 삭제 가능 여부는 다른 곳에서 사용되는지 확인 후 결정

    /**
     * 도서 리뷰 요약 정보를 엔티티 형태로 조회합니다.
     *
     * @param bookId 도서 ID
     * @return 리뷰 요약 정보 엔티티
     */
    public Optional<BookReviewSummary> getReviewSummaryEntity(Long bookId) {
        return bookReviewSummaryRepository.findById(bookId);
    }
}
