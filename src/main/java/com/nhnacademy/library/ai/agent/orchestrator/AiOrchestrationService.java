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

        // 키워드 기반 Agent 선택 (간단 규칙 기반)
        String message = userMessage.toLowerCase();

        // 대출 가능 키워드 (확장: "빌리", "대여" 포함)
        if (message.contains("대출") || message.contains("빌리") ||
            message.contains("빌려") || message.contains("대여") ||
            message.contains("가능") || message.contains("loan")) {
            selectedAgents.add(AgentType.LOAN);
        }

        // 리뷰 키워드
        if (message.contains("리뷰") || message.contains("review") ||
            message.contains("평점") || message.contains("별점")) {
            selectedAgents.add(AgentType.REVIEW);
        }

        // 도서관 검색 키워드
        if (message.contains("도서관") || message.contains("library") ||
            message.contains("근처") || message.contains("주변") ||
            containsRegion(message)) {
            selectedAgents.add(AgentType.LIBRARY);
        }

        // 기본: 도서 검색 (확장: "도서", "book" 포함)
        if (selectedAgents.isEmpty() ||
            message.contains("책") || message.contains("도서") ||
            message.contains("book") || message.contains("검색") ||
            message.contains("찾")) {
            selectedAgents.add(0, AgentType.BOOK); // 항상 먼저 실행
        }

        // 도서 상세 정보 키워드
        if (message.contains("상세") || message.contains("detail") ||
            message.contains("정보") || message.contains("키워드") ||
            message.contains("keyword")) {
            selectedAgents.add(AgentType.BOOK_DETAIL);
        }

        // 추천 키워드 (단, "도서관 추천" 제외 - LIBRARY + LOAN로 처리)
        boolean hasLibraryContext = message.contains("도서관") || message.contains("library") || containsRegion(message);
        if ((message.contains("추천") || message.contains("recommend") ||
             message.contains("인기") || message.contains("best") ||
             message.contains("마니아") || message.contains("다독")) && !hasLibraryContext) {
            selectedAgents.add(AgentType.RECOMMENDATION);
        }

        // 트렌드 키워드
        if (message.contains("급상승") || message.contains("hot") ||
            message.contains("트렌드") || message.contains("trend") ||
            message.contains("월간") || message.contains("독서량") ||
            message.contains("독서율")) {
            selectedAgents.add(AgentType.TREND);
        }

        // 도서관 통합정보 키워드
        if (message.contains("연령") || message.contains("age") ||
            message.contains("소장") || message.contains("exist")) {
            selectedAgents.add(AgentType.LIBRARY_EXTENDED);
        }

        return selectedAgents;
    }

    /**
     * 선택된 Agent들을 실행하고 결과를 수집합니다.
     *
     * @param userMessage      사용자 메시지
     * @param selectedAgents 선택된 Agent 목록
     * @return Agent별 실행 결과
     */
    private Map<AgentType, String> executeAgents(String userMessage, List<AgentType> selectedAgents) {
        Map<AgentType, String> results = new EnumMap<>(AgentType.class);

        for (AgentType agentType : selectedAgents) {
            try {
                String agentResult = executeAgent(userMessage, agentType);
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
     * @return Agent 실행 결과
     */
    private String executeAgent(String userMessage, AgentType agentType) {
        log.info("[MainAgent] Agent 실행 시작: agentType={}", agentType);

        Agent agent = getAgent(agentType);
        AgentRequest request = createAgentRequest(userMessage, agentType);
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
     * @return Agent 요청
     */
    private AgentRequest createAgentRequest(String userMessage, AgentType agentType) {
        // TODO: LLM을 사용하여 검색어/파라미터 추출 (현재는 단순 구현)
        String query = extractQuery(userMessage, agentType);
        return AgentRequest.of(agentType, userMessage, query);
    }

    /**
     * 사용자 메시지에서 검색어를 추출합니다.
     *
     * @param userMessage 사용자 메시지
     * @param agentType  Agent 타입
     * @return 추출된 검색어
     */
    private String extractQuery(String userMessage, AgentType agentType) {
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
                // ISBN이나 책명 추출
                return extractBookNameOrIsbn(userMessage);
            case REVIEW:
                // 책명 추출
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
     * Agent 실행 결과들을 종합하여 최종 응답을 생성합니다.
     *
     * @param userMessage   사용자 메시지
     * @param agentResults  Agent별 실행 결과
     * @return 종합된 최종 응답
     */
    private String synthesizeResults(String userMessage, Map<AgentType, String> agentResults) {
        log.info("[MainAgent] 결과 종합 시작: results={}", agentResults.size());

        // LLM을 사용하여 결과 종합
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
            "강남구 근처 도서관에서 빌릴 수 있는 자바 책 3권을 찾았습니다! 📚

            **강남구 도서관:**
            - 강남도서관: 2권 대출 가능
            - 역삼도서관: 1권 대출 가능

            **찾은 도서:**
            1. 토비의 스프링 (강남도서관 대출 가능)
            2. 자바의 정석 (역삼도서관 대출 가능)
            ..."
            """;

        // Agent 결과를 프롬프트에 포함
        StringBuilder agentResultsText = new StringBuilder("**Agent 실행 결과:**\n\n");
        agentResults.forEach((agentType, result) -> {
            agentResultsText.append(String.format("[%s 결과]\n%s\n\n",
                agentType.getDisplayName(), result));
        });

        String fullPrompt = String.format("""
            %s

            **사용자 요청:**
            %s

            %s
            """, systemPrompt, userMessage, agentResultsText);

        String response = chatClient.prompt()
            .user(fullPrompt)
            .call()
            .content();

        log.info("[MainAgent] 결과 종합 완료");
        return response;
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
