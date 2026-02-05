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
 * RAG(Retrieval-Augmented Generation) 검색 전략
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RagSearchStrategy implements SearchStrategy {

    private final HybridSearchStrategy hybridSearchStrategy;
    private final SemanticCacheService semanticCacheService;
    private final AiRecommendationService aiRecommendationService;
    private final ApplicationEventPublisher eventPublisher;

    private static final int DEFAULT_BATCH_SIZE = 100;
    private static final double SCORE_THRESHOLD = 0.02;

    @Override
    public BookSearchResult search(Pageable pageable, BookSearchRequest request) {
        // 1. Warm-up 모드가 아닐 때만 캐시 조회 및 이벤트 발행
        if (!request.isWarmUp()) {
            Optional<BookSearchResult> cachedResult = semanticCacheService.findSimilarResult(request);
            if (cachedResult.isPresent()) {
                log.info("[STRATEGIC_CACHE] Found similar RAG result in cache.");
                return cachedResult.get();
            }

            // 사용자 요청인데 캐시 미스 -> 하이브리드 결과 반환 및 백그라운드 워밍업 이벤트 발행
            log.info("[STRATEGIC_CACHE] No RAG cache found. Falling back to hybrid results and publishing event.");
            eventPublisher.publishEvent(new BookSearchEvent(this, request.keyword()));
            return hybridSearchStrategy.search(pageable, request);
        }

        // 2. Warm-up 모드: 실제 AI 추천 생성
        log.info("[STRATEGIC_CACHE] Performing AI recommendation for warm-up.");
        
        // Retrieval K(100) 기반 후보군 추출
        BookSearchResult retrievalResult = hybridSearchStrategy.search(PageRequest.of(0, DEFAULT_BATCH_SIZE), request);
        List<BookSearchResponse> topKBooks = retrievalResult.getBooks().getContent().stream()
                .filter(b -> b.getRrfScore() != null && b.getRrfScore() >= SCORE_THRESHOLD)
                .limit(5)
                .toList();

        // Warm-up 모드에서 임계값을 통과한 후보가 없으면 키워드 상위 결과로 대체하여 AI를 한 번 호출합니다.
        if (request.isWarmUp() && topKBooks.isEmpty()) {
            log.info("No books passed the threshold (>= {}). Using top keyword results as fallback for warm-up.", SCORE_THRESHOLD);
            topKBooks = retrievalResult.getBooks().getContent().stream().limit(3).toList();
        }

        List<BookAiRecommendationResponse> aiResponse;
        if (topKBooks.isEmpty()) {
            log.info("No candidates available even after fallback. Skipping AI response generation.");
            aiResponse = List.of();
        } else {
            aiResponse = aiRecommendationService.recommend(request.keyword(), topKBooks);
        }

        // 사용자 페이징 크기에 맞춘 도서 결과
        BookSearchResult finalBooks = hybridSearchStrategy.search(pageable, request);
        
        BookSearchResult finalResult = BookSearchResult.builder()
                .books(finalBooks.getBooks())
                .aiResponse(aiResponse)
                .createdAt(System.currentTimeMillis())
                .build();

        // 3. 캐시 저장
        semanticCacheService.save(request, finalResult);

        return finalResult;
    }
}
