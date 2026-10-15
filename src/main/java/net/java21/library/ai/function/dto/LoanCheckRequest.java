package net.java21.library.ai.function.dto;

/**
 * AI Function 호출용 대출 가능 여부 조회 요청 DTO
 *
 * @param isbn      ISBN (필수)
 * @param region    지역 코드 (선택, 예: "서울", "경기")
 * @param latitude  위도 (선택, 거리 계산용)
 * @param longitude 경도 (선택, 거리 계산용)
 * @param limit     최대 결과 수 (기본값 10)
 */
public record LoanCheckRequest(
    String isbn,
    String region,
    Double latitude,
    Double longitude,
    int limit
) {
    private static final int DEFAULT_LIMIT = 10;

    public LoanCheckRequest {
        if (limit <= 0) {
            limit = DEFAULT_LIMIT;
        }
    }

    /**
     * ISBN으로 기본 요청을 생성합니다.
     *
     * @param isbn ISBN
     * @return LoanCheckRequest
     */
    public static LoanCheckRequest of(String isbn) {
        return new LoanCheckRequest(isbn, null, null, null, DEFAULT_LIMIT);
    }

    /**
     * ISBN과 지역으로 요청을 생성합니다.
     *
     * @param isbn   ISBN
     * @param region 지역
     * @return LoanCheckRequest
     */
    public static LoanCheckRequest of(String isbn, String region) {
        return new LoanCheckRequest(isbn, region, null, null, DEFAULT_LIMIT);
    }
}
