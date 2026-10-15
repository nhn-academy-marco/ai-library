package net.java21.library.ai.agent;

/**
 * Agent 타입 enum
 * <p>
 * 각 Agent의 전문 분야를 정의합니다.
 * 도서관정보나루 API 기반 동작을 설명합니다.
 * </p>
 */
public enum AgentType {

    /**
     * 도서 검색 Agent
     */
    BOOK("도서 검색", "책 제목, 저자, 출판사로 도서를 검색합니다. 내부 DB와 도서관정보나루 API를 모두 활용합니다."),

    /**
     * 리뷰 조회 Agent
     */
    REVIEW("리뷰 조회", "도서의 리뷰, 평점, 요약 정보를 조회합니다."),

    /**
     * 대출 가능 확인 Agent
     */
    LOAN("대출 가능 확인", "ISBN으로 전국 도서관의 대출 가능 여부를 조회합니다. libSrchByBook API를 활용합니다."),

    /**
     * 도서관 검색 Agent
     */
    LIBRARY("도서관 검색", "지역별 도서관 정보를 검색합니다. libSrch API를 활용합니다."),

    /**
     * 도서 상세 정보 Agent
     */
    BOOK_DETAIL("도서 상세 정보", "도서의 상세 정보, 키워드, 이용 분석 정보를 조회합니다. srchDtlList, keywordList, usageAnalysisList API를 활용합니다."),

    /**
     * 추천 Agent
     */
    RECOMMENDATION("추천", "도서 추천, 인기 대출도서, 마니아/다독자 추천 정보를 조회합니다. recommandList, loanItemSrch API를 활용합니다."),

    /**
     * 트렌드 Agent
     */
    TREND("트렌드", "대출 급상승 도서, 월간 키워드, 독서량/독서율 트렌드를 조회합니다. hotTrend, monthlyKeywords, readQt API를 활용합니다."),

    /**
     * 도서관 통합정보 Agent
     */
    LIBRARY_EXTENDED("도서관 통합정보", "도서관별 연령대별 인기도서, 대출반납추이, 소장 여부를 조회합니다. extends/loanItemSrchByLib, usageTrend, bookExist API를 활용합니다."),

    /**
     * 메인 오케스트레이터 Agent
     */
    ORCHESTRATOR("오케스트레이터", "여러 Agent를 조합하여 복잡한 요청을 처리합니다.");

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
