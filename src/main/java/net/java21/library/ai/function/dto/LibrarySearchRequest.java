package net.java21.library.ai.function.dto;

/**
 * 도서관 검색 요청 DTO
 */
public record LibrarySearchRequest(
    String libraryName,
    String region  // 지역 코드
) {
    public static LibrarySearchRequest byRegion(String region) {
        return new LibrarySearchRequest(null, region);
    }

    public static LibrarySearchRequest byName(String libraryName) {
        return new LibrarySearchRequest(libraryName, null);
    }
}
