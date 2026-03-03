package com.nhnacademy.library.ai.agent.orchestrator;

import com.nhnacademy.library.ai.agent.QueryParserAgent;
import com.nhnacademy.library.ai.agent.dto.ParsedQuery;
import com.nhnacademy.library.ai.function.LibraryFunctions;
import com.nhnacademy.library.ai.function.dto.*;
import com.nhnacademy.library.external.opennaru.util.RegionCodeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.stereotype.Service;

/**
 * AI Orchestration Service (MainAgent) - Function Calling 스타일
 * <p>
 * Spring AI 1.1.3 Function Calling 패턴을 따라 LLM이 스스로 도구를 선택하고 실행합니다.
 * </p>
 *
 * <p><b>아키텍처:</b></p>
 * <ol>
 *   <li><b>QueryParserAgent (llama3-korean-blossom)</b>: 쿼리 파싱 및 엔티티 추출</li>
 *   <li><b>LLM (qwen2.5)</b>: 필요한 Function 스스로 선택 및 실행</li>
 *   <li><b>LibraryFunctions</b>: 도서관정보나루 API Function</li>
 * </ol>
 *
 * <p><b>변경 사항:</b></p>
 * <ul>
 *   <li>복잡한 Agent 선택 로직 제거 (selectAgents, executeAgentsWithA2A)</li>
 *   <li>LLM이 스스로 도구 선택 (FunctionToolCallback)</li>
 *   <li>코드 370줄 → 100줄로 간소화</li>
 * </ul>
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
        log.info("[MainAgent] 사용자 요청 수신: {}", userMessage);

        try {
            // 1. QueryParserAgent로 쿼리 파싱 (llama3-korean-blossom)
            ParsedQuery parsed = queryParserAgent.parse(userMessage);
            log.info("[MainAgent] 파싱 결과: {}", parsed.toDebugString());

            if (!parsed.isValid()) {
                return "질문을 이해하지 못했습니다. 도서 검색, 도서관 조회 등으로 질문해주세요.";
            }

            // 2. FunctionToolCallback 생성
            FunctionToolCallback<BookSearchRequest, BookSearchResult> searchBooksTool =
                FunctionToolCallback.builder("SearchBooks", libraryFunctions.searchBooks())
                    .description("도서관 시스템에서 도서를 검색합니다. " +
                                "제목으로 검색할 수 있으며, 검색어를 받아 도서 목록과 ISBN을 반환합니다.")
                    .inputType(BookSearchRequest.class)
                    .build();

            FunctionToolCallback<LibrarySearchRequest, LibrarySearchResult> searchLibrariesTool =
                FunctionToolCallback.builder("SearchLibraries", libraryFunctions.searchLibraries())
                    .description("지역별 도서관을 검색합니다. " +
                                "지역명(예: 광주, 서울, 부산)을 받아 해당 지역의 도서관 목록을 반환합니다. " +
                                "RegionCodeMapper를 사용하여 지역명을 코드로 변환하세요: 광주=29, 서울=11, 부산=26")
                    .inputType(LibrarySearchRequest.class)
                    .build();

            FunctionToolCallback<BookExistRequest, BookExistResult> checkBookExistsTool =
                FunctionToolCallback.builder("CheckBookExists", libraryFunctions.checkBookExists())
                    .description("특정 도서관에 도서가 소장되어 있는지 확인합니다. " +
                                "도서관 코드(libCode)와 ISBN을 받아 소장 여부와 대출 가능 여부를 반환합니다.")
                    .inputType(BookExistRequest.class)
                    .build();

            // 3. ChatClient 생성 (Function Tool 등록)
            ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultToolCallbacks(searchBooksTool, searchLibrariesTool, checkBookExistsTool)
                .build();

            // 4. System Prompt
            String systemPrompt = buildSystemPrompt(parsed);

            // 5. LLM 호출 (qwen2.5)
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

    /**
     * System Prompt 생성
     */
    private String buildSystemPrompt(ParsedQuery parsed) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("당신은 도서관 도우미 AI입니다.\n\n");
        prompt.append("**중요:**\n");
        prompt.append("- 반드시 한국어로 답변하세요.\n");
        prompt.append("- 사용자에게 한눈에 보이도록 정보를 정리해주세요.\n\n");

        // 파싱된 정보가 있으면 힌트 제공
        if (parsed.hasBookName()) {
            prompt.append(String.format("**검색할 도서:** %s\n\n", parsed.bookName()));
        }
        if (parsed.hasRegionName()) {
            String regionCode = RegionCodeMapper.convert(parsed.regionName());
            prompt.append(String.format("**지역:** %s (코드: %s)\n\n", parsed.regionName(), regionCode));
        }

        prompt.append("**도구 호출 가이드:**\n");
        prompt.append("1. SearchBooks: 도서 제목으로 검색하여 ISBN 확보\n");
        prompt.append("2. SearchLibraries: 지역별 도서관 검색 (지역명→코드 변환 필요)\n");
        prompt.append("3. CheckBookExists: 도서관 코드와 ISBN으로 소장 여부 확인\n\n");

        prompt.append("**출력 형식:**\n\n");
        prompt.append("## 도서별 대출 가능 도서관\n\n");
        prompt.append("### 1. [도서명] (ISBN: xxx)\n\n");
        prompt.append("**대출 가능한 도서관:**\n");
        prompt.append("- ✅ 도서관명 (libCode: xxxxx)\n");
        prompt.append("- ✅ 도서관명 (libCode: xxxxx)\n\n");
        prompt.append("**대출 불가능한 도서관:**\n");
        prompt.append("- ❌ 도서관명 (libCode: xxxxx) - 미소장\n\n");
        prompt.append("---\n\n");

        return prompt.toString();
    }
}
