package com.nhnacademy.library.core.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 캐시 설정 클래스
 *
 * <p>Caffeine을 사용하여 TTL과 사이즈 제한이 있는 인메모리 캐시를 제공합니다.</p>
 *
 * <p>캐시 전략:</p>
 * <ul>
 *   <li>개인화 캐시: 24시간 TTL, 최대 1000개</li>
 *   <li>도서 검색 캐시: 30분 TTL, 최대 500개</li>
 *   <li>도서관 API 캐시: 60분 TTL, 최대 1000개</li>
 *   <li>리뷰 캐시: 30분 TTL, 최대 500개</li>
 * </ul>
 */
@EnableCaching
@Configuration
public class CacheConfig {

    @Value("${cache.personalization.ttl.hours:24}")
    private int personalizationTtlHours;

    @Value("${cache.personalization.max-size:1000}")
    private int personalizationMaxSize;

    @Value("${cache.book-search.ttl.minutes:30}")
    private int bookSearchTtlMinutes;

    @Value("${cache.book-search.max-size:500}")
    private int bookSearchMaxSize;

    @Value("${cache.library-api.ttl.minutes:60}")
    private int libraryApiTtlMinutes;

    @Value("${cache.library-api.max-size:1000}")
    private int libraryApiMaxSize;

    @Value("${cache.review.ttl.minutes:30}")
    private int reviewTtlMinutes;

    @Value("${cache.review.max-size:500}")
    private int reviewMaxSize;

    /**
     * 메인 CacheManager
     *
     * <p>CaffeineCacheManager를 사용하여 모든 캐시에 TTL과 사이즈 제한을 적용합니다.
     * 캐시 통계를 활성화하여 모니터링을 지원합니다.</p>
     */
    @Bean
    @Primary
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        // 캐시 이름 등록 (명시적 등록으로 자동 생성 방지)
        cacheManager.setCacheNames(List.of(
            // 도서 검색 캐시
            "bookSearchCache",
            // 리뷰 캐시
            "reviewSummaries",
            // 도서관정보나루 API 캐시
            "librarySearch",
            "librarySearchByIsbn",
            "librarySearchByIsbn10",
            "librarySearchByAuthor",
            "librarySearchByPublisher",
            "libraryLoanItems",
            // 추가 API 캐시
            "librarySearchAll",
            "librarySearchByRegion",
            "librarySearchByName",
            "bookDetail",
            "recommendedBooks",
            "recommendedBooksForMania",
            "recommendedBooksForReader",
            // 나머지 API 캐시
            "popularBooks",
            "bookKeywords",
            "usageAnalysis",
            "popularBooksByLib",
            "usageTrend",
            "bookExist",
            "hotTrendBooks",
            "monthlyKeywords",
            "readingQuantity",
            // extends API 캐시
            "libraryExtendedInfo",
            "extendedPopularBooks"
        ));

        // 기본 Caffeine 설정 (도서 검색 및 리뷰 캐시용)
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .expireAfterWrite(bookSearchTtlMinutes, TimeUnit.MINUTES)
            .maximumSize(bookSearchMaxSize)
            .recordStats()); // 통계 활성화

        return cacheManager;
    }

    /**
     * 개인화 캐시 전용 CacheManager
     *
     * <p>사용자 선호도 벡터 캐시는 별도의 TTL과 사이즈 제한을 적용합니다.
     * 기본값: 24시간 TTL, 최대 1000개</p>
     */
    @Bean("personalizationCacheManager")
    public CacheManager personalizationCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("userPreferenceVectors");
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .expireAfterWrite(personalizationTtlHours, TimeUnit.HOURS)
            .maximumSize(personalizationMaxSize)
            .recordStats()); // 통계 활성화
        return cacheManager;
    }
}
