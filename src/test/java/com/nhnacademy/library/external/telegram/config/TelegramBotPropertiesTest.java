package com.nhnacademy.library.external.telegram.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TelegramBotProperties 테스트
 */
@DisplayName("Telegram Bot Properties 테스트")
class TelegramBotPropertiesTest {

    @Test
    @DisplayName("Token Setter/Getter 테스트")
    void testTokenSetterGetter() {
        // Given
        TelegramBotProperties properties = new TelegramBotProperties();

        // When
        properties.setToken("test-token-12345");

        // Then
        assertThat(properties.getToken()).isEqualTo("test-token-12345");
    }

    @Test
    @DisplayName("Username Setter/Getter 테스트")
    void testUsernameSetterGetter() {
        // Given
        TelegramBotProperties properties = new TelegramBotProperties();

        // When
        properties.setUsername("test_bot_username");

        // Then
        assertThat(properties.getUsername()).isEqualTo("test_bot_username");
    }

    @Test
    @DisplayName("여러 번 설정 가능 확인")
    void testMultipleSetters() {
        // Given
        TelegramBotProperties properties = new TelegramBotProperties();

        // When
        properties.setToken("first-token");
        properties.setUsername("first-username");
        properties.setToken("second-token");
        properties.setUsername("second-username");

        // Then
        assertThat(properties.getToken()).isEqualTo("second-token");
        assertThat(properties.getUsername()).isEqualTo("second-username");
    }
}
