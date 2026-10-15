package net.java21.library.ai.agent.orchestrator;

import net.java21.library.ai.agent.QueryParserAgent;
import net.java21.library.ai.agent.dto.ParsedQuery;
import net.java21.library.ai.function.LibraryFunctions;
import net.java21.library.ai.function.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.stereotype.Service;

/**
 * AI Orchestration Service (MainAgent) - Function Calling 스타일
 * <p>
 * Spring AI 1.1.2 Function Calling 패턴을 따라 LLM이 스스로 도구를 선택하고 실행합니다.
 * </p>
 *
 * <p><b>아키텍처:</b></p>
 * <ol>
 *   <li><b>QueryParserAgent (llama3-korean-blossom)</b>: 쿼리 파싱 및 엔티티 추출</li>
 *   <li><b>LLM (qwen2.5)</b>: 필요한 Function 스스로 선택 및 실행</li>
 *   <li><b>LibraryFunctions</b>: 도서관정보나루 API Function</li>
 * </ol>
 *
 * <p><b>리팩토링 롤백:</b> 순환 의존성 문제로 인해 FunctionToolCallback을 Bean으로 등록하지 않고,
 * 서비스 내부에서 프로그래밍 방식으로 생성합니다.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiOrchestrationService {

    private final ChatModel chatModel;
    private final QueryParserAgent queryParserAgent;
    private final LibraryFunctions libraryFunctions;

    /**
     * 사용자 요청을 처리하고 AI 응답을 반환합니다.
     * <p>
     * <b>실행 흐름:</b>
     * </p>
     * <ol>
     *   <li>QueryParserAgent로 쿼리 파싱 (엔티티 추출)</li>
     *   <li>LLM이 필요한 Function 스스로 선택 및 실행</li>
     *   <li>결과 종합 및 자연어 응답 생성</li>
     * </ol>
     *
     * @param userMessage 사용자 메시지
     * @return AI 응답
     */
    public String ask(String userMessage) {
        long startTime = System.currentTimeMillis();
        log.info("[MainAgent] 사용자 요청 수신: {}", userMessage);

        try {
            // 1. QueryParserAgent로 쿼리 파싱 (llama3-korean-blossom)
            log.info("[MainAgent] Step 1: 쿼리 파싱 시작");
            long parseStart = System.currentTimeMillis();

            ParsedQuery parsed = queryParserAgent.parse(userMessage);

            long parseElapsed = System.currentTimeMillis() - parseStart;
            log.info("[MainAgent] 쿼리 파싱 완료 ({}ms): bookName='{}', region='{}', intent={}",
                parseElapsed, parsed.bookName(), parsed.regionName(), parsed.intent());

            if (!parsed.isValid()) {
                log.warn("[MainAgent] 파싱 실패: 유효하지 않은 쿼리");
                return "질문을 이해하지 못했습니다. 도서 검색, 도서관 조회 등으로 질문해주세요.";
            }

            // 2. FunctionToolCallback 생성
            log.info("[MainAgent] Step 2: Function Tool 등록 시작");
            long toolRegStart = System.currentTimeMillis();

            FunctionToolCallback<BookSearchRequest, BookSearchResult> searchBooksTool =
                FunctionToolCallback.builder("SearchBooks", libraryFunctions.searchBooks())
                    .description("도서관 시스템에서 도서를 검색합니다. " +
                                "제목으로 검색할 수 있으며, 검색어를 받아 도서 목록과 ISBN을 반환합니다.")
                    .inputType(BookSearchRequest.class)
                    .build();

            FunctionToolCallback<LibrarySearchRequest, LibrarySearchResult> searchLibrariesTool =
                FunctionToolCallback.builder("SearchLibraries", libraryFunctions.searchLibraries())
                    .description("지역별 도서관을 검색합니다. " +
                                "지역 코드(숫자)를 받아 해당 지역의 도서관 목록을 반환합니다. " +
                                "**중요**: 반드시 숫자 지역 코드를 사용하세요. " +
                                "한글 지역명(광주, 서울 등)을 사용하면 API 에러가 발생합니다. " +
                                "지역 코드 예시: 서울=11, 부산=21, 대구=22, 인천=23, 광주=24, 대전=25, 울산=26, 세종=29, 경기=31")
                    .inputType(LibrarySearchRequest.class)
                    .build();

            FunctionToolCallback<BookExistRequest, BookExistResult> checkBookExistsTool =
                FunctionToolCallback.builder("CheckBookExists", libraryFunctions.checkBookExists())
                    .description("특정 도서관에 도서가 소장되어 있는지 확인합니다. " +
                                "도서관 코드(libCode)와 ISBN을 받아 소장 여부와 대출 가능 여부를 반환합니다.")
                    .inputType(BookExistRequest.class)
                    .build();

            long toolRegElapsed = System.currentTimeMillis() - toolRegStart;
            log.info("[MainAgent] Function Tool 등록 완료 ({}ms)", toolRegElapsed);

            // 3. ChatClient 생성 (Function Tool 등록)
            log.info("[MainAgent] Step 3: ChatClient 생성 및 Function 등록");
            long clientBuildStart = System.currentTimeMillis();

            ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultToolCallbacks(searchBooksTool, searchLibrariesTool, checkBookExistsTool)
                .build();

            long clientBuildElapsed = System.currentTimeMillis() - clientBuildStart;
            log.info("[MainAgent] ChatClient 생성 완료 ({}ms)", clientBuildElapsed);

            // 4. System Prompt
            log.info("[MainAgent] Step 4: System Prompt 생성");
            String systemPrompt = buildSystemPrompt(parsed);

            // 5. LLM 호출 (qwen2.5)
            log.info("[MainAgent] Step 5: LLM 호출 시작 (qwen2.5)");
            log.info("[MainAgent] 사용자 메시지를 LLM에 전달하여 Function 선택 유도...");
            long llmCallStart = System.currentTimeMillis();

            String response = chatClient.prompt()
                .system(systemPrompt)
                .user(userMessage)
                .call()
                .content();

            long llmCallElapsed = System.currentTimeMillis() - llmCallStart;
            log.info("[MainAgent] LLM 호출 완료 ({}ms)", llmCallElapsed);

            // 총 소요시간
            long totalElapsed = System.currentTimeMillis() - startTime;
            log.info("[MainAgent] 응답 생성 완료 (총 {}ms)", totalElapsed);

            return response;

        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("[MainAgent] 요청 처리 실패 ({}ms)", elapsed, e);
            return "요청 처리 중 오류가 발생했습니다. 다시 시도해주세요.";
        }
    }

    /**
     * System Prompt 생성 (Telegram 메신저에 최적화)
     */
    private String buildSystemPrompt(ParsedQuery parsed) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("당신은 도서관 도우미 AI입니다.\n\n");

        // 파싱된 정보가 있으면 힌트 제공
        if (parsed.hasBookName()) {
            prompt.append(String.format("검색 도서: %s\n\n", parsed.bookName()));
        }
        if (parsed.hasRegionName() && parsed.regionCode() != null) {
            prompt.append(String.format("**지역 정보:** %s (지역 코드: %s)\n", parsed.regionName(), parsed.regionCode()));
            prompt.append(String.format("**⚠️ 매우 중요:** SearchLibraries 함수를 호출할 때 반드시 지역 코드 \"%s\"를 사용하세요.\n", parsed.regionCode()));
            prompt.append(String.format("절대 한글 지역명(\"%s\")을 region 파라미터에 넣지 마세요. API 에러가 발생합니다.\n\n", parsed.regionName()));
        }

        prompt.append("**중요한 지침:**\n");
        prompt.append("1. 반드시 한국어로 답변하세요.\n");
        prompt.append("2. Telegram 메신저에 맞춰 간단 명료하게 작성하세요.\n");
        prompt.append("3. 제목(#, ##) 대신 볼드(*text*)를 사용하세요.\n");
        prompt.append("4. 줄바꿈을 적절히 사용하여 가독성을 높이세요.\n\n");

        prompt.append("**도구 호출 가이드:**\n");
        prompt.append("- SearchBooks: 도서 제목으로 검색하여 ISBN 확보\n");
        prompt.append("- SearchLibraries: 지역 코드(숫자)로 도서관 검색 (한글 지역명 사용 금지!)\n");
        prompt.append("- GetLibrariesWithBook: ISBN으로 특정 도서의 대출 가능 도서관 조회\n");
        prompt.append("- CheckBookExists: 도서관 코드와 ISBN으로 소장 여부 확인\n\n");

        prompt.append("**⚠️ 매우 중요 - 각 도서별로 대출 가능 여부 확인:**\n");
        prompt.append("검색된 도서가 여러 권인 경우, 각 도서마다 GetLibrariesWithBook을 호출하여 ");
        prompt.append("해당 도서의 대출 가능 여부를 개별적으로 확인해야 합니다.\n");
        prompt.append("절대 여러 도서를 합쳐서 대출 가능 도서관을 표시하지 마세요.\n\n");

        prompt.append("**중요:** 도서관이 많을 경우는 최대 10~15개만 보여주고, " );
        prompt.append("\"나머지 도서관이 있습니다. 전체 목록이 필요하시면 말씀해주세요.\"라고 안내해 주세요.\n\n");

        prompt.append("**응답 형식 (Telegram용) - 각 도서별로 표시:**\n\n");
        prompt.append("*도서 검색 결과*\n\n");
        prompt.append("1. *도서명* (ISBN: xxx)\n");
        prompt.append("저자: xxx | 출판사: xxx\n");
        prompt.append("대출 가능한 도서관:\n");
        prompt.append("✅ 도서관명 (코드: xxxxx)\n");
        prompt.append("대출 불가능한 도서관:\n");
        prompt.append("❌ 도서관명 - 사유\n");
        prompt.append("━━━━━━━━━━━━━━━━━━━━\n\n");
        prompt.append("2. *도서명* (ISBN: xxx)\n");
        prompt.append("저자: xxx | 출판사: xxx\n");
        prompt.append("대출 가능한 도서관:\n");
        prompt.append("✅ 도서관명 (코드: xxxxx)\n");
        prompt.append("대출 불가능한 도서관:\n");
        prompt.append("❌ 도서관명 - 사유\n");
        prompt.append("━━━━━━━━━━━━━━━━━━━━\n\n");

        return prompt.toString();
    }
}
