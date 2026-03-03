package com.nhnacademy.library.ai.agent.orchestrator;

import com.nhnacademy.library.ai.function.LibraryFunctions;
import com.nhnacademy.library.ai.function.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.stereotype.Service;

/**
 * AI Orchestration Service (MainAgent)
 * <p>
 * Step 7: LLM 기반 Function Calling 오케스트레이션
 * </p>
 * <p>
 * LLM이 스스로 필요한 Function을 선택하고 호출하여 복잡한 사용자 요청을 처리합니다.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiOrchestrationService {

    private final ChatModel chatModel;
    private final LibraryFunctions libraryFunctions;

    /**
     * 사용자 요청을 처리하고 AI 응답을 반환합니다.
     * <p>
     * Step 7: LLM이 스스로 Function을 선택하고 호출합니다.
     * </p>
     *
     * @param userMessage 사용자 메시지
     * @return AI 응답
     */
    public String ask(String userMessage) {
        log.info("[MainAgent] 사용자 요청 수신: {}", userMessage);

        try {
            // Function Tool Callbacks 생성
            FunctionToolCallback<BookSearchRequest, BookSearchResult> searchBooksTool =
                FunctionToolCallback.builder("searchBooks", libraryFunctions.searchBooks())
                    .description("도서관 시스템에서 도서를 검색합니다. 제목, 저자명으로 검색할 수 있습니다.")
                    .inputType(BookSearchRequest.class)
                    .build();

            FunctionToolCallback<LoanAvailabilityRequest, LoanAvailabilityResult> librariesWithBookTool =
                FunctionToolCallback.builder("getLibrariesWithBook", libraryFunctions.getLibrariesWithBook())
                    .description("특정 도서를 소장한 도서관 목록을 조회합니다. ISBN과 지역 코드로 검색합니다. 지역 코드: 11=서울, 26=부산, 27=대구, 28=인천, 29=광주, 30=대전, 31=울산, 36=세종, 41=경기, 42=강원, 43=충북, 44=충남, 45=전북, 46=전남, 47=경북, 48=경남, 50=제주")
                    .inputType(LoanAvailabilityRequest.class)
                    .build();

            FunctionToolCallback<LibrarySearchRequest, LibrarySearchResult> searchLibrariesTool =
                FunctionToolCallback.builder("searchLibraries", libraryFunctions.searchLibraries())
                    .description("도서관을 검색합니다. 지역 코드나 도서관명으로 검색할 수 있습니다.")
                    .inputType(LibrarySearchRequest.class)
                    .build();

            FunctionToolCallback<BookDetailRequest, BookDetailResult> getBookDetailTool =
                FunctionToolCallback.builder("getBookDetail", libraryFunctions.getBookDetail())
                    .description("도서의 상세 정보를 조회합니다. ISBN으로 검색합니다.")
                    .inputType(BookDetailRequest.class)
                    .build();

            FunctionToolCallback<PopularBooksRequest, PopularBooksResult> getPopularBooksTool =
                FunctionToolCallback.builder("getPopularBooks", libraryFunctions.getPopularBooks())
                    .description("특정 기간 동안의 인기 대출 도서를 조회합니다. startDt와 endDt는 YYYY-MM-DD 형식입니다.")
                    .inputType(PopularBooksRequest.class)
                    .build();

            FunctionToolCallback<HotTrendRequest, HotTrendResult> getHotTrendBooksTool =
                FunctionToolCallback.builder("getHotTrendBooks", libraryFunctions.getHotTrendBooks())
                    .description("최근 7일간 대출 순위가 급상승한 도서를 조회합니다. searchDt는 YYYY-MM-DD 형식입니다.")
                    .inputType(HotTrendRequest.class)
                    .build();

            FunctionToolCallback<RecommendRequest, RecommendResult> getRecommendedBooksTool =
                FunctionToolCallback.builder("getRecommendedBooks", libraryFunctions.getRecommendedBooks())
                    .description("특정 도서와 관련된 추천도서를 조회합니다. type는 'mania'(마니아) 또는 'reader'(다독자)입니다.")
                    .inputType(RecommendRequest.class)
                    .build();

            // ChatClient 생성 (Function Tool 등록)
            ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultToolCallbacks(
                    searchBooksTool,
                    librariesWithBookTool,
                    searchLibrariesTool,
                    getBookDetailTool,
                    getPopularBooksTool,
                    getHotTrendBooksTool,
                    getRecommendedBooksTool
                )
                .build();

            String systemPrompt = """
                당신은 도서관 도우미 AI입니다.

                **중요:**
                - 반드시 한국어로 답변하세요.
                - 코드를 작성하지 말고, 직접 Function을 호출하세요.
                - 사용자의 질문에 친절하고 명확하게 답변하세요.

                **사용 가능한 기능:**
                - searchBooks: 도서 검색 (제목, 저자명)
                - getLibrariesWithBook: 대출 가능 도서관 조회 (ISBN, 지역)
                - searchLibraries: 도서관 검색 (지역, 도서관명)
                - getBookDetail: 도서 상세 정보 조회
                - getPopularBooks: 인기대출도서 조회
                - getHotTrendBooks: 대출 급상승 도서 조회
                - getRecommendedBooks: 추천도서 조회

                **지역 코드 (광주=29):**
                11=서울, 26=부산, 27=대구, 28=인천, 29=광주, 30=대전, 31=울산,
                36=세종, 41=경기, 42=강원, 43=충북, 44=충남, 45=전북, 46=전남,
                47=경북, 48=경남, 50=제주

                **응답 스타일:**
                - 필요한 경우 Function을 호출하여 정보를 조회하세요.
                - 결과를 사용자에게 친절하게 설명하세요.
                - 여러 Function을 조합하여 복잡한 질문에 답변하세요.
                """;

            String response = chatClient.prompt()
                .system(systemPrompt)
                .user(userMessage)
                .call()
                .content();

            log.info("[MainAgent] 응답 생성 완료");
            return response;

        } catch (Exception e) {
            log.error("[MainAgent] 요청 처리 실패: userMessage={}", userMessage, e);
            return "요청 처리 중 오류가 발생했습니다. 다시 시도해주세요.";
        }
    }
}
