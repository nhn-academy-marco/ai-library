package com.nhnacademy.library.core.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestCacheConfig {

    @Bean
    public CacheManager cacheManager() {
        // 테스트용 간단한 인메모리 캐시 매니저
        return new ConcurrentMapCacheManager("bookSearchCache");
    }
}
