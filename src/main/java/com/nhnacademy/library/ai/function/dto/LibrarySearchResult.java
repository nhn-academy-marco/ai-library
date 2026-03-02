package com.nhnacademy.library.ai.function.dto;

import java.util.List;

/**
 * 도서관 검색 결과 DTO
 */
public record LibrarySearchResult(
    String region,         // 검색된 지역
    int totalCount,        // 전체 도서관 수
    List<LibraryItem> libraries // 도서관 목록
) {
    /**
     * 도서관 항목
     */
    public record LibraryItem(
        String name,     // 도서관 이름
        String address,  // 도서관 주소
        String url       // 도서관 웹사이트 URL
    ) {
        public static LibraryItem of(String name, String address, String url) {
            return new LibraryItem(
                name != null ? name : "",
                address != null ? address : "",
                url != null ? url : ""
            );
        }
    }

    /**
     * 빈 결과 반환
     */
    public static LibrarySearchResult empty() {
        return new LibrarySearchResult("", 0, List.of());
    }

    /**
     * 결과 생성
     */
    public static LibrarySearchResult of(String region, int totalCount, List<LibraryItem> libraries) {
        return new LibrarySearchResult(
            region != null ? region : "",
            totalCount,
            libraries != null ? libraries : List.of()
        );
    }
}
