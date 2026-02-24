package com.nhnacademy.library.external.opennaru.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.library.core.config.CacheConfig;
import com.nhnacademy.library.external.opennaru.properties.LibraryApiProperties;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestClient;

/**
 * API Client 테스트를 위한 최소한의 설정
 */
@Configuration
@Import({
    CacheConfig.class,
    JacksonAutoConfiguration.class,
    HttpMessageConvertersAutoConfiguration.class,
    RestClientAutoConfiguration.class
})
class ApiClientTestConfig {

    @Bean
    public LibraryApiProperties libraryApiProperties() {
        LibraryApiProperties properties = new LibraryApiProperties();
        properties.setKey("356634a513872ef340a52007c03c19603a7a3908165d1a9ab46b79a5afd6b83d");
        return properties;
    }

    @Bean
    public LibraryInfoNaruApiClient libraryInfoNaruApiClient(
            LibraryApiProperties properties,
            ObjectMapper objectMapper,
            RestClient.Builder restClientBuilder) {
        return new LibraryInfoNaruApiClient(properties, objectMapper, restClientBuilder);
    }
}
