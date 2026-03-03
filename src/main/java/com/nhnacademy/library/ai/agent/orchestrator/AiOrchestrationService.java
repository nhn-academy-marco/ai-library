package com.nhnacademy.library.ai.agent.orchestrator;

import com.nhnacademy.library.ai.agent.*;
import com.nhnacademy.library.ai.agent.dto.ParsedQuery;
import com.nhnacademy.library.external.opennaru.client.LibraryInfoNaruApiClient;
import com.nhnacademy.library.external.opennaru.dto.BookExistInfo;
import com.nhnacademy.library.external.opennaru.dto.LibraryInfo;
import com.nhnacademy.library.external.opennaru.util.RegionCodeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI Orchestration Service (MainAgent)
 * <p>
 * A2A (Agent-to-Agent) 패턴 기반 오케스트레이션
 * </p>
 * <p>
 * 여러 전문 Agent를 조합하여 복잡한 사용자 요청을 처리합니다.
 * </p>
 *
 * <p><b>2단계 파이프라인:</b></p>
 * <ol>
 *   <li><b>QueryParserAgent (llama3-korean-blossom)</b>: 쿼리 파싱 및 엔티티 추출</li>
 *   <li><b>A2A 오케스트레이션 (qwen2.5)</b>: Agent 조합 및 실행</li>
 * </ol>
 *
 * <p><b>사용 모델:</b> qwen2.5 (Function Calling 지원)</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiOrchestrationService {

    private final ChatClient qwenChatClient;  // qwen2.5 전용 (A2A 오케스트레이션)
    private final QueryParserAgent queryParserAgent;  // llama3-korean-blossom 전용 (쿼리 파싱)
    private final BookAgent bookAgent;
    private final LibraryAgent libraryAgent;
    private final LibraryExtendedAgent libraryExtendedAgent;
    private final LibraryInfoNaruApiClient libraryInfoNaruApiClient;

    /**
     * 사용자 요청을 처리하고 AI 응답을 반환합니다.
     * <p>
     * <b>하이브리드 오케스트레이션:</b>
     * </p>
     * <ol>
     *   <li>llama3-korean-blossom으로 쿼리 파싱 (정확한 엔티티 추출)</li>
     *   <li>A2A로 정보 수집 (정확성 보장)</li>
     *   <li>qwen2.5로 결과 재구성 (유연성 확보)</li>
     * </ol>
     *
     * @param userMessage 사용자 메시지
     * @return AI 응답
     */
    public String ask(String userMessage) {
        log.info("[MainAgent] 사용자 요청 수신: {}", userMessage);

        try {
            // 1. llama3-korean-blossom으로 쿼리 파싱 (엔티티 추출)
            var parsedQuery = queryParserAgent.parse(userMessage);
            log.info("[MainAgent] 파싱 결과: {}", parsedQuery.toDebugString());

            if (!parsedQuery.isValid()) {
                return "질문을 이해하지 못했습니다. 도서 검색, 도서관 조회 등으로 질문해주세요.";
            }

            // 2. 파싱된 정보로 Agent 선택
            List<AgentType> selectedAgents = selectAgentsByParsedQuery(parsedQuery);
            log.info("[MainAgent] 선택된 Agent: {}", selectedAgents);

            if (selectedAgents.isEmpty()) {
                return "질문을 이해하지 못했습니다. 도서 검색, 도서관 조회 등으로 질문해주세요.";
            }

            // 3. A2A로 정보 수집 (정확성 보장)
            Map<AgentType, String> agentResults = executeAgentsWithA2A(parsedQuery, selectedAgents);

            // 4. qwen2.5로 결과 재구성 (유연성 확보)
            String finalResponse = synthesizeWithLLM(userMessage, agentResults);

            log.info("[MainAgent] 응답 생성 완료");
            return finalResponse;

        } catch (Exception e) {
            log.error("[MainAgent] 요청 처리 실패: userMessage={}", userMessage, e);
            return "요청 처리 중 오류가 발생했습니다. 다시 시도해주세요.";
        }
    }

    /**
     * 파싱된 쿼리를 분석하여 필요한 Agent를 선택합니다.
     * <p>
     * LLM 기반 파싱 결과를 활용하여 정확하게 Agent를 선택합니다.
     * </p>
     */
    private List<AgentType> selectAgentsByParsedQuery(ParsedQuery parsedQuery) {
        List<AgentType> selectedAgents = new ArrayList<>();

        // 도서명이 있거나 검색 의도인 경우 → BookAgent
        if (parsedQuery.hasBookName() ||
            parsedQuery.intent() == ParsedQuery.SearchIntent.SEARCH ||
            parsedQuery.intent() == ParsedQuery.SearchIntent.CHECK_AVAILABILITY) {
            selectedAgents.add(AgentType.BOOK);
        }

        // 지역명이 있는 경우 → LibraryAgent + LibraryExtendedAgent
        if (parsedQuery.hasRegionName()) {
            selectedAgents.add(AgentType.LIBRARY);
            selectedAgents.add(AgentType.LIBRARY_EXTENDED);
        }

        return selectedAgents;
    }

    /**
     * A2A 패턴으로 Agent들을 실행하고 결과를 공유합니다.
     * <p>
     * <b>중요:</b> 파싱된 쿼리 정보를 사용하여 정확한 입력을 각 Agent에 전달합니다.
     * </p>
     *
     * @param parsedQuery LLM으로 파싱된 쿼리 정보
     * @param selectedAgents 실행할 Agent 목록
     * @return Agent 실행 결과
     */
    private Map<AgentType, String> executeAgentsWithA2A(ParsedQuery parsedQuery, List<AgentType> selectedAgents) {
        Map<AgentType, String> results = new EnumMap<>(AgentType.class);

        for (AgentType agentType : selectedAgents) {
            log.info("[MainAgent] {} Agent 실행 시작...", agentType);

            // 파싱된 정보로 query 추출 (정확한 입력 전달)
            String query = extractQueryFromParsed(parsedQuery, agentType, results);
            log.debug("[MainAgent] {} Agent에 전달할 query: {}", agentType, query);

            // 이전 Agent 결과 전달 (A2A 정보 공유)
            AgentRequest request = AgentRequest.builder()
                .agentType(agentType)
                .userMessage(parsedQuery.originalQuery())
                .query(query)
                .sharedResults(results)
                .build();

            String result = executeAgent(agentType, request);
            results.put(agentType, result);

            log.info("[MainAgent] {} Agent 실행 완료", agentType);
        }

        // BOOK + LIBRARY 조합인 경우, bookExist 직접 실행
        if (results.containsKey(AgentType.BOOK) &&
            results.containsKey(AgentType.LIBRARY) &&
            !results.containsKey(AgentType.LIBRARY_EXTENDED)) {

            log.info("[MainAgent] bookExist API 직접 호출 시작...");
            String bookExistResult = executeBookExistDirect(results, parsedQuery.originalQuery());
            results.put(AgentType.LIBRARY_EXTENDED, bookExistResult);
        }

        return results;
    }

    /**
     * bookExist API를 직접 실행합니다 (A2A 완성체)
     *
     * @param results Agent 실행 결과
     * @param originalUserMessage 원본 사용자 메시지 (지역명 추출용)
     */
    private String executeBookExistDirect(Map<AgentType, String> results, String originalUserMessage) {
        StringBuilder response = new StringBuilder();
        response.append("**bookExist API 호출 결과:**\n\n");

        // BookAgent 결과에서 ISBN 추출
        String bookResult = results.get(AgentType.BOOK);
        String isbn = extractIsbnFromBookResult(bookResult);

        if (isbn == null) {
            return "ISBN을 찾을 수 없어 bookExist를 조회할 수 없습니다.";
        }

        // 원본 사용자 메시지에서 지역 코드 추출 (BookAgent/LibraryAgent 결과가 아닌 원본 메시지 사용)
        String region = extractRegionName(originalUserMessage);
        log.info("[executeBookExistDirect] 원본 메시지에서 추출한 지역: {}", region);

        String regionCode = RegionCodeMapper.convert(region);
        log.info("[executeBookExistDirect] 지역 코드: {}", regionCode);

        if (regionCode == null) {
            return "지역을 찾을 수 없어 bookExist를 조회할 수 없습니다.";
        }

        // 도서관 리스트 조회
        List<LibraryInfo> libraries = libraryInfoNaruApiClient.searchLibrariesByRegion(regionCode);
        log.info("[executeBookExistDirect] 조회된 도서관 수: {}", libraries.size());

        // 상위 5개 도서관만 확인 (성능 고려)
        int checkCount = Math.min(5, libraries.size());

        for (int i = 0; i < checkCount; i++) {
            LibraryInfo library = libraries.get(i);
            BookExistInfo existInfo = libraryExtendedAgent.checkBookExists(library.libCode(), isbn);

            if (existInfo != null) {
                response.append(String.format("- **%s** (libCode: %s)\n",
                    library.libName(), library.libCode()));
                response.append(String.format("  소장: %s\n",
                    existInfo.exists() ? "✅ 소장 중" : "❌ 미소장"));

                if (existInfo.exists() && "Y".equals(existInfo.loanAvailable())) {
                    response.append("  대출 가능: ✅\n");
                }

                response.append("\n");
            }
        }

        return response.toString();
    }

    /**
     * Agent를 실행합니다.
     */
    private String executeAgent(AgentType agentType, AgentRequest request) {
        return switch (agentType) {
            case BOOK -> bookAgent.execute(request);
            case LIBRARY -> libraryAgent.execute(request);
            case LIBRARY_EXTENDED -> libraryExtendedAgent.execute(request);
            default -> "";
        };
    }

    /**
     * Agent 타입별로 쿼리를 추출합니다.
     * <p>
     * <b>중요:</b> 파싱된 쿼리 정보를 활용하여 정확한 입력을 전달합니다.
     * </p>
     *
     * @param parsedQuery LLM으로 파싱된 쿼리 정보
     * @param agentType Agent 타입
     * @param previousResults 이전 Agent 실행 결과
     * @return 해당 Agent에 전달할 query
     */
    private String extractQueryFromParsed(ParsedQuery parsedQuery, AgentType agentType, Map<AgentType, String> previousResults) {
        return switch (agentType) {
            case BOOK -> {
                // 파싱된 도서명이 있으면 사용, 없으면 원본 메시지 사용
                if (parsedQuery.hasBookName()) {
                    yield parsedQuery.bookName();
                } else {
                    yield parsedQuery.originalQuery();
                }
            }
            case LIBRARY -> {
                // 파싱된 지역명 사용 (명확한 지역명 전달)
                if (parsedQuery.hasRegionName()) {
                    yield parsedQuery.regionName();
                } else {
                    yield "";  // 지역명 없으면 빈 문자열
                }
            }
            case LIBRARY_EXTENDED -> {
                // BookAgent 결과에서 ISBN 추출
                if (previousResults.containsKey(AgentType.BOOK)) {
                    String bookResult = previousResults.get(AgentType.BOOK);
                    String isbn = extractIsbnFromBookResult(bookResult);
                    // LibraryAgent 결과에서 도서관 코드 추출
                    if (previousResults.containsKey(AgentType.LIBRARY)) {
                        String libResult = previousResults.get(AgentType.LIBRARY);
                        List<String> libCodes = extractLibCodesFromLibraryResult(libResult);
                        yield "isbn:" + isbn + " libCodes:" + String.join(",", libCodes);
                    }
                    yield "isbn:" + isbn;
                }
                yield parsedQuery.originalQuery();
            }
            default -> parsedQuery.originalQuery();
        };
    }

    /**
     * 지역명을 추출합니다.
     */
    private String extractRegionName(String message) {
        String[] regions = {
            "서울", "부산", "대구", "인천", "광주", "대전", "울산", "세종",
            "경기", "강원", "충북", "충남", "전북", "전남", "경북", "경남", "제주"
        };

        log.debug("[extractRegionName] 입력 메시지: {}", message);

        for (String region : regions) {
            if (message.contains(region)) {
                log.info("[extractRegionName] 지역 추출 성공: '{}' (from: {})", region, message);
                return region;
            }
        }

        log.warn("[extractRegionName] 지역 추출 실패: {}", message);
        return null;
    }

    /**
     * BookAgent 결과에서 ISBN을 추출합니다.
     */
    private String extractIsbnFromBookResult(String bookResult) {
        Pattern isbnPattern = Pattern.compile("ISBN[:\\s]*(\\d{13})");
        Matcher matcher = isbnPattern.matcher(bookResult);

        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * LibraryAgent 결과에서 도서관 코드를 추출합니다.
     */
    private List<String> extractLibCodesFromLibraryResult(String libraryResult) {
        List<String> libCodes = new ArrayList<>();
        // 도서관 코드를 추출하는 정규식 (예: (129001))
        Pattern libCodePattern = Pattern.compile("\\((\\d{6})\\)");
        Matcher matcher = libCodePattern.matcher(libraryResult);

        while (matcher.find()) {
            libCodes.add(matcher.group(1));
        }

        return libCodes;
    }

    /**
     * LLM을 활용한 결과 종합 (하이브리드 방식)
     * <p>
     * A2A로 수집된 정보를 LLM에게 전달하여 유연하게 재구성합니다.
     * </p>
     */
    private String synthesizeWithLLM(String userMessage, Map<AgentType, String> agentResults) {
        log.info("[MainAgent] LLM 결과 재구성 시작...");

        try {
            // A2A 수집 정보 구조화
            StringBuilder collectedInfo = new StringBuilder();
            collectedInfo.append("**사용자 질문:**\n").append(userMessage).append("\n\n");

            // 도서 검색 결과
            if (agentResults.containsKey(AgentType.BOOK)) {
                collectedInfo.append("**검색된 도서:**\n").append(agentResults.get(AgentType.BOOK)).append("\n\n");
            }

            // 도서관 리스트
            if (agentResults.containsKey(AgentType.LIBRARY)) {
                collectedInfo.append("**지역 도서관:**\n").append(agentResults.get(AgentType.LIBRARY)).append("\n\n");
            }

            // bookExist 결과 (실제 소장 여부)
            if (agentResults.containsKey(AgentType.LIBRARY_EXTENDED)) {
                collectedInfo.append("**실제 소장 확인 (bookExist API):**\n").append(agentResults.get(AgentType.LIBRARY_EXTENDED)).append("\n\n");
            }

            // LLM에게 전달할 시스템 프롬프트
            String systemPrompt = """
                당신은 도서관 도우미 AI입니다.

                **중요:**
                - 반드시 한국어로 답변하세요.
                - 사용자에게 한눈에 보이도록 정보를 정리해주세요.

                **출력 형식: 도서별 대출 가능 도서관 표**

                사용자가 "광주에서 토비의 스프링 빌릴까?"라고 물어보면:

                ┌─────────────────────┬──────────────────────────────┐
                │ 도서                 │ 대출 가능 도서관             │
                ├─────────────────────┼──────────────────────────────┤
                │ 토비의 스프링 3.1   │ ✅ 광주광역시립북구도서관     │
                │ (ISBN: 97911...)   │ ✅ 광주광역시립남구도서관     │
                │                     │ ❌ 광주광역시립동구도서관     │
                ├─────────────────────┼──────────────────────────────┤
                │ Clean Code         │ ✅ 고운동도서관               │
                │ (ISBN: 97911...)   │ ✅ 나성동도서관               │
                └─────────────────────┴──────────────────────────────┘

                **정렬 기준:**
                - 먼저 도서별로 그룹화
                - 각 도서 아래 대출 가능한 도서관 나열
                - ✅ 표시로 대출 가능 여부 명확히 표시

                **팁:**
                - 표 형식이 깨지지 않도록 주의하세요
                - 도서가 너무 많으면 상위 5~10권만 보여주세요
                - 대출 가능한 도서관이 없으면 "❌ 대출 불가" 표시
                """;

            // qwen2.5로 결과 재구성 (qwenChatClient 사용)
            String response = qwenChatClient.prompt()
                .system(systemPrompt)
                .user(collectedInfo.toString())
                .call()
                .content();

            log.info("[MainAgent] LLM 결과 재구성 완료");
            return response;

        } catch (Exception e) {
            log.error("[MainAgent] LLM 재구성 실패", e);
            // 실패하면 기본 A2A 결과 반환
            return synthesizeBasicResults(agentResults);
        }
    }

    /**
     * 기본 결과 종합 (LLM 실패 시 Fallback)
     */
    private String synthesizeBasicResults(Map<AgentType, String> agentResults) {
        StringBuilder response = new StringBuilder();

        for (Map.Entry<AgentType, String> entry : agentResults.entrySet()) {
            response.append("**").append(entry.getKey()).append(" 결과:**\n\n");
            response.append(entry.getValue());
            response.append("\n\n");
        }

        return response.toString();
    }
}
