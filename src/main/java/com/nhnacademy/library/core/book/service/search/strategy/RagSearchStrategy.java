package com.nhnacademy.library.core.book.service.search.strategy;

import com.nhnacademy.library.core.book.dto.BookAiRecommendationResponse;
import com.nhnacademy.library.core.book.dto.BookSearchRequest;
import com.nhnacademy.library.core.book.dto.BookSearchResponse;
import com.nhnacademy.library.core.book.dto.BookSearchResult;
import com.nhnacademy.library.core.book.event.BookSearchEvent;
import com.nhnacademy.library.core.book.service.ai.AiRecommendationService;
import com.nhnacademy.library.core.book.service.cache.SemanticCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * RAG(Retrieval-Augmented Generation) 검색 전략 구현체입니다.
 * 하이브리드 검색을 통해 후보군을 추출하고, LLM을 사용하여 사용자에게 맞춤형 도서 추천 사유를 제공합니다.
 * 시맨틱 캐싱(Semantic Caching)을 적용하여 동일하거나 유사한 질의에 대해 빠른 응답을 보장합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RagSearchStrategy implements SearchStrategy {
    
    private final HybridSearchStrategy hybridSearchStrategy;
    private final SemanticCacheService semanticCacheService;
    private final AiRecommendationService aiRecommendationService;
    private final ApplicationEventPublisher eventPublisher;
    
    /** 검색 후보군 추출을 위한 기본 배치 크기 */
    private static final int DEFAULT_BATCH_SIZE = 100;
    
    /** AI 추천 후보로 선정되기 위한 최소 RRF 점수 임계값 */
    private static final double SCORE_THRESHOLD = 0.02;
    
    /** AI에게 전달할 최대 도서 후보 수 */
    private static final int MAX_AI_CANDIDATES = 5;
    
    /** 적절한 후보가 없을 경우 대체 사용할 도서 수 */
    private static final int FALLBACK_CANDIDATES = 3;

    /**
     * RAG 기반 검색을 수행합니다.
     * 1. 캐시 확인: Warm-up 모드가 아닐 경우 시맨틱 캐시에서 유사 결과를 조회합니다.
     * 2. 캐시 미스 시: 하이브리드 검색 결과를 먼저 반환하고, 백그라운드에서 AI 추천을 생성하기 위한 이벤트를 발행합니다.
     * 3. Warm-up 모드: 실제로 하이브리드 검색 -> 후보 필터링 -> AI 추천 생성 -> 캐시 저장 과정을 수행합니다.
     *
     * @param pageable 페이징 정보
     * @param request  검색 요청 DTO (키워드, Warm-up 여부 등 포함)
     * @return AI 추천 응답과 도서 검색 결과가 포함된 BookSearchResult
     */
    @Override
    public BookSearchResult search(Pageable pageable, BookSearchRequest request) {
        // 1. Warm-up 모드가 아닐 때만 캐시 조회 및 이벤트 발행
        if (!request.isWarmUp()) {
            Optional<BookSearchResult> cachedResult = semanticCacheService.findSimilarResult(request);
            if (cachedResult.isPresent()) {
                log.info("[STRATEGIC_CACHE] Found similar RAG result in cache.");
                return cachedResult.get();
            }

            // 사용자 실시간 요청인데 캐시 미스가 발생한 경우
            // 빠른 응답을 위해 하이브리드 검색 결과를 먼저 반환하고, AI 추천 생성은 백그라운드(Warm-up)로 위임합니다.
            log.info("[STRATEGIC_CACHE] No RAG cache found. Falling back to hybrid results and publishing event.");
            eventPublisher.publishEvent(new BookSearchEvent(this, request.keyword()));
            return hybridSearchStrategy.search(pageable, request);
        }

        // 2. Warm-up 모드: 실제 AI 추천 생성 및 캐싱 수행
        log.info("[STRATEGIC_CACHE] Performing AI recommendation for warm-up.");
        
        // Retrieval K(100) 기반 후보군 추출: RRF 점수가 높은 도서 위주로 필터링
        BookSearchResult retrievalResult = hybridSearchStrategy.search(PageRequest.of(0, DEFAULT_BATCH_SIZE), request);
        List<BookSearchResponse> topKBooks = retrievalResult.getBooks().getContent().stream()
                .filter(b -> b.getRrfScore() != null && b.getRrfScore() >= SCORE_THRESHOLD)
                .limit(MAX_AI_CANDIDATES)
                .toList();

        // Warm-up 모드에서 임계값을 통과한 후보가 없으면 검색 상위 결과로 대체하여 AI 추천을 생성합니다.
        if (request.isWarmUp() && topKBooks.isEmpty()) {
            log.info("No books passed the threshold (>= {}). Using top keyword results as fallback for warm-up.", SCORE_THRESHOLD);
            topKBooks = retrievalResult.getBooks().getContent().stream().limit(FALLBACK_CANDIDATES).toList();
        }

        List<BookAiRecommendationResponse> aiResponse;
        if (topKBooks.isEmpty()) {
            log.info("No candidates available even after fallback. Skipping AI response generation.");
            aiResponse = List.of();
        } else {
            // LLM 서비스를 호출하여 추천 사유 생성
            aiResponse = aiRecommendationService.recommend(request.keyword(), topKBooks);
        }

        // 최종 응답을 위해 사용자 요청 페이징 크기에 맞춘 도서 검색 결과 재추출
        BookSearchResult finalBooks = hybridSearchStrategy.search(pageable, request);
        
        BookSearchResult finalResult = BookSearchResult.builder()
                .books(finalBooks.getBooks())
                .aiResponse(aiResponse)
                .createdAt(System.currentTimeMillis())
                .build();

        // 3. 생성된 결과를 시맨틱 캐시에 저장
        semanticCacheService.save(request, finalResult);

        return finalResult;
    }
}
