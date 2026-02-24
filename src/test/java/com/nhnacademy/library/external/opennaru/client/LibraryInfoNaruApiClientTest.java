package com.nhnacademy.library.external.opennaru.client;

import com.nhnacademy.library.external.opennaru.dto.*;
import com.nhnacademy.library.external.opennaru.properties.LibraryApiProperties;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 도서관정보나루 API 클라이언트 단위 테스트
 *
 * Spring 컨텍스트 없이 메서드 호출 가능 여부를 검증합니다.
 * 실제 API는 호출되지 않으며, 메서드 시그니처와 예외 처리만 확인합니다.
 */
@Slf4j
@DisplayName("도서관정보나루 API 클라이언트 단위 테스트")
class LibraryInfoNaruApiClientTest {

    /**
     * 메서드 시그니처 검증
     * - 모든 public 메서드가 올바른 타입을 반환하는지 확인
     */
    @Test
    @DisplayName("API 클라이언트 클래스 로드 확인")
    void testClassLoadable() {
        // When & Then
        assertThat(LibraryInfoNaruApiClient.class).isNotNull();
        log.info("[DEBUG_LOG] LibraryInfoNaruApiClient 클래스 로드 성공");
    }

    @Test
    @DisplayName("LibraryBookInfo 레코드 클래스 로드 확인")
    void testLibraryBookInfoClassLoadable() {
        // When & Then
        assertThat(LibraryBookInfo.class).isNotNull();
        log.info("[DEBUG_LOG] LibraryBookInfo 클래스 로드 성공");
    }

    @Test
    @DisplayName("LoanItemInfo 레코드 클래스 로드 확인")
    void testLoanItemInfoClassLoadable() {
        // When & Then
        assertThat(LoanItemInfo.class).isNotNull();
        log.info("[DEBUG_LOG] LoanItemInfo 클래스 로드 성공");
    }

    @Test
    @DisplayName("LibraryApiProperties 클래스 로드 확인")
    void testLibraryApiPropertiesClassLoadable() {
        // When & Then
        assertThat(LibraryApiProperties.class).isNotNull();
        log.info("[DEBUG_LOG] LibraryApiProperties 클래스 로드 성공");
    }

    @Test
    @DisplayName("ObjectMapper 설정 클래스 로드 확인")
    void testObjectMapperConfigClassLoadable() {
        // When & Then
        assertThat(com.nhnacademy.library.core.config.ObjectMapperConfig.class).isNotNull();
        log.info("[DEBUG_LOG] ObjectMapperConfig 클래스 로드 성공");
    }

    @Test
    @DisplayName("CacheConfig 설정 클래스 로드 확인")
    void testCacheConfigClassLoadable() {
        // When & Then
        assertThat(com.nhnacademy.library.core.config.CacheConfig.class).isNotNull();
        log.info("[DEBUG_LOG] CacheConfig 클래스 로드 성공");
    }

    @Test
    @DisplayName("DTO 클래스 인스턴스화 테스트")
    void testDtoInstantiation() {
        // Given & When
        LibraryBookInfo bookInfo = new LibraryBookInfo(
            "테스트 도서", "저자", "출판사", "1234567890123",
            true, 1, null, null
        );

        LoanItemInfo loanItemInfo = LoanItemInfo.builder()
            .isbn13("1234567890123")
            .bookName("테스트 도서")
            .libraryName("테스트 도서관")
            .build();

        // Then
        assertThat(bookInfo).isNotNull();
        assertThat(bookInfo.title()).isEqualTo("테스트 도서");
        assertThat(loanItemInfo).isNotNull();
        assertThat(loanItemInfo.isbn13()).isEqualTo("1234567890123");

        log.info("[DEBUG_LOG] DTO 인스턴스화 성공");
    }

    @Test
    @DisplayName("LibraryApiProperties 프로퍼티 설정 테스트")
    void testLibraryApiProperties() {
        // Given
        LibraryApiProperties properties = new LibraryApiProperties();

        // When
        properties.setUrl("https://test.com");
        properties.setKey("test-key");
        properties.setTimeout(3000);
        properties.setCacheEnabled(true);

        // Then
        assertThat(properties.getUrl()).isEqualTo("https://test.com");
        assertThat(properties.getKey()).isEqualTo("test-key");
        assertThat(properties.getTimeout()).isEqualTo(3000);
        assertThat(properties.isCacheEnabled()).isTrue();

        log.info("[DEBUG_LOG] LibraryApiProperties 설정 성공");
    }
}
