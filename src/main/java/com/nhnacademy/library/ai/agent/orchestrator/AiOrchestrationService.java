package com.nhnacademy.library.ai.agent.orchestrator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * AI Orchestration Service (MainAgent)
 * <p>
 * Step 7: LLM 기반 Function Calling 오케스트레이션
 * </p>
 * <p>
 * LLM이 스스로 필요한 Function을 선택하고 호출하여 복잡한 사용자 요청을 처리합니다.
 * 수동 Agent 선택 및 조합 로직을 제거하고 LLM에게 모든 오케스트레이션을 맡깁니다.
 * </p>
 * <p>
 * A2A (Agent-to-Agent) 패턴의 핵심 컴포넌트입니다.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiOrchestrationService {

    private final ChatClient chatClient;

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
            String systemPrompt = """
                당신은 도서관 도우미 AI입니다.

                **중요:**
                - 반드시 한국어로 답변하세요.
                - 중국어나 다른 언어를 사용하지 마세요.
                - 사용자의 질문에 친절하고 명확하게 답변하세요.

                **사용 가능한 기능:**
                - 도서 검색 (제목, 저자명)
                - 대출 가능 도서관 조회 (ISBN, 지역)
                - 도서관 검색 (지역, 도서관명)
                - 도서 상세 정보 조회
                - 인기대출도서 조회
                - 대출 급상승 도서 조회
                - 추천도서 조회

                **지역 코드:**
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
