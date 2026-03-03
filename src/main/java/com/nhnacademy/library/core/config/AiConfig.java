package com.nhnacademy.library.core.config;

import com.nhnacademy.library.ai.function.LibraryFunctions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * AI 설정
 * <p>
 * ChatModel 선택 및 ChatClient 빈을 설정합니다.
 * Step 7에서는 Function Calling을 위한 ChatClient를 추가합니다.
 * </p>
 */
@Configuration
public class AiConfig {

    @Bean
    @Primary
    @ConditionalOnProperty(name = "spring.ai.selected-model", havingValue = "gemini", matchIfMissing = true)
    public ChatModel selectedGeminiChatModel(@Qualifier("googleGenAiChatModel") ChatModel googleChatModel) {
        return googleChatModel;
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = "spring.ai.selected-model", havingValue = "ollama")
    public ChatModel selectedOllamaChatModel(@Qualifier("ollamaChatModel") ChatModel ollamaChatModel) {
        return ollamaChatModel;
    }

    /**
     * Ollama RestClient.Builder 커스텀 빈
     * <p>
     * Ollama 서버 응답이 느릴 경우 타임아웃을 늘립니다.
     * </p>
     */
    @Bean
    @Primary  // 기본 RestClient.Builder 대신 이 빈을 우선 사용
    @ConditionalOnProperty(name = "spring.ai.selected-model", havingValue = "ollama")
    public RestClient.Builder ollamaRestClientBuilder() {
        return RestClient.builder()
            .requestFactory(ClientHttpRequestFactories.get(
                ClientHttpRequestFactorySettings.DEFAULTS
                    .withConnectTimeout(Duration.ofSeconds(30))
                    .withReadTimeout(Duration.ofMinutes(5))  // 5분 타임아웃
            ));
    }

    /**
     * Library Assistant Agent용 ChatClient
     * <p>
     * Step 7: MCP Function Calling을 위한 기본 ChatClient 생성
     * Function은 AiOrchestrationService에서 호출 시점에 등록합니다.
     * </p>
     *
     * @param chatModel 선택된 ChatModel
     * @return ChatClient
     */
    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }
}
