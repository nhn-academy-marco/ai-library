package com.nhnacademy.library.ai.agent;

/**
 * Agent 타입 enum
 * <p>
 * 각 Agent의 전문 분야를 정의합니다.
 * </p>
 */
public enum AgentType {

    /**
     * 도서 검색 Agent
     */
    BOOK("도서 검색", "책 제목, 저자, 출판사로 도서를 검색합니다"),

    /**
     * 리뷰 조회 Agent
     */
    REVIEW("리뷰 조회", "도서의 리뷰, 평점, 요약 정보를 조회합니다"),

    /**
     * 대출 가능 확인 Agent
     */
    LOAN("대출 가능 확인", "도서의 대출 가능 여부와 도서관 정보를 조회합니다"),

    /**
     * 도서관 검색 Agent
     */
    LIBRARY("도서관 검색", "지역별 도서관 정보를 검색합니다"),

    /**
     * 메인 오케스트레이터 Agent
     */
    ORCHESTRATOR("오케스트레이터", "여러 Agent를 조합하여 복잡한 요청을 처리합니다");

    private final String displayName;
    private final String description;

    AgentType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
