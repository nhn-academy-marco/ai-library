package com.nhnacademy.library.core.book.service.ai;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
@TestPropertySource(properties = {
    "spring.ai.selected-model=ollama",
    "rabbitmq.queue.review-summary=nhnacademy-library-review"
})
class OllamaApiTest {

    @Autowired
    private ChatModel chatModel;

    @Test
    @Disabled
    @DisplayName("Ollama API 기본 호출 테스트")
    void testOllamaCall() {
        // Given
        String message = "안녕하세요, 만나서 반갑습니다. 자기소개를 간단히 해주세요.";

        // When & Then
        try {
            // Note: ollama.java21.net might be slow or unreachable in some environments
            String response = chatModel.call(message);
            log.info("[DEBUG_LOG] Ollama Response: {}", response);
            assertThat(response).isNotEmpty();
        } catch (Exception e) {
            log.warn("[DEBUG_LOG] Ollama API Call Failed: {}", e.getMessage());
            // We don't necessarily want to fail the build if the external API is down, 
            // but we want to see it try.
        }
    }

    @Test
    @Disabled
    @DisplayName("ChatModel을 통한 Ollama 호출 테스트")
    void testBookAiServiceCallWithOllama() {
        // Given
        String prompt = "당신은 도서관 사서입니다. '파이썬 프로그래밍'에 대해 간단히 설명해주세요.";

        // When
        try {
            String response = chatModel.call(prompt);
            // Then
            log.info("[DEBUG_LOG] ChatModel (Ollama) Response: {}", response);
            assertThat(response).isNotEmpty();
        } catch (Exception e) {
            log.warn("[DEBUG_LOG] ChatModel (Ollama) Call Failed: {}", e.getMessage());
        }
    }
}
