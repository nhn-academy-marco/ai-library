package com.nhnacademy.library.ai.function.dto;

/**
 * 도서관 검색 요청 DTO
 */
public record LibrarySearchRequest(
    String region,     // 지역명 (예: 강남구, 서초구)
    Double latitude,  // 위도 (선택 사항)
    Double longitude, // 경도 (선택 사항)
    int limit         // 최대 결과 수
) {
    private static final int DEFAULT_LIMIT = 10;

    /**
     * 지역명으로 검색
     */
    public static LibrarySearchRequest byRegion(String region) {
        return new LibrarySearchRequest(region, null, null, DEFAULT_LIMIT);
    }

    /**
     * 지역명과 제한으로 검색
     */
    public static LibrarySearchRequest byRegion(String region, int limit) {
        return new LibrarySearchRequest(region, null, null, limit);
    }

    /**
     * 위도/경도로 근처 도서관 검색
     */
    public static LibrarySearchRequest byLocation(Double latitude, Double longitude) {
        return new LibrarySearchRequest(null, latitude, longitude, DEFAULT_LIMIT);
    }
}
