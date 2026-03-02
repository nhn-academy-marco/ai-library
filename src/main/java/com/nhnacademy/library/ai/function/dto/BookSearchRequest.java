package com.nhnacademy.library.ai.function.dto;

/**
 * AI Function 호출용 도서 검색 요청 DTO
 *
 * @param query 검색어 (제목, 저자명 등)
 * @param limit 최대 결과 수 (기본값 10)
 */
public record BookSearchRequest(
    String query,
    int limit
) {
    private static final int DEFAULT_LIMIT = 10;

    public BookSearchRequest(String query, int limit) {
        this.query = query;
        this.limit = limit > 0 ? limit : DEFAULT_LIMIT;
    }

    /**
     * 기본 limit로 요청을 생성합니다.
     *
     * @param query 검색어
     * @return BookSearchRequest
     */
    public static BookSearchRequest of(String query) {
        return new BookSearchRequest(query, DEFAULT_LIMIT);
    }
}
