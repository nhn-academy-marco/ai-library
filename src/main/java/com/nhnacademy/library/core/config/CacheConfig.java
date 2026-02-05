package com.nhnacademy.library.core.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        // 단순한 인메모리 CacheManager (테스트/개발 환경에서 사용)
        return new ConcurrentMapCacheManager("bookSearchCache");
    }
}
