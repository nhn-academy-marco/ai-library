package net.java21.library.ai.agent;

import lombok.Builder;

/**
 * Agent 응답 DTO
 * <p>
 * 각 Agent가 MainAgent(Orchestrator)에게 반환하는 응답입니다.
 * </p>
 */
@Builder
public record AgentResponse(
    /**
     * Agent 타입
     */
    AgentType agentType,

    /**
     * 응답 성공 여부
     */
    boolean success,

    /**
     * 응답 메시지 (자연어 형식)
     */
    String message,

    /**
     * 원본 데이터 (선택 사항)
     */
    Object data,

    /**
     * 에러 메시지 (실패 시)
     */
    String errorMessage
) {
    /**
     * 성공 응답 생성
     */
    public static AgentResponse success(AgentType agentType, String message) {
        return new AgentResponse(agentType, true, message, null, null);
    }

    /**
     * 데이터 포함 성공 응답 생성
     */
    public static AgentResponse successWithData(AgentType agentType, String message, Object data) {
        return new AgentResponse(agentType, true, message, data, null);
    }

    /**
     * 실패 응답 생성
     */
    public static AgentResponse failure(AgentType agentType, String errorMessage) {
        return new AgentResponse(agentType, false, null, null, errorMessage);
    }
}
