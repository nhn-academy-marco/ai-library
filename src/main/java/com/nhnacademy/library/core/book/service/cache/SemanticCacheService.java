package com.nhnacademy.library.core.book.service.cache;

import com.nhnacademy.library.core.book.domain.BookSearchCache;
import com.nhnacademy.library.core.book.domain.SearchType;
import com.nhnacademy.library.core.book.dto.BookSearchRequest;
import com.nhnacademy.library.core.book.dto.BookSearchResult;
import com.nhnacademy.library.core.book.repository.BookSearchCacheRepository;
import com.nhnacademy.library.core.book.util.VectorUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 의미적 캐싱 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SemanticCacheService {

    private final BookSearchCacheRepository cacheRepository;
    private final CacheManager cacheManager;

    @Value("${cache.ttl.minutes:30}")
    private int cacheTtlMinutes;

    private static final double SIMILARITY_THRESHOLD = 0.98;

    /**
     * 벡터 유사도 기반 캐시 조회
     */
    public Optional<BookSearchResult> findSimilarResult(BookSearchRequest request) {
        if (request.vector() == null) return Optional.empty();

        long ttlMillis = (long) cacheTtlMinutes * 60 * 1000;
        long now = System.currentTimeMillis();

        Iterable<BookSearchCache> allCached = cacheRepository.findAll();

        for (BookSearchCache cached : allCached) {
            double similarity = VectorUtils.calculateCosineSimilarity(request.vector(), cached.getVector());
            if (similarity < SIMILARITY_THRESHOLD) continue;

            long age = now - cached.getCreatedAt();
            if (age > ttlMillis || age < 0) {
                log.info("[SEMANTIC_CACHE] Cache expired for keyword: '{}' (Age: {}ms, TTL: {}ms)",
                        cached.getKeyword(), age, ttlMillis);
                evictCache(cached);
                continue;
            }

            log.info("[SEMANTIC_CACHE] Found similar request in cache: '{}' (Similarity: {})",
                    cached.getKeyword(), similarity);
            
            return Optional.of(BookSearchResult.builder()
                    .books(new PageImpl<>(cached.getBooks()))
                    .aiResponse(cached.getAiResponse())
                    .createdAt(cached.getCreatedAt())
                    .build());
        }
        return Optional.empty();
    }

    /**
     * 캐시 저장
     */
    public void save(BookSearchRequest request, BookSearchResult result) {
        if (request.searchType() != SearchType.RAG) return;

        // Redis 캐시 저장
        BookSearchCache cacheEntry = BookSearchCache.builder()
                .keyword(request.keyword())
                .vector(request.vector())
                .books(result.getBooks().getContent())
                .aiResponse(result.getAiResponse())
                .createdAt(result.getCreatedAt())
                .build();
        
        log.info("[SEMANTIC_CACHE] Putting result into Redis for keyword: {}", request.keyword());
        cacheRepository.save(cacheEntry);

        // Spring Cache(Local) 저장
        Cache cache = cacheManager.getCache("bookSearchCache");
        if (cache != null) {
            cache.put(request, result);
        }
    }

    private void evictCache(BookSearchCache cached) {
        cacheRepository.delete(cached);
        Cache cache = cacheManager.getCache("bookSearchCache");
        if (cache != null) {
            // Warm-up 및 일반 유저 키 모두 만료 처리
            BookSearchRequest warmUpKey = new BookSearchRequest(cached.getKeyword(), null, SearchType.RAG, cached.getVector(), true);
            cache.evict(warmUpKey);
            BookSearchRequest userKey = new BookSearchRequest(cached.getKeyword(), null, SearchType.RAG, cached.getVector(), false);
            cache.evict(userKey);
        }
    }
}
