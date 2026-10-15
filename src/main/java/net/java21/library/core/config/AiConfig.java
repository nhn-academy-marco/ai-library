package net.java21.library.core.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * AI 설정
 * <p>
 * Step 7: 두 개의 LLM을 분리하여 사용합니다.
 * </p>
 * <ul>
 *   <li><b>llama3-korean-blossom</b>: QueryParserAgent용 (한국어 쿼리 파싱)</li>
 *   <li><b>qwen2.5:latest</b>: AiOrchestrationService용 (Tool 기반 오케스트레이션, Function Calling)</li>
 * </ul>
 */
@Configuration
public class AiConfig {

    @Value("${spring.ai.ollama.base-url}")
    private String ollamaBaseUrl;

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
     * qwen2.5 ChatClient
     * <p>
     * AiOrchestrationService용: Tool 기반 오케스트레이션과 Function Calling에 특화된 모델
     * </p>
     */
    @Bean
    @ConditionalOnProperty(name = "spring.ai.selected-model", havingValue = "ollama")
    public ChatClient qwenChatClient(@Qualifier("ollamaChatModel") ChatModel ollamaChatModel) {
        return ChatClient.builder(ollamaChatModel).build();
    }

    /**
     * llama3-korean-blossom ChatClient
     * <p>
     * QueryParserAgent용: 한국어 쿼리 파싱 전용 ChatClient
     * 기본 ollamaChatModel(qwen2.5)을 사용합니다.
     * </p>
     * <p>
     * <b>참고:</b> 현재는 qwen2.5를 사용하며, 필요시 llama3-korean-blossom로 변경 가능합니다.
     * llama3-korean-blossom 사용 시 QueryParserAgent에서 options()로 모델 지정 필요.
     * </p>
     */
    @Bean
    @ConditionalOnProperty(name = "spring.ai.selected-model", havingValue = "ollama")
    public ChatClient llama3ChatClient(@Qualifier("ollamaChatModel") ChatModel ollamaChatModel) {
        return ChatClient.builder(ollamaChatModel).build();
    }

    /**
     * 기본 ChatClient
     * <p>
     * Step 7: qwen2.5를 기본으로 사용 (Tool 기반 오케스트레이션용)
     * </p>
     */
    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }
}
