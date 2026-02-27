package com.nhnacademy.library.core.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 캐시 설정 클래스
 *
 * <p>ConcurrentMapCacheManager를 사용하여 간단한 인메모리 캐시를 제공합니다.
 * userPreferenceVectors는 Caffeine을 사용하여 TTL을 적용합니다.</p>
 */
@EnableCaching
@Configuration
public class CacheConfig {

    /**
     * 메인 CacheManager
     *
     * <p>대부분의 캐시는 ConcurrentMapCacheManager를 사용합니다.</p>
     */
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
            "bookSearchCache",
            "reviewSummaries",
            // 도서관정보나루 API 캐시
            "librarySearch",
            "librarySearchByIsbn",
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
            // 나머지 12개 API 캐시
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
        );
    }

    /**
     * 개인화 캐시 전용 CacheManager
     *
     * <p>사용자 선호도 벡터 캐시는 24시간 TTL을 적용합니다.
     * 최대 1000개의 캐시를 저장합니다.</p>
     */
    @Bean("personalizationCacheManager")
    public CacheManager personalizationCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("userPreferenceVectors");
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .expireAfterWrite(24, TimeUnit.HOURS)
            .maximumSize(1000));
        return cacheManager;
    }
}
