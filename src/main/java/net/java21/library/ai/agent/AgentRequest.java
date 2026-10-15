package net.java21.library.ai.agent;

import lombok.Builder;

import java.util.Map;

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
    Object params,

    /**
     * 다른 Agent들의 실행 결과 (Agent 간 정보 공유용)
     * <p>
     * Key: AgentType, Value: 해당 Agent의 실행 결과
     * </p>
     * <p>
     * 예: BookAgent가 찾은 ISBN을 LoanAgent가 활용 가능
     * </p>
     */
    Map<AgentType, String> sharedResults
) {
    /**
     * 기본 생성자 (params, sharedResults 없음)
     */
    public static AgentRequest of(AgentType agentType, String userMessage, String query) {
        return new AgentRequest(agentType, userMessage, query, null, null);
    }

    /**
     * 파라미터 포함 생성자
     */
    public static AgentRequest withParams(AgentType agentType, String userMessage, String query, Object params) {
        return new AgentRequest(agentType, userMessage, query, params, null);
    }

    /**
     * sharedResults 포함 생성자 (Agent 간 정보 공유용)
     */
    public static AgentRequest withSharedResults(AgentType agentType, String userMessage, String query,
                                                   Map<AgentType, String> sharedResults) {
        return new AgentRequest(agentType, userMessage, query, null, sharedResults);
    }

    /**
     * params와 sharedResults 포함 생성자 (Agent 간 정보 공유 + 추가 파라미터)
     */
    public static AgentRequest withParamsAndSharedResults(AgentType agentType, String userMessage, String query,
                                                           Object params, Map<AgentType, String> sharedResults) {
        return new AgentRequest(agentType, userMessage, query, params, sharedResults);
    }

    /**
     * 파라미터를 반환합니다.
     */
    public Object params() {
        return params;
    }
}
