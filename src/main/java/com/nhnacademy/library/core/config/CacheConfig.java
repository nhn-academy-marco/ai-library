package com.nhnacademy.library.core.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableCaching
@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        // 단순한 인메모리 CacheManager (테스트/개발 환경에서 사용)
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
}
