package net.java21.library.core.book.service.search.strategy;

import net.java21.library.core.book.domain.SearchType;
import net.java21.library.core.book.dto.BookAiRecommendationResponse;
import net.java21.library.core.book.dto.BookSearchRequest;
import net.java21.library.core.book.dto.BookSearchResponse;
import net.java21.library.core.book.dto.BookSearchResult;
import net.java21.library.core.book.event.BookSearchEvent;
import net.java21.library.core.book.service.ai.AiRecommendationService;
import net.java21.library.core.book.service.cache.SearchResultCache;
import net.java21.library.core.book.service.cache.SemanticCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

/**
 * RAG(Retrieval-Augmented Generation) 검색 전략 구현체입니다.
 *
 * <p>화면은 항상 [AI 추천 패널] + [하이브리드 검색 결과 전체(페이징)]로 구성됩니다.</p>
 * <ul>
 *   <li>검색 결과 목록: 매 요청 하이브리드 검색과 같은 결과입니다. 하이브리드 검색과 같은 짧은 TTL 결과 캐시
 *       ({@link SearchResultCache})를 공유합니다.</li>
 *   <li>AI 추천: 시맨틱 캐시({@link SemanticCacheService})에 있으면 함께 돌려주고, 없으면 백그라운드 웜업 이벤트를
 *       발행한 뒤 {@code aiPending=true}로 돌려줍니다. 화면은 {@code GET /api/recommendations}로 준비 여부를 확인합니다.</li>
 *   <li>웜업 모드: 하이브리드 상위 후보 → RRF 임계값 필터 → LLM 추천 생성 → 시맨틱 캐시에 AI 추천만 저장합니다.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RagSearchStrategy implements SearchStrategy {

    private final HybridSearchStrategy hybridSearchStrategy;
    private final SemanticCacheService semanticCacheService;
    private final AiRecommendationService aiRecommendationService;
    private final ApplicationEventPublisher eventPublisher;
    private final SearchResultCache searchResultCache;

    /** 검색 후보군 추출을 위한 기본 배치 크기 */
    private static final int DEFAULT_BATCH_SIZE = 100;

    /** AI 추천 후보로 선정되기 위한 최소 RRF 점수 임계값 */
    private static final double SCORE_THRESHOLD = 0.02;

    /** AI에게 전달할 최대 도서 후보 수 */
    private static final int MAX_AI_CANDIDATES = 5;

    /** 적절한 후보가 없을 경우 대체 사용할 도서 수 */
    private static final int FALLBACK_CANDIDATES = 3;

    /** LLM 검증 최소 relevance 점수 (로그용 승인 건수 집계) */
    private static final int MIN_RELEVANCE_SCORE = 50;

    /**
     * RAG 기반 검색을 수행합니다.
     *
     * @param pageable 페이징 정보 (하이브리드 목록에 적용)
     * @param request  검색 요청 DTO (키워드, 벡터, Warm-up 여부 등 포함)
     * @return 하이브리드 검색 결과 목록과 AI 추천(또는 생성 중 표시)
     */
    @Override
    public BookSearchResult search(Pageable pageable, BookSearchRequest request) {
        if (request.isWarmUp()) {
            return warmUp(pageable, request);
        }

        BookSearchResult hybrid = hybridResult(pageable, request);
        Optional<List<BookAiRecommendationResponse>> cached = semanticCacheService.findRecommendations(request);
        if (cached.isPresent()) {
            log.info("[STRATEGIC_CACHE] Found similar RAG recommendations in cache.");
            return new BookSearchResult(hybrid.getBooks(), cached.get(), 0L, false, false);
        }

        // 캐시 미스: 목록은 바로 보여 주고, AI 추천 생성은 백그라운드(Warm-up)로 위임합니다.
        boolean canWarmUp = StringUtils.hasText(request.keyword());
        if (canWarmUp) {
            log.info("[STRATEGIC_CACHE] No RAG cache found. Returning hybrid results and publishing warm-up event.");
            eventPublisher.publishEvent(new BookSearchEvent(this, request.keyword()));
        }
        return new BookSearchResult(hybrid.getBooks(), List.of(), 0L, false, canWarmUp);
    }

    /**
     * 시맨틱 캐시에서 AI 추천만 조회합니다. LLM을 호출하지 않습니다.
     *
     * @param request 검색어와 벡터가 있는 RAG 요청
     * @return 캐시된 AI 추천, 아직 없으면 empty
     */
    public Optional<List<BookAiRecommendationResponse>> findCachedRecommendations(BookSearchRequest request) {
        return semanticCacheService.findRecommendations(request);
    }

    /**
     * 하이브리드 검색 결과를 하이브리드 검색과 같은 결과 캐시 키로 조회·저장합니다.
     */
    private BookSearchResult hybridResult(Pageable pageable, BookSearchRequest request) {
        BookSearchRequest hybridKey = new BookSearchRequest(
                SearchResultCache.normalizeKeyword(request.keyword()), request.isbn(), SearchType.HYBRID, null, false);
        if (!searchResultCache.isCacheable(pageable, hybridKey)) {
            return hybridSearchStrategy.search(pageable, request);
        }
        return searchResultCache.get(pageable, hybridKey).orElseGet(() -> {
            BookSearchResult result = hybridSearchStrategy.search(pageable, request);
            searchResultCache.put(pageable, hybridKey, result);
            return result;
        });
    }

    /**
     * 웜업 모드: 하이브리드 상위 후보로 LLM 추천을 만들어 시맨틱 캐시에 저장합니다.
     *
     * <p>LLM 호출이 실패하면 예외가 그대로 전파되어 캐시에 저장되지 않으므로, 다음 검색 요청이 다시 웜업합니다.</p>
     */
    private BookSearchResult warmUp(Pageable pageable, BookSearchRequest request) {
        log.info("[STRATEGIC_CACHE] Performing AI recommendation for warm-up.");

        // Retrieval K(100) 기반 후보군 추출: RRF 점수가 높은 도서 위주로 필터링
        BookSearchResult retrievalResult = hybridSearchStrategy.search(PageRequest.of(0, DEFAULT_BATCH_SIZE), request);
        List<BookSearchResponse> retrieved = retrievalResult.getBooks().getContent();
        List<BookSearchResponse> topKBooks = retrieved.stream()
                .filter(b -> b.getRrfScore() != null && b.getRrfScore() >= SCORE_THRESHOLD)
                .limit(MAX_AI_CANDIDATES)
                .toList();

        // 임계값을 통과한 후보가 없으면 검색 상위 결과로 대체하여 AI 추론을 생성합니다.
        if (topKBooks.isEmpty()) {
            log.info("No books passed the threshold (>= {}). Using top results as fallback for warm-up.", SCORE_THRESHOLD);
            topKBooks = retrieved.stream().limit(FALLBACK_CANDIDATES).toList();
        }

        List<BookAiRecommendationResponse> aiResponse;
        if (topKBooks.isEmpty()) {
            log.info("No candidates available even after fallback. Skipping AI response generation.");
            aiResponse = List.of();
        } else {
            // LLM 서비스를 호출하여 추천 사유 생성 및 relevance 점수 부여 (도서 정보는 서비스가 후보에서 채움)
            aiResponse = aiRecommendationService.recommend(request.keyword(), topKBooks);
        }

        long approved = aiResponse.stream()
                .filter(r -> r.getRelevance() != null && r.getRelevance() >= MIN_RELEVANCE_SCORE)
                .count();
        log.info("[LLM_VALIDATION] Approved {}/{} books with relevance >= {}", approved, aiResponse.size(), MIN_RELEVANCE_SCORE);

        semanticCacheService.save(request, aiResponse);

        // 호출한 쪽(웜업)은 결과를 쓰지 않지만, 목록 형태는 일반 요청과 같게 맞춥니다.
        int from = (int) Math.min(pageable.getOffset(), retrieved.size());
        int to = Math.min(from + pageable.getPageSize(), retrieved.size());
        return new BookSearchResult(new PageImpl<>(retrieved.subList(from, to), pageable, retrieved.size()),
                aiResponse, 0L, false, false);
    }
}
