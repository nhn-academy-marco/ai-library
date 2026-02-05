package com.nhnacademy.library.core.book.service.ai;
import com.nhnacademy.library.core.book.service.search.BookSearchService;
import com.nhnacademy.library.core.book.service.embedding.EmbeddingService;
import com.nhnacademy.library.core.book.service.embedding.BookEmbeddingService;
import com.nhnacademy.library.core.review.service.ReviewSummarizer;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
class GeminiApiTest {

    @Autowired
    @Qualifier("googleGenAiChatModel")
    private ChatModel chatModel;

    @Test
    @Disabled("Gemini API 토큰 용량 문제로 테스트 제외")
    @DisplayName("Gemini API 직접 호출 테스트")
    void testGeminiCall() {
        // Given
        String message = "안녕하세요, 만나서 반갑습니다. 자기소개를 간단히 해주세요.";

        // When & Then
        try {
            String response = chatModel.call(message);
            log.info("[DEBUG_LOG] Gemini Response: {}", response);
            assertThat(response).isNotEmpty();
        } catch (Exception e) {
            log.error("[DEBUG_LOG] Gemini API Call Failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Test
    @Disabled("Gemini API 토큰 용량 문제로 테스트 제외")
    @DisplayName("ChatModel을 통한 Gemini 호출 테스트")
    void testBookAiServiceCall() {
        // Given
        String prompt = "당신은 도서관 사서입니다. '자바 프로그래밍'에 대해 간단히 설명해주세요.";

        // When
        String response = chatModel.call(prompt);

        // Then
        log.info("[DEBUG_LOG] ChatModel Response: {}", response);
        assertThat(response).isNotEmpty();
        assertThat(response).contains("자바");
    }
}
