package net.java21.library.ai.agent;

/**
 * AI Agent 인터페이스
 * <p>
 * 모든 Agent가 구현해야 할 기본 인터페이스입니다.
 * 각 Agent는 자신만의 ChatClient와 전문 도메인 로직을 가집니다.
 * </p>
 */
public interface Agent {

    /**
     * Agent 타입을 반환합니다.
     *
     * @return Agent 타입
     */
    AgentType getType();

    /**
     * Agent 이름을 반환합니다.
     *
     * @return Agent 이름
     */
    String getName();

    /**
     * Agent 설명을 반환합니다.
     * 이 설명은 MainAgent(Orchestrator)가 어떤 Agent를 호출할지 결정할 때 사용됩니다.
     *
     * @return Agent 설명
     */
    String getDescription();

    /**
     * Agent를 실행하여 응답을 생성합니다.
     *
     * @param request Agent 요청
     * @return Agent 응답
     */
    String execute(AgentRequest request);
}
