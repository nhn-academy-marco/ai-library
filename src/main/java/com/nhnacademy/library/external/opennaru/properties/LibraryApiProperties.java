package com.nhnacademy.library.external.opennaru.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 도서관정보나루 API 설정 프로퍼티
 */
@Data
@Component
@ConfigurationProperties(prefix = "library.api")
public class LibraryApiProperties {

    /**
     * 도서관정보나루 API 기본 URL
     */
    private String url = "http://data4library.kr/api";

    /**
     * 도서관정보나루 API 인증 키
     */
    private String key;

    /**
     * API 호출 타임아웃 (밀리초)
     */
    private int timeout = 5000;

    /**
     * 캐시 사용 여부
     */
    private boolean cacheEnabled = true;
}
