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
        
        // 1. RAG 검색용 요청 생성
        BookSearchRequest ragRequest = new BookSearchRequest(keyword, null, "rag", null);
        Pageable pageable = PageRequest.of(0, 24); // 기본 페이지 사이즈

        // 2. 이미 캐시되어 있는지 확인
        Cache cache = cacheManager.getCache("bookSearchCache");
        if (cache != null && cache.get(ragRequest) != null) {
            log.info("[STRATEGIC_CACHE] Cache already exists for keyword: {}", keyword);
            return;
        }

        try {
            // BookSearchService를 ApplicationContext에서 지연 조회하여 순환 참조 해결
            BookSearchService bookSearchService = applicationContext.getBean(BookSearchService.class);
            
            // 3. RAG 검색 수행 (이 과정에서 LLM 호출 등이 발생함)
            bookSearchService.searchBooks(pageable, ragRequest);
            
            log.info("[STRATEGIC_CACHE] Successfully warmed up RAG cache for keyword: {}", keyword);
        } catch (Exception e) {
            log.error("[STRATEGIC_CACHE] Failed to warm up RAG cache for keyword: {}", keyword, e);
        }
    }
}
