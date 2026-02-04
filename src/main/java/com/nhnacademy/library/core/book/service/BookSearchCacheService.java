package com.nhnacademy.library.core.book.service;

import com.nhnacademy.library.core.book.dto.BookAiRecommendationResponse;
import com.nhnacademy.library.core.book.dto.BookSearchRequest;
import com.nhnacademy.library.core.book.dto.BookSearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookSearchCacheService {

    private final ApplicationContext applicationContext;
    private final CacheManager cacheManager;

    @Async
    public void warmUpRagCache(String keyword) {
        log.info("[STRATEGIC_CACHE] Warming up RAG cache for keyword: {}", keyword);
        
        try {
            // BookSearchService를 ApplicationContext에서 지연 조회하여 순환 참조 해결
            BookSearchService bookSearchService = applicationContext.getBean(BookSearchService.class);
            EmbeddingService embeddingService = applicationContext.getBean(EmbeddingService.class);
            
            // 1. 임베딩 생성 (유사도 비교를 위해)
            float[] vector = embeddingService.getEmbedding(keyword);
            BookSearchRequest ragRequest = new BookSearchRequest(keyword, null, "rag", vector);
            Pageable pageable = PageRequest.of(0, 24);

            // 2. 이미 캐시되어 있는지 확인 (의미적 캐싱 적용)
            if (bookSearchService.findSimilarResultInCache(ragRequest) != null) {
                log.info("[STRATEGIC_CACHE] Similar cache already exists for keyword: {}", keyword);
                return;
            }
            
            // 3. RAG 검색 수행 (이 과정에서 LLM 호출 등이 발생함)
            bookSearchService.searchBooksWithVector(pageable, ragRequest);
            
            log.info("[STRATEGIC_CACHE] Successfully warmed up RAG cache for keyword: {}", keyword);
        } catch (Exception e) {
            log.error("[STRATEGIC_CACHE] Failed to warm up RAG cache for keyword: {}", keyword, e);
        }
    }
}
