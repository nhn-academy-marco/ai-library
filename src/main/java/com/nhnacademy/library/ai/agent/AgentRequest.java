package com.nhnacademy.library.ai.agent;

import lombok.Builder;

/**
 * Agent 요청 DTO
 * <p>
 * MainAgent(Orchestrator)가 각 Agent에게 보내는 요청입니다.
 * </p>
 */
@Builder
public record AgentRequest(
    /**
     * Agent 타입 (요청할 Agent)
     */
    AgentType agentType,

    /**
     * 사용자 원본 메시지
     */
    String userMessage,

    /**
     * 추출된 검색어 또는 파라미터
     */
    String query,

    /**
     * 추가 파라미터 (선택 사항)
     */
    Object params
) {
    /**
     * 기본 생성자 (params 없음)
     */
    public static AgentRequest of(AgentType agentType, String userMessage, String query) {
        return new AgentRequest(agentType, userMessage, query, null);
    }

    /**
     * 파라미터 포함 생성자
     */
    public static AgentRequest withParams(AgentType agentType, String userMessage, String query, Object params) {
        return new AgentRequest(agentType, userMessage, query, params);
    }

    /**
     * 파라미터를 반환합니다.
     */
    public Object params() {
        return params;
    }
}
