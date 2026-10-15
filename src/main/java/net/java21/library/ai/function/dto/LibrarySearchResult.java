package net.java21.library.ai.function.dto;

import java.util.List;

/**
 * 도서관 검색 결과 DTO
 */
public record LibrarySearchResult(
    int count,
    List<LibraryItem> libraries
) {
    /**
     * 도서관 항목
     */
    public record LibraryItem(
        String libCode,
        String libName,
        String address,
        String tel,
        String homepage
    ) {
        public static LibraryItem of(String libCode, String libName, String address, String tel, String homepage) {
            return new LibraryItem(libCode, libName, address, tel, homepage);
        }
    }
}
