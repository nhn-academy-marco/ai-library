package net.java21.library.core.book.service.cache;

import net.java21.library.core.book.domain.BookSearchCache;
import net.java21.library.core.book.domain.SearchType;
import net.java21.library.core.book.dto.BookSearchRequest;
import net.java21.library.core.book.dto.BookSearchResult;
import net.java21.library.core.book.repository.BookSearchCacheRepository;
import net.java21.library.core.book.util.VectorUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 의미적 캐싱 서비스
 *
 * <p>검색어 임베딩의 코사인 유사도로 이전 RAG 결과를 찾습니다. 조회·저장·만료는 모두
 * {@link BookSearchCacheRepository}(인메모리, 최대 건수 제한) 하나로 처리합니다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SemanticCacheService {

    private final BookSearchCacheRepository cacheRepository;

    @Value("${cache.ttl.minutes:30}")
    private int cacheTtlMinutes;

    private static final double SIMILARITY_THRESHOLD = 0.98;
    private static final long MILLIS_PER_MINUTE = 60 * 1000L;

    /**
     * 벡터 유사도 기반 캐시 조회
     */
    public Optional<BookSearchResult> findSimilarResult(BookSearchRequest request) {
        if (request.vector() == null) return Optional.empty();

        long ttlMillis = (long) cacheTtlMinutes * MILLIS_PER_MINUTE;
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

        BookSearchCache cacheEntry = BookSearchCache.builder()
                .keyword(request.keyword())
                .vector(request.vector())
                .books(result.getBooks().getContent())
                .aiResponse(result.getAiResponse())
                .createdAt(result.getCreatedAt())
                .build();
        
        log.info("[SEMANTIC_CACHE] Putting result into semantic cache for keyword: {}", request.keyword());
        cacheRepository.save(cacheEntry);
    }

    private void evictCache(BookSearchCache cached) {
        cacheRepository.delete(cached);
    }
}
