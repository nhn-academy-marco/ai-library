package com.nhnacademy.library.ai.agent.orchestrator;

import com.nhnacademy.library.ai.agent.*;
import com.nhnacademy.library.external.opennaru.client.LibraryInfoNaruApiClient;
import com.nhnacademy.library.external.opennaru.dto.BookExistInfo;
import com.nhnacademy.library.external.opennaru.dto.LibraryInfo;
import com.nhnacademy.library.external.opennaru.util.RegionCodeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
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
 * bookExist API를 활용하여 실제 도서관별 소장 여부를 확인합니다.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiOrchestrationService {

    private final ChatModel chatModel;
    private final BookAgent bookAgent;
    private final LibraryAgent libraryAgent;
    private final LibraryExtendedAgent libraryExtendedAgent;
    private final LibraryInfoNaruApiClient libraryInfoNaruApiClient;

    /**
     * 사용자 요청을 처리하고 AI 응답을 반환합니다.
     * <p>
     * A2A 패턴: Agent 간 협력으로 복잡한 작업 수행
     * </p>
     *
     * @param userMessage 사용자 메시지
     * @return AI 응답
     */
    public String ask(String userMessage) {
        log.info("[MainAgent] 사용자 요청 수신: {}", userMessage);

        try {
            // 1. Agent 선택
            List<AgentType> selectedAgents = selectAgents(userMessage);
            log.info("[MainAgent] 선택된 Agent: {}", selectedAgents);

            if (selectedAgents.isEmpty()) {
                return "질문을 이해하지 못했습니다. 도서 검색, 도서관 조회 등으로 질문해주세요.";
            }

            // 2. Agent 실행 및 결과 수집 (A2A 정보 공유)
            Map<AgentType, String> agentResults = executeAgentsWithA2A(userMessage, selectedAgents);

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
     */
    private List<AgentType> selectAgents(String userMessage) {
        List<AgentType> selectedAgents = new ArrayList<>();
        String message = userMessage.toLowerCase();

        // 도서 검색 키워드
        if (message.contains("책") || message.contains("도서") || message.contains("검색") ||
            message.contains("찾아") || message.contains("isbn")) {
            selectedAgents.add(AgentType.BOOK);
        }

        // 지역명이 포함된 경우 → LibraryAgent + LibraryExtendedAgent
        String region = extractRegionName(userMessage);
        if (region != null) {
            selectedAgents.add(AgentType.LIBRARY);
            selectedAgents.add(AgentType.LIBRARY_EXTENDED);
        }

        return selectedAgents;
    }

    /**
     * A2A 패턴으로 Agent들을 실행하고 결과를 공유합니다.
     * <p>
     * bookExist API 활용을 위해 Agent 간 정보를 전달합니다.
     * </p>
     */
    private Map<AgentType, String> executeAgentsWithA2A(String userMessage, List<AgentType> selectedAgents) {
        Map<AgentType, String> results = new EnumMap<>(AgentType.class);

        for (AgentType agentType : selectedAgents) {
            log.info("[MainAgent] {} Agent 실행 시작...", agentType);

            String query = extractQuery(userMessage, agentType, results);

            // 이전 Agent 결과 전달 (A2A 정보 공유)
            AgentRequest request = AgentRequest.builder()
                .agentType(agentType)
                .userMessage(userMessage)
                .query(query)
                .sharedResults(results)
                .build();

            String result = executeAgent(agentType, request);
            results.put(agentType, result);

            log.info("[MainAgent] {} Agent 실행 완료", agentType);
        }

        return results;
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
     * A2A 정보 공유를 활용하여 이전 Agent 결과를 참고합니다.
     */
    private String extractQuery(String userMessage, AgentType agentType, Map<AgentType, String> previousResults) {
        return switch (agentType) {
            case BOOK -> userMessage; // 원본 메시지
            case LIBRARY -> extractRegionName(userMessage); // 지역명 추출
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
                yield userMessage;
            }
            default -> userMessage;
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

        for (String region : regions) {
            if (message.contains(region)) {
                return region;
            }
        }
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
     * 결과를 종합합니다.
     * <p>
     * bookExist API 결과를 포함하여 최종 응답을 생성합니다.
     * </p>
     */
    private String synthesizeResults(String userMessage, Map<AgentType, String> agentResults) {
        // BOOK + LIBRARY + LIBRARY_EXTENDED 조합
        if (agentResults.containsKey(AgentType.BOOK) &&
            agentResults.containsKey(AgentType.LIBRARY) &&
            agentResults.containsKey(AgentType.LIBRARY_EXTENDED)) {

            return synthesizeBookWithLibrariesWithBookExist(userMessage, agentResults);
        }

        // BOOK + LIBRARY 조합
        if (agentResults.containsKey(AgentType.BOOK) &&
            agentResults.containsKey(AgentType.LIBRARY)) {
            return synthesizeBookWithLibraries(agentResults);
        }

        // 단일 Agent 결과
        return agentResults.values().iterator().next();
    }

    /**
     * 도서 + 도서관 + bookExist 종합 응답 (A2A 완성체)
     */
    private String synthesizeBookWithLibrariesWithBookExist(String userMessage, Map<AgentType, String> agentResults) {
        StringBuilder response = new StringBuilder();

        // 1. 도서 검색 결과
        String bookResult = agentResults.get(AgentType.BOOK);
        response.append(bookResult);
        response.append("\n\n");

        // 2. 도서관 리스트
        String libResult = agentResults.get(AgentType.LIBRARY);
        response.append(libResult);
        response.append("\n\n");

        // 3. bookExist로 실제 소장 여부 확인
        String isbn = extractIsbnFromBookResult(bookResult);
        String region = extractRegionName(userMessage);

        if (isbn != null && region != null) {
            response.append("📚 **실제 소장 도서관 확인 (bookExist API)**\n\n");

            // 지역 코드 변환
            String regionCode = RegionCodeMapper.convert(region);

            // 도서관 리스트 직접 조회
            List<LibraryInfo> libraries = libraryInfoNaruApiClient.searchLibrariesByRegion(regionCode);

            // 상위 5개 도서관만 확인 (성능 고려)
            int checkCount = Math.min(5, libraries.size());
            response.append(String.format("(상위 %d개 도서관만 확인)\n\n", checkCount));

            for (int i = 0; i < checkCount; i++) {
                LibraryInfo library = libraries.get(i);
                BookExistInfo existInfo = libraryExtendedAgent.checkBookExists(library.libCode(), isbn);

                if (existInfo != null) {
                    response.append(String.format("- **%s** (libCode: %s)\n",
                        library.libName(), library.libCode()));
                    response.append(String.format("  소장 여부: %s\n",
                        existInfo.exists() ? "✅ 소장 중" : "❌ 미소장"));

                    if (existInfo.exists()) {
                        if ("Y".equals(existInfo.loanAvailable())) {
                            response.append("  대출 가능: ✅\n");
                        }
                        if (existInfo.callNumber() != null && !existInfo.callNumber().isBlank()) {
                            response.append(String.format("  청구기호: %s\n", existInfo.callNumber()));
                        }
                    }

                    response.append("\n");
                }
            }
        }

        return response.toString();
    }

    /**
     * 도서 + 도서관 종합 응답
     */
    private String synthesizeBookWithLibraries(Map<AgentType, String> agentResults) {
        StringBuilder response = new StringBuilder();

        // 도서 검색 결과
        response.append(agentResults.get(AgentType.BOOK));
        response.append("\n\n");

        // 도서관 리스트
        response.append(agentResults.get(AgentType.LIBRARY));

        return response.toString();
    }

    /**
     * LibraryAgent 결과에서 LibraryInfo 목록 추출 (도우미 메서드)
     */
    private List<LibraryInfo> getLibrariesFromResult(String libraryResult) {
        // 간단 구현: 필요시 LibraryInfoNaruApiClient 직접 호출
        return Collections.emptyList();
    }
}
