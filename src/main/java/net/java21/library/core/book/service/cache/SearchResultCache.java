package net.java21.library.core.book.service.cache;

import net.java21.library.core.book.domain.SearchType;
import net.java21.library.core.book.dto.BookSearchRequest;
import net.java21.library.core.book.dto.BookSearchResult;
import net.java21.library.core.config.CacheConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

/**
 * 키워드·벡터·하이브리드 검색 결과의 짧은 TTL 캐시입니다.
 *
 * <p>같은 검색을 짧은 간격으로 반복할 때 임베딩 생성과 DB 조회(특히 15만 건 벡터 정렬)를 건너뛰기 위한
 * 성능용 보조 수단입니다. 결과의 결정성은 저장소 정렬(키워드·벡터)과 RRF 동점 처리로 보장하며,
 * 이 캐시에 의존하지 않습니다.</p>
 *
 * <p>캐시하지 않는 경우:</p>
 * <ul>
 *   <li>RAG 검색: {@link SemanticCacheService}가 따로 담당</li>
 *   <li>호출자가 벡터를 직접 넘긴 요청, 캐시 워밍업 요청</li>
 *   <li>임베딩 장애로 키워드 검색으로 대체된 결과(장애가 풀리면 바로 원래 검색을 해야 하므로)</li>
 * </ul>
 *
 * <p>{@code @Cacheable} 대신 {@link CacheManager}를 직접 사용하므로 같은 클래스 내부 호출(self-invocation)에도 동작합니다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SearchResultCache {

    private static final Set<SearchType> CACHEABLE_TYPES = Set.of(SearchType.KEYWORD, SearchType.VECTOR, SearchType.HYBRID);

    private final CacheManager cacheManager;

    /**
     * 캐시 키. 검색어는 {@link #normalizeKeyword(String)}로 정규화된 값을 씁니다.
     */
    record Key(String keyword, SearchType searchType, String isbn, int page, int size, String sort) {
    }

    /**
     * 이 요청의 결과를 캐시할 수 있는지 판단합니다.
     */
    public boolean isCacheable(Pageable pageable, BookSearchRequest request) {
        return pageable.isPaged()
                && CACHEABLE_TYPES.contains(request.searchType())
                && request.vector() == null
                && !Boolean.TRUE.equals(request.isWarmUp());
    }

    public Optional<BookSearchResult> get(Pageable pageable, BookSearchRequest request) {
        Cache cache = cache();
        if (cache == null) {
            return Optional.empty();
        }
        BookSearchResult cached = cache.get(keyOf(pageable, request), BookSearchResult.class);
        if (cached != null) {
            log.debug("[SEARCH_RESULT_CACHE] Hit: keyword={}, type={}", request.keyword(), request.searchType());
        }
        return Optional.ofNullable(cached);
    }

    public void put(Pageable pageable, BookSearchRequest request, BookSearchResult result) {
        Cache cache = cache();
        if (cache == null || result == null || result.isKeywordFallback()) {
            return;
        }
        cache.put(keyOf(pageable, request), result);
    }

    /**
     * 검색어 앞뒤 공백을 제거하고 연속 공백을 하나로 줄입니다. 빈 값은 null로 바꿉니다.
     *
     * <p>캐시 키와 실제 검색에 같은 값을 써야 하므로 검색 서비스도 이 메서드로 정규화한 검색어로 검색합니다.</p>
     */
    public static String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String normalized = keyword.strip().replaceAll("\\s+", " ");
        return normalized.isEmpty() ? null : normalized;
    }

    private Key keyOf(Pageable pageable, BookSearchRequest request) {
        return new Key(
                normalizeKeyword(request.keyword()),
                request.searchType(),
                request.isbn(),
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSort().toString());
    }

    private Cache cache() {
        return cacheManager.getCache(CacheConfig.BOOK_SEARCH_RESULT_CACHE);
    }
}
