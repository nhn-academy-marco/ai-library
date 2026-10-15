package net.java21.library.core.book.service.search;

import net.java21.library.core.book.domain.BrowseSort;

import net.java21.library.core.book.domain.SearchType;
import net.java21.library.core.book.dto.BookAiRecommendationResponse;
import net.java21.library.core.book.dto.BookSearchRequest;
import net.java21.library.core.book.dto.BookSearchResponse;
import net.java21.library.core.book.dto.BookSearchResult;
import net.java21.library.core.book.dto.BookViewResponse;
import net.java21.library.core.book.exception.BookNotFoundException;
import net.java21.library.core.book.repository.BookRepository;
import net.java21.library.core.book.service.cache.SearchResultCache;
import net.java21.library.core.book.service.embedding.EmbeddingService;
import net.java21.library.core.book.service.personalization.PersonalizationService;
import net.java21.library.core.book.service.search.strategy.HybridSearchStrategy;
import net.java21.library.core.book.service.search.strategy.KeywordSearchStrategy;
import net.java21.library.core.book.service.search.strategy.RagSearchStrategy;
import net.java21.library.core.book.service.search.strategy.SearchStrategy;
import net.java21.library.core.book.service.search.strategy.VectorSearchStrategy;
import net.java21.library.core.review.domain.BookReview;
import net.java21.library.core.review.domain.BookReviewSummary;
import net.java21.library.core.review.repository.BookReviewRepository;
import net.java21.library.core.review.repository.BookReviewSummaryRepository;
import net.java21.library.core.review.service.ReviewSummarizer;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 도서 검색 서비스
 *
 * <p>검색({@link #searchBooks})은 트랜잭션을 열지 않습니다. 트랜잭션은 DB를 실제로 조회하는 전략이 필요한 범위만큼 엽니다
 * (키워드·벡터 전략은 메서드 단위, 하이브리드는 후보 조회 스레드마다). 바깥에서 커넥션을 쥔 채 하이브리드 병렬 조회가
 * 커넥션을 2개 더 빌리거나, RAG 웜업이 LLM 응답을 기다리는 동안 커넥션을 붙잡지 않게 하기 위해서입니다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookSearchService {

    private final BookRepository bookRepository;
    private final EmbeddingService embeddingService;
    private final BookReviewRepository bookReviewRepository;
    private final BookReviewSummaryRepository bookReviewSummaryRepository;
    private final ReviewSummarizer reviewSummarizer;
    private final PersonalizationService personalizationService;
    private final SearchResultCache searchResultCache;

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
    public BookSearchResult searchBooks(Pageable pageable, BookSearchRequest request) {
        log.info("Searching books with request: {}, pageable: {}", request, pageable);

        // RAG는 목록을 하이브리드 결과 캐시와 공유하고 시맨틱 캐시 웜업에도 검색어를 넘기므로 같은 규칙으로 정규화합니다.
        if (request.searchType() == SearchType.RAG) {
            request = new BookSearchRequest(SearchResultCache.normalizeKeyword(request.keyword()), request.isbn(),
                    request.searchType(), request.vector(), request.isWarmUp());
        }

        // 키워드·벡터·하이브리드는 짧은 TTL 결과 캐시를 먼저 확인 (성능용 보조, 결정성은 정렬 규칙이 보장)
        if (searchResultCache.isCacheable(pageable, request)) {
            BookSearchRequest normalized = new BookSearchRequest(
                    SearchResultCache.normalizeKeyword(request.keyword()), request.isbn(),
                    request.searchType(), null, request.isWarmUp());
            Optional<BookSearchResult> cached = searchResultCache.get(pageable, normalized);
            if (cached.isPresent()) {
                return cached.get();
            }
            BookSearchResult result = executeSearch(pageable, normalized);
            searchResultCache.put(pageable, normalized, result);
            return result;
        }
        return executeSearch(pageable, request);
    }

    private BookSearchResult executeSearch(Pageable pageable, BookSearchRequest request) {
        try {
            try {
                request = ensureEmbedding(request);
            } catch (RuntimeException e) {
                // 임베딩 서버에 연결할 수 없으면 의미·하이브리드·RAG 검색이 모두 불가능하므로
                // 오류 화면 대신 키워드 검색 결과로 대체합니다.
                log.warn("Embedding unavailable for '{}'. Falling back to keyword search: {}", request.keyword(), e.getMessage());
                BookSearchRequest keywordRequest = new BookSearchRequest(
                        request.keyword(), request.isbn(), SearchType.KEYWORD, null, request.isWarmUp());
                return BookSearchResult.keywordFallback(keywordSearchStrategy.search(pageable, keywordRequest).getBooks());
            }

            SearchStrategy strategy = selectStrategy(request.searchType());
            return strategy.search(pageable, request);
        } finally {
            SearchTimings.clear();
        }
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

    /**
     * RAG 검색의 AI 추천을 시맨틱 캐시에서 조회합니다. LLM을 호출하지 않습니다(화면 폴링용).
     *
     * <p>검색어 임베딩은 임베딩 캐시에서 가져오므로 검색 직후의 폴링은 임베딩 서버를 다시 부르지 않습니다.</p>
     *
     * @param keyword 검색어
     * @return 준비된 AI 추천, 아직 생성 중이면 empty
     * @throws RuntimeException 임베딩 서버에 연결할 수 없는 경우
     */
    public Optional<List<BookAiRecommendationResponse>> findCachedRecommendations(String keyword) {
        String normalized = SearchResultCache.normalizeKeyword(keyword);
        if (normalized == null) {
            return Optional.empty();
        }
        float[] vector = embeddingService.getEmbedding(normalized);
        return ragSearchStrategy.findCachedRecommendations(
                new BookSearchRequest(normalized, null, SearchType.RAG, vector, false));
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
            long begin = System.nanoTime();
            float[] vector = embeddingService.getEmbedding(request.keyword());
            // 하이브리드 검색의 단계별 소요 시간 로그에 함께 남깁니다 (캐시 적중 시 0에 가까움)
            SearchTimings.recordEmbedding(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - begin));
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
    @Transactional(readOnly = true)
    public BookViewResponse getBook(Long id) {
        log.info("Fetching book details for id: {}", id);
        return bookRepository.findById(id)
                .map(BookViewResponse::from)
                .orElseThrow(() -> new BookNotFoundException(id));
    }

    /**
     * 검색어 없이 정해진 기준으로 도서를 둘러봅니다.
     *
     * <p>검색 전략은 관련도 순위를 매기기 위해 상위 후보만 다루므로, 검색어가 없는 첫 화면에는 맞지 않습니다.
     * 둘러보기는 정렬 기준이 고정되어 있어 새로고침해도 같은 목록이 나오고 전체 페이지를 넘길 수 있습니다.</p>
     *
     * @param pageable 페이지 정보
     * @param sort     정렬 기준
     * @return 도서 목록
     */
    @Transactional(readOnly = true)
    public Page<BookSearchResponse> browseBooks(Pageable pageable, BrowseSort sort) {
        return bookRepository.browse(pageable, sort == null ? BrowseSort.RECENT : sort);
    }

    /**
     * 도서 리뷰 요약을 조회합니다.
     * DB에 저장된 요약을 즉시 반환하며, 요약이 없는 경우에만 대체 텍스트를 반환합니다.
     * (요약 생성은 리뷰 등록 시 이벤트 기반으로 비동기 처리됨)
     *
     * @param bookId 도서 ID
     * @return 요약된 리뷰 텍스트
     */
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
    public Optional<BookReviewSummary> getReviewSummaryEntity(Long bookId) {
        return bookReviewSummaryRepository.findById(bookId);
    }
}
