package com.nhnacademy.library.core.book.service.search;

import com.nhnacademy.library.core.book.domain.SearchType;
import com.nhnacademy.library.core.book.dto.BookSearchRequest;
import com.nhnacademy.library.core.book.dto.BookSearchResult;
import com.nhnacademy.library.core.book.dto.BookViewResponse;
import com.nhnacademy.library.core.book.exception.BookNotFoundException;
import com.nhnacademy.library.core.book.repository.BookRepository;
import com.nhnacademy.library.core.book.service.embedding.EmbeddingService;
import com.nhnacademy.library.core.book.service.search.strategy.HybridSearchStrategy;
import com.nhnacademy.library.core.book.service.search.strategy.KeywordSearchStrategy;
import com.nhnacademy.library.core.book.service.search.strategy.RagSearchStrategy;
import com.nhnacademy.library.core.book.service.search.strategy.SearchStrategy;
import com.nhnacademy.library.core.book.service.search.strategy.VectorSearchStrategy;
import com.nhnacademy.library.core.review.domain.BookReview;
import com.nhnacademy.library.core.review.repository.BookReviewRepository;
import com.nhnacademy.library.core.review.service.ReviewSummarizer;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
    private final ReviewSummarizer reviewSummarizer;
    
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
     * 도서 리뷰 요약을 생성합니다.
     *
     * @param bookId 도서 ID
     * @return 요약된 리뷰 텍스트
     */
    public String getReviewSummary(Long bookId) {
        log.info("Generating review summary for book id: {}", bookId);
        List<String> reviews = bookReviewRepository.findAllByBookId(bookId).stream()
                .map(BookReview::getContent)
                .toList();
        
        return reviewSummarizer.summarizeReviews(reviews);
    }
}
