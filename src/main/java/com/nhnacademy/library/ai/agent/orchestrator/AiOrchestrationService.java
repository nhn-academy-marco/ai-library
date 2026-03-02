package com.nhnacademy.library.ai.agent.orchestrator;

import com.nhnacademy.library.ai.agent.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * AI Orchestration Service (MainAgent)
 * <p>
 * 여러 전문 Agent를 조합하여 복잡한 사용자 요청을 처리하는 오케스트레이터입니다.
 * 사용자의 의도를 분석하고 적절한 Agent를 선택하여 실행한 뒤,
 * 결과를 종합하여 최종 응답을 생성합니다.
 * </p>
 * <p>
 * A2A (Agent-to-Agent) 패턴의 핵심 컴포넌트입니다.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiOrchestrationService {

    private final ChatModel chatModel;
    private final BookAgent bookAgent;
    private final ReviewAgent reviewAgent;
    private final LoanAgent loanAgent;
    private final LibraryAgent libraryAgent;
    private final BookDetailAgent bookDetailAgent;
    private final RecommendationAgent recommendationAgent;
    private final TrendAgent trendAgent;
    private final LibraryExtendedAgent libraryExtendedAgent;

    /**
     * 사용자 요청을 처리하고 AI 응답을 반환합니다.
     * <p>
     * MainAgent(Orchestrator)로서 다음 작업을 수행합니다:
     * 1. 사용자 의도 분석
     * 2. 필요한 Agent 선택
     * 3. Agent 실행 및 결과 수집
     * 4. 결과 종합 및 최종 응답 생성
     * </p>
     *
     * @param userMessage 사용자 메시지
     * @return AI 응답
     */
    public String ask(String userMessage) {
        log.info("[MainAgent] 사용자 요청 수신: {}", userMessage);

        try {
            // 1. Agent 선택 (의도 분석)
            List<AgentType> selectedAgents = selectAgents(userMessage);
            log.info("[MainAgent] 선택된 Agent: {}", selectedAgents);

            if (selectedAgents.isEmpty()) {
                // Agent가 필요 없는 일반 대화
                return handleGeneralChat(userMessage);
            }

            // 2. Agent 실행 및 결과 수집
            Map<AgentType, String> agentResults = executeAgents(userMessage, selectedAgents);

            // 3. 결과 종합
            String finalResponse = synthesizeResults(userMessage, agentResults);

            log.info("[MainAgent] 응답 생성 완료");
            return finalResponse;

        } catch (Exception e) {
            log.error("[MainAgent] 요청 처리 실패: userMessage={}", userMessage, e);
            return "요청 처리 중 오류가 발생했습니다. 다시 시도해주세요.";
        }
    }

    /**
     * 사용자 요청을 분석하여 필요한 Agent를 선택합니다.
     *
     * @param userMessage 사용자 메시지
     * @return 선택된 Agent 목록
     */
    private List<AgentType> selectAgents(String userMessage) {
        List<AgentType> selectedAgents = new ArrayList<>();

        // 키워드 기반 Agent 선택 (도서관정보나루 API 문서 용어 기반)
        String message = userMessage.toLowerCase();

        // 대출 가능 키워드 (libSrchByBook API 관련)
        // "소장", "소장하고", "소장한" → 대출 가능 확인 의도
        if (message.contains("대출") || message.contains("빌리") ||
            message.contains("빌려") || message.contains("대여") ||
            message.contains("가능") || message.contains("loan") ||
            message.contains("소장") || message.contains("장서")) {
            selectedAgents.add(AgentType.LOAN);
        }

        // 리뷰 키워드
        if (message.contains("리뷰") || message.contains("review") ||
            message.contains("평점") || message.contains("별점")) {
            selectedAgents.add(AgentType.REVIEW);
        }

        // 도서관 검색 키워드 (libSrch API 관련)
        if (message.contains("도서관") || message.contains("library") ||
            message.contains("근처") || message.contains("주변") ||
            message.contains("libname") || containsRegion(message)) {
            selectedAgents.add(AgentType.LIBRARY);
        }

        // 기본: 도서 검색 (srchBooks API 관련)
        // "리스트", "목록" 요청은 제외 (도서관 목록만 원하는 경우)
        boolean isListRequest = message.contains("리스트") || message.contains("목록") ||
                               message.contains("list") || message.contains("목록");

        if (selectedAgents.isEmpty() ||
            message.contains("책") || message.contains("도서") ||
            message.contains("book") || message.contains("검색") ||
            message.contains("찾") || message.contains("keyword") ||
            message.contains("isbn") || message.contains("저자") ||
            message.contains("출판사")) {
            // 단, "도서관 리스트/목록" 요청이고 다른 Agent가 있으면 BOOK 제외
            if (!isListRequest || selectedAgents.isEmpty()) {
                selectedAgents.add(0, AgentType.BOOK); // 항상 먼저 실행
            }
        }

        // 도서 상세 정보 키워드 (srchDtlList, keywordList, usageAnalysisList API 관련)
        if (message.contains("상세") || message.contains("detail") ||
            message.contains("정보") || message.contains("키워드") ||
            message.contains("keyword") || message.contains("서지") ||
            message.contains("이용") || message.contains("분석") ||
            message.contains("사용")) {
            selectedAgents.add(AgentType.BOOK_DETAIL);
        }

        // 추천 키워드 (recommandList, loanItemSrch API 관련)
        // 단, "도서관 추천"은 LIBRARY + LOAN로 처리
        boolean hasLibraryContext = message.contains("도서관") || message.contains("library") || containsRegion(message);
        if ((message.contains("추천") || message.contains("recommend") ||
             message.contains("인기") || message.contains("best") ||
             message.contains("마니아") || message.contains("다독") ||
             message.contains("bestseller")) && !hasLibraryContext) {
            selectedAgents.add(AgentType.RECOMMENDATION);
        }

        // 트렌드 키워드 (hotTrend, monthlyKeywords, readQt API 관련)
        if (message.contains("급상승") || message.contains("hot") ||
            message.contains("핫") || message.contains("트렌드") ||
            message.contains("trend") || message.contains("월간") ||
            message.contains("month") || message.contains("독서량") ||
            message.contains("독서율") || message.contains("reading")) {
            selectedAgents.add(AgentType.TREND);
        }

        // 도서관 통합정보 키워드 (extends/loanItemSrchByLib, usageTrend, bookExist API 관련)
        if (message.contains("연령") || message.contains("age") ||
            message.contains("소장여부") || message.contains("exist") ||
            message.contains("추이") || message.contains("통합")) {
            selectedAgents.add(AgentType.LIBRARY_EXTENDED);
        }

        return selectedAgents;
    }

    /**
     * 선택된 Agent들을 실행하고 결과를 수집합니다.
     * <p>
     * 순차적으로 실행하며, 이전 Agent의 결과를 다음 Agent에 전달하여 정보 공유를 지원합니다.
     * 예: BookAgent가 찾은 ISBN을 LoanAgent가 활용
     * </p>
     *
     * @param userMessage      사용자 메시지
     * @param selectedAgents 선택된 Agent 목록
     * @return Agent별 실행 결과
     */
    private Map<AgentType, String> executeAgents(String userMessage, List<AgentType> selectedAgents) {
        Map<AgentType, String> results = new EnumMap<>(AgentType.class);

        for (AgentType agentType : selectedAgents) {
            try {
                // 현재까지 수집된 결과를 다음 Agent에 전달 (Agent 간 정보 공유)
                String agentResult = executeAgent(userMessage, agentType, results);
                results.put(agentType, agentResult);
            } catch (Exception e) {
                log.error("[MainAgent] Agent 실행 실패: agentType={}", agentType, e);
                results.put(agentType, String.format("[%s 실행 실패: %s]",
                    agentType.getDisplayName(), e.getMessage()));
            }
        }

        return results;
    }

    /**
     * 개별 Agent를 실행합니다.
     *
     * @param userMessage 사용자 메시지
     * @param agentType  Agent 타입
     * @param previousResults 이전 Agent들의 실행 결과 (정보 공유용)
     * @return Agent 실행 결과
     */
    private String executeAgent(String userMessage, AgentType agentType, Map<AgentType, String> previousResults) {
        log.info("[MainAgent] Agent 실행 시작: agentType={}, sharedResults={}", agentType, previousResults.size());

        Agent agent = getAgent(agentType);
        AgentRequest request = createAgentRequest(userMessage, agentType, previousResults);
        return agent.execute(request);
    }

    /**
     * Agent 타입에 해당하는 Agent 인스턴스를 반환합니다.
     *
     * @param agentType Agent 타입
     * @return Agent 인스턴스
     */
    private Agent getAgent(AgentType agentType) {
        return switch (agentType) {
            case BOOK -> bookAgent;
            case REVIEW -> reviewAgent;
            case LOAN -> loanAgent;
            case LIBRARY -> libraryAgent;
            case BOOK_DETAIL -> bookDetailAgent;
            case RECOMMENDATION -> recommendationAgent;
            case TREND -> trendAgent;
            case LIBRARY_EXTENDED -> libraryExtendedAgent;
            default -> throw new IllegalArgumentException("Unknown agent type: " + agentType);
        };
    }

    /**
     * Agent 요청을 생성합니다.
     *
     * @param userMessage 사용자 메시지
     * @param agentType  Agent 타입
     * @param previousResults 이전 Agent들의 실행 결과 (정보 공유용)
     * @return Agent 요청
     */
    private AgentRequest createAgentRequest(String userMessage, AgentType agentType, Map<AgentType, String> previousResults) {
        // TODO: LLM을 사용하여 검색어/파라미터 추출 (현재는 단순 구현)
        String query = extractQuery(userMessage, agentType, previousResults);
        return AgentRequest.withSharedResults(agentType, userMessage, query, previousResults);
    }

    /**
     * 사용자 메시지에서 검색어를 추출합니다.
     * <p>
     * 다른 Agent의 실행 결과가 있으면 그 정보를 활용합니다.
     * 예: BookAgent가 찾은 ISBN을 LoanAgent가 활용
     * </p>
     *
     * @param userMessage 사용자 메시지
     * @param agentType  Agent 타입
     * @param previousResults 이전 Agent들의 실행 결과
     * @return 추출된 검색어
     */
    private String extractQuery(String userMessage, AgentType agentType, Map<AgentType, String> previousResults) {
        // 간단 구현: 도서명, 지역명 등을 추출
        // TODO: LLM 기반 지능형 추출로 개선

        switch (agentType) {
            case BOOK:
                // 책 관련 키워드 제거
                return userMessage.replaceAll("(책|검색|찾아줘|推荐)", "").trim();
            case LIBRARY:
                // 지역명 추출 (강남구, 서초구 등)
                return extractRegionName(userMessage);
            case LOAN:
                // BookAgent 결과에서 ISBN 추출 시도
                if (previousResults.containsKey(AgentType.BOOK)) {
                    String bookAgentResult = previousResults.get(AgentType.BOOK);
                    String isbn = extractIsbnFromResult(bookAgentResult);
                    if (isbn != null && !isbn.isBlank()) {
                        log.info("[MainAgent] BookAgent 결과에서 ISBN 추출 성공: {}", isbn);
                        return isbn;
                    }
                }
                // ISBN이 없으면 책명 추출
                return extractBookNameOrIsbn(userMessage);
            case REVIEW:
                // BookAgent 결과에서 ISBN 추출 시도
                if (previousResults.containsKey(AgentType.BOOK)) {
                    String bookAgentResult = previousResults.get(AgentType.BOOK);
                    String isbn = extractIsbnFromResult(bookAgentResult);
                    if (isbn != null && !isbn.isBlank()) {
                        log.info("[MainAgent] BookAgent 결과에서 ISBN 추출 성공: {}", isbn);
                        return isbn;
                    }
                }
                // ISBN이 없으면 책명 추출
                return extractBookName(userMessage);
            case BOOK_DETAIL:
                // ISBN 추출 (우선 순위)
                if (userMessage.matches(".*\\d{10,13}.*")) {
                    return userMessage.replaceAll(".*\\b(\\d{10,13})\\b.*", "$1");
                }
                return userMessage.replaceAll("(상세|정보|키워드|keyword)", "").trim();
            case RECOMMENDATION:
            case TREND:
            case LIBRARY_EXTENDED:
                // 전체 메시지 전달 (Agent 내에서 분석)
                return userMessage;
            default:
                return userMessage;
        }
    }

    /**
     * 메시지에서 지역명을 추출합니다.
     */
    private String extractRegionName(String message) {
        // 대한민국 주요 지역명 매칭
        String[] regions = {
            "강남구", "서초구", "송파구", "강동구", "강서구",
            "마포구", "양천구", "은평구", "서대문구", "종로구",
            "용산구", "성동구", "광진구", "동대문구", "중구",
            "중랑구", "성북구", "강북구", "도봉구", "노원구",
            "강남구", "관악구"
        };

        for (String region : regions) {
            if (message.contains(region)) {
                return region;
            }
        }

        return message; // 지역명을 찾지 못하면 원본 반환
    }

    /**
     * 메시지에서 책명이나 ISBN을 추출합니다.
     */
    private String extractBookNameOrIsbn(String message) {
        // ISBN 패턴이 있는지 확인
        if (message.matches(".*\\d{10,13}.*")) {
            return message.replaceAll(".*\\b(\\d{10,13})\\b.*", "$1");
        }
        // 책명 추출 (따옴표 안의 내용)
        return extractBookName(message);
    }

    /**
     * 메시지에서 책명을 추출합니다.
     */
    private String extractBookName(String message) {
        // 따옴표로 감싸진 책명 추출
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("[\"']([^\"']+)[\"']");
        java.util.regex.Matcher matcher = pattern.matcher(message);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return message;
    }

    /**
     * 지역명 포함 여부 확인
     */
    private boolean containsRegion(String message) {
        String[] regions = {"구", "시", "군"};
        for (String region : regions) {
            if (message.contains(region)) {
                return true;
            }
        }
        return false;
    }

    /**
     * BookAgent 실행 결과에서 ISBN을 추출합니다.
     * <p>
     * BookAgent 결과 형식: "도서 제목 (ISBN: 1234567890123)"
     * </p>
     *
     * @param bookAgentResult BookAgent 실행 결과
     * @return 추출된 ISBN (없으면 null)
     */
    private String extractIsbnFromResult(String bookAgentResult) {
        if (bookAgentResult == null || bookAgentResult.isBlank()) {
            return null;
        }

        // ISBN 패턴: 10자리 또는 13자리 숫자
        // "ISBN: 1234567890" 또는 "ISBN:1234567890" 형식 검색
        java.util.regex.Pattern isbnPattern = java.util.regex.Pattern.compile("ISBN\\s*:?\s*(\\d{10,13})");
        java.util.regex.Matcher matcher = isbnPattern.matcher(bookAgentResult);

        if (matcher.find()) {
            return matcher.group(1);
        }

        // ISBN 형식이 없으면 일반 10~13자리 숫자 패턴 검색
        java.util.regex.Pattern numberPattern = java.util.regex.Pattern.compile("\\b(\\d{10,13})\\b");
        java.util.regex.Matcher numberMatcher = numberPattern.matcher(bookAgentResult);

        if (numberMatcher.find()) {
            return numberMatcher.group(1);
        }

        return null;
    }

    /**
     * Agent 실행 결과들을 종합하여 최종 응답을 생성합니다.
     *
     * @param userMessage   사용자 메시지
     * @param agentResults  Agent별 실행 결과
     * @return 종합된 최종 응답
     */
    private String synthesizeResults(String userMessage, Map<AgentType, String> agentResults) {
        log.info("[MainAgent] 결과 종합 시작: results={}", agentResults.size());

        // 단일 Agent 결과는 LLM 없이 직접 반환 (성능 최적화)
        if (agentResults.size() == 1) {
            Map.Entry<AgentType, String> entry = agentResults.entrySet().iterator().next();
            log.info("[MainAgent] 단일 Agent 결과이므로 직접 반환: agentType={}", entry.getKey());
            return entry.getValue();
        }

        // 복수 Agent 결과는 LLM로 종합 (결과 길이 제한)
        try {
            // 전체 결과 길이 확인 - 너무 길면 fallback 사용
            int totalLength = agentResults.values().stream()
                .mapToInt(String::length)
                .sum();

            // 전체 결과가 2000자를 넘으면 직접 합치기 사용 (LLM 타임아웃 방지)
            if (totalLength > 2000) {
                log.warn("[MainAgent] 결과가 너무 김({}자), fallback 사용", totalLength);
                return fallbackSynthesize(agentResults);
            }

            // 각 Agent 결과를 500자로 제한
            Map<AgentType, String> truncatedResults = new EnumMap<>(AgentType.class);
            agentResults.forEach((agentType, result) -> {
                String truncated = result.length() > 500 ?
                    result.substring(0, 500) + "\n...(중략)" : result;
                truncatedResults.put(agentType, truncated);
            });

            ChatClient chatClient = ChatClient.builder(chatModel).build();

            String systemPrompt = """
                당신은 도서관 도우미 AI 오케스트레이터입니다.
                여러 전문 Agent들의 실행 결과를 종합하여 사용자에게 친절한 답변을 제공하세요.

                **중요: 반드시 한국어로 답변하세요. 중국어나 다른 언어를 사용하지 마세요.**

                **종합 방법:**
                1. 각 Agent의 결과를 확인
                2. 중요한 정보를 추출
                3. 자연스러운 한국어로 정리
                4. 사용자에게 도움이 되는 방향으로 답변

                **예시 응답:**
                "광주 지역 도서관에서 토비의 스프링을 찾았습니다! 📚

                **도서 검색 결과:**
                - 토비의 스프링 (ISBN: 9788960777331)
                - 토비의 스프링 3.1 (ISBN: 9788996673142)

                **광주 도서관 (28개):**
                - 계림꿈나무도서관
                - 광산구립신가도서관
                ..."
                """;

            // Agent 결과를 프롬프트에 포함
            StringBuilder agentResultsText = new StringBuilder("**Agent 실행 결과:**\n\n");
            truncatedResults.forEach((agentType, result) -> {
                agentResultsText.append(String.format("[%s 결과]\n%s\n\n",
                    agentType.getDisplayName(), result));
            });

            String fullPrompt = String.format("""
                %s

                **사용자 요청:**
                %s

                %s
                """, systemPrompt, userMessage, agentResultsText);

            log.info("[MainAgent] LLM 호출 시작: 결과 길이={}", totalLength);
            String response = chatClient.prompt()
                .user(fullPrompt)
                .call()
                .content();

            log.info("[MainAgent] 결과 종합 완료");
            return response;

        } catch (Exception e) {
            log.error("[MainAgent] LLM 종합 실패, 직접 합치 시도", e);
            // LLM 실패 시 결과를 직접 합쳐서 반환
            return fallbackSynthesize(agentResults);
        }
    }

    /**
     * LLM 실패 시 대안으로 결과를 직접 합칩니다.
     */
    private String fallbackSynthesize(Map<AgentType, String> agentResults) {
        StringBuilder response = new StringBuilder();
        response.append("📚 도서관 검색 결과\n\n");

        agentResults.forEach((agentType, result) -> {
            // 각 결과를 800자로 제한 (텔레그램 메시지 제한 방지)
            String truncated = result.length() > 800 ?
                result.substring(0, 800) + "\n...(중략)" : result;

            response.append(String.format("**%s**\n%s\n\n",
                agentType.getDisplayName(), truncated));
        });

        return response.toString();
    }

    /**
     * Agent가 필요 없는 일반 대화 처리
     *
     * @param userMessage 사용자 메시지
     * @return 일반 대화 응답
     */
    private String handleGeneralChat(String userMessage) {
        ChatClient chatClient = ChatClient.builder(chatModel).build();

        return chatClient.prompt()
            .system("당신은 도서관 도우미 AI입니다. 반드시 한국어로만 답변하세요. 중국어나 다른 언어를 사용하지 마세요.")
            .user(userMessage)
            .call()
            .content();
    }
}
