package net.java21.library.core.book.service.cache;

import net.java21.library.core.book.domain.BookSearchCache;
import net.java21.library.core.book.domain.SearchType;
import net.java21.library.core.book.dto.BookAiRecommendationResponse;
import net.java21.library.core.book.dto.BookSearchRequest;
import net.java21.library.core.book.dto.BookSearchResponse;
import net.java21.library.core.book.repository.BookSearchCacheRepository;
import net.java21.library.core.book.util.VectorUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 의미적 캐싱 서비스
 *
 * <p>검색어 임베딩의 코사인 유사도로 이전 RAG의 AI 추천을 찾습니다. 조회·저장·만료는 모두
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
     * 검색어 벡터와 코사인 유사도가 {@value #SIMILARITY_THRESHOLD} 이상인 캐시 항목의 AI 추천을 찾습니다.
     *
     * <p>캐시에는 AI 추천만 의미가 있습니다. 화면의 검색 결과 목록은 호출하는 쪽이 매 요청 하이브리드 검색으로 채웁니다.
     * 이전 버전 항목처럼 추천에 도서 정보가 없고 {@code books}가 남아 있으면 그 목록으로 채워서 돌려줍니다.</p>
     *
     * @param request 검색 요청 (벡터 필수)
     * @return 캐시된 AI 추천 (추천할 도서가 없었다면 빈 목록), 캐시가 없거나 만료됐으면 empty
     */
    public Optional<List<BookAiRecommendationResponse>> findRecommendations(BookSearchRequest request) {
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
            return Optional.of(withBookInfo(cached));
        }
        return Optional.empty();
    }

    /**
     * AI 추천을 캐시에 저장합니다. (RAG 요청만)
     *
     * @param request    검색 요청 (검색어와 벡터)
     * @param aiResponse AI 추천 결과 (빈 목록이면 "추천할 도서 없음"으로 저장)
     */
    public void save(BookSearchRequest request, List<BookAiRecommendationResponse> aiResponse) {
        if (request.searchType() != SearchType.RAG) return;

        BookSearchCache cacheEntry = BookSearchCache.builder()
                .keyword(request.keyword())
                .vector(request.vector())
                .aiResponse(aiResponse == null ? List.of() : List.copyOf(aiResponse))
                .createdAt(System.currentTimeMillis())
                .build();

        log.info("[SEMANTIC_CACHE] Putting AI recommendations into semantic cache for keyword: {}", request.keyword());
        cacheRepository.save(cacheEntry);
    }

    /**
     * 이전 버전 항목(도서 정보 없이 id·why만 있는 추천 + books 목록)을 현재 형태로 맞춥니다.
     */
    private static List<BookAiRecommendationResponse> withBookInfo(BookSearchCache cached) {
        List<BookAiRecommendationResponse> recommendations = cached.getAiResponse() == null ? List.of() : cached.getAiResponse();
        List<BookSearchResponse> legacyBooks = cached.getBooks();
        if (legacyBooks == null || legacyBooks.isEmpty()) {
            return recommendations;
        }
        Map<Long, BookSearchResponse> booksById = legacyBooks.stream()
                .filter(book -> book.getId() != null)
                .collect(Collectors.toMap(BookSearchResponse::getId, Function.identity(), (first, second) -> first));
        for (BookAiRecommendationResponse rec : recommendations) {
            BookSearchResponse book = booksById.get(rec.getId());
            if (rec.getTitle() == null && book != null) {
                rec.applyBook(book);
            }
        }
        return recommendations;
    }

    private void evictCache(BookSearchCache cached) {
        cacheRepository.delete(cached);
    }
}
