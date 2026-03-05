package com.nhnacademy.library.core.config;

import com.nhnacademy.library.ai.function.BookSearchFunction;
import com.nhnacademy.library.ai.function.LibraryFunctions;
import com.nhnacademy.library.ai.function.LoanCheckFunction;
import com.nhnacademy.library.ai.function.ReviewFunction;
import com.nhnacademy.library.ai.function.dto.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.function.FunctionToolCallback;
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
 *   <li><b>qwen2.5:latest</b>: AiOrchestrationService용 (A2A 오케스트레이션, Function Calling)</li>
 * </ul>
 */
@Configuration
public class AiConfig {

    @Value("${spring.ai.ollama.base-url}")
    private String ollamaBaseUrl;

    @Value("${spring.ai.ollama.chat.options.duration:300}")
    private int duration;

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
     * AiOrchestrationService용: A2A 오케스트레이션과 Function Calling에 특화된 모델
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
     * Step 7: qwen2.5를 기본으로 사용 (A2A 오케스트레이션용)
     * </p>
     */
    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }

    // ========================================
    // FunctionToolCallback Beans (성능 최적화)
    // ========================================

    /**
     * 도서 검색 FunctionToolCallback
     * <p>빈으로 등록하여 재사용합니다.</p>
     */
    @Bean
    public FunctionToolCallback<BookSearchRequest, BookSearchResult> bookSearchTool(
        BookSearchFunction bookSearchFunction) {
        return FunctionToolCallback.builder("BookSearch", bookSearchFunction::searchBooks)
            .description("도서관 시스템에서 도서를 검색합니다. " +
                        "제목, 저자명으로 검색할 수 있으며, 검색어를 받아 도서 목록을 반환합니다.")
            .inputType(BookSearchRequest.class)
            .build();
    }

    /**
     * 리뷰 조회 FunctionToolCallback
     */
    @Bean
    public FunctionToolCallback<Long, ReviewResult> reviewTool(
        ReviewFunction reviewFunction) {
        return FunctionToolCallback.builder("GetReviews", reviewFunction::getReviews)
            .description("도서의 리뷰 목록을 조회합니다. " +
                        "도서 ID를 받아 리뷰 요약, 평점, 주요 리뷰를 제공합니다.")
            .inputType(Long.class)
            .build();
    }

    /**
     * 대출 가능 확인 FunctionToolCallback
     */
    @Bean
    public FunctionToolCallback<LoanCheckRequest, LoanCheckResult> loanCheckTool(
        LoanCheckFunction loanCheckFunction) {
        return FunctionToolCallback.builder("CheckLoanAvailability", loanCheckFunction::checkLoanAvailability)
            .description("도서의 대출 가능 여부를 조회합니다. " +
                        "ISBN을 받아 전국 도서관의 대출 가능 현황을 제공합니다. " +
                        "지역, 위도, 경도를 제공하면 해당 지역의 가까운 도서관을 우선으로 보여줍니다.")
            .inputType(LoanCheckRequest.class)
            .build();
    }

    // ========================================
    // 서비스 전용 ChatClient Beans
    // ========================================

    /**
     * AiLibraryAssistantService용 ChatClient
     * <p>FunctionToolCallback들이 미리 등록된 ChatClient입니다.</p>
     */
    @Bean
    @ConditionalOnProperty(name = "spring.ai.selected-model", havingValue = "ollama")
    public ChatClient libraryAssistantChatClient(
        @Qualifier("ollamaChatModel") ChatModel chatModel,
        FunctionToolCallback<BookSearchRequest, BookSearchResult> bookSearchTool,
        FunctionToolCallback<Long, ReviewResult> reviewTool,
        FunctionToolCallback<LoanCheckRequest, LoanCheckResult> loanCheckTool) {
        return ChatClient.builder(chatModel)
            .defaultToolCallbacks(bookSearchTool, reviewTool, loanCheckTool)
            .build();
    }

    /**
     * AiLibraryAssistantService용 ChatClient (Gemini)
     */
    @Bean
    @ConditionalOnProperty(name = "spring.ai.selected-model", havingValue = "gemini", matchIfMissing = true)
    public ChatClient libraryAssistantChatClientGemini(
        @Qualifier("googleGenAiChatModel") ChatModel chatModel,
        FunctionToolCallback<BookSearchRequest, BookSearchResult> bookSearchTool,
        FunctionToolCallback<Long, ReviewResult> reviewTool,
        FunctionToolCallback<LoanCheckRequest, LoanCheckResult> loanCheckTool) {
        return ChatClient.builder(chatModel)
            .defaultToolCallbacks(bookSearchTool, reviewTool, loanCheckTool)
            .build();
    }

    // ========================================
    // LibraryFunctions용 FunctionToolCallback Beans
    // ========================================

    /**
     * LibraryFunctions - SearchBooks FunctionToolCallback
     */
    @Bean
    public FunctionToolCallback<BookSearchRequest, BookSearchResult> librarySearchBooksTool(
        LibraryFunctions libraryFunctions) {
        return FunctionToolCallback.builder("SearchBooks", libraryFunctions.searchBooks())
            .description("도서관 시스템에서 도서를 검색합니다. " +
                        "제목으로 검색할 수 있으며, 검색어를 받아 도서 목록과 ISBN을 반환합니다.")
            .inputType(BookSearchRequest.class)
            .build();
    }

    /**
     * LibraryFunctions - SearchLibraries FunctionToolCallback
     */
    @Bean
    public FunctionToolCallback<LibrarySearchRequest, LibrarySearchResult> librarySearchLibrariesTool(
        LibraryFunctions libraryFunctions) {
        return FunctionToolCallback.builder("SearchLibraries", libraryFunctions.searchLibraries())
            .description("지역별 도서관을 검색합니다. " +
                        "지역 코드(숫자)를 받아 해당 지역의 도서관 목록을 반환합니다. " +
                        "**중요**: 반드시 숫자 지역 코드를 사용하세요. " +
                        "한글 지역명(광주, 서울 등)을 사용하면 API 에러가 발생합니다. " +
                        "지역 코드 예시: 서울=11, 부산=21, 대구=22, 인천=23, 광주=24, 대전=25, 울산=26, 세종=29, 경기=31")
            .inputType(LibrarySearchRequest.class)
            .build();
    }

    /**
     * LibraryFunctions - CheckBookExists FunctionToolCallback
     */
    @Bean
    public FunctionToolCallback<BookExistRequest, BookExistResult> libraryCheckBookExistsTool(
        LibraryFunctions libraryFunctions) {
        return FunctionToolCallback.builder("CheckBookExists", libraryFunctions.checkBookExists())
            .description("특정 도서관에 도서가 소장되어 있는지 확인합니다. " +
                        "도서관 코드(libCode)와 ISBN을 받아 소장 여부와 대출 가능 여부를 반환합니다.")
            .inputType(BookExistRequest.class)
            .build();
    }

    // ========================================
    // AiOrchestrationService용 ChatClient Bean
    // ========================================

    /**
     * AiOrchestrationService용 ChatClient
     * <p>LibraryFunctions의 FunctionToolCallback들이 미리 등록된 ChatClient입니다.</p>
     */
    @Bean
    @ConditionalOnProperty(name = "spring.ai.selected-model", havingValue = "ollama")
    public ChatClient orchestrationChatClient(
        @Qualifier("ollamaChatModel") ChatModel chatModel,
        FunctionToolCallback<BookSearchRequest, BookSearchResult> librarySearchBooksTool,
        FunctionToolCallback<LibrarySearchRequest, LibrarySearchResult> librarySearchLibrariesTool,
        FunctionToolCallback<BookExistRequest, BookExistResult> libraryCheckBookExistsTool) {
        return ChatClient.builder(chatModel)
            .defaultToolCallbacks(librarySearchBooksTool, librarySearchLibrariesTool, libraryCheckBookExistsTool)
            .build();
    }

    /**
     * AiOrchestrationService용 ChatClient (Gemini)
     */
    @Bean
    @ConditionalOnProperty(name = "spring.ai.selected-model", havingValue = "gemini", matchIfMissing = true)
    public ChatClient orchestrationChatClientGemini(
        @Qualifier("googleGenAiChatModel") ChatModel chatModel,
        FunctionToolCallback<BookSearchRequest, BookSearchResult> librarySearchBooksTool,
        FunctionToolCallback<LibrarySearchRequest, LibrarySearchResult> librarySearchLibrariesTool,
        FunctionToolCallback<BookExistRequest, BookExistResult> libraryCheckBookExistsTool) {
        return ChatClient.builder(chatModel)
            .defaultToolCallbacks(librarySearchBooksTool, librarySearchLibrariesTool, libraryCheckBookExistsTool)
            .build();
    }
}
