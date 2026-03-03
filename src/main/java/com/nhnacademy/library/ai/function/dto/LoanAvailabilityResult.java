package com.nhnacademy.library.ai.function.dto;

import java.util.List;

/**
 * 대출 가능 도서관 조회 결과 DTO
 */
public record LoanAvailabilityResult(
    String isbn13,
    int libraryCount,
    List<LibraryItem> libraries
) {
    public static LoanAvailabilityResult of(String isbn13, List<LibraryItem> libraries) {
        return new LoanAvailabilityResult(isbn13, libraries.size(), libraries);
    }

    public record LibraryItem(
        String libraryName,
        String address,
        boolean loanAvailable,
        String region
    ) {
        public static LibraryItem of(String libraryName, String address, boolean loanAvailable, String region) {
            return new LibraryItem(libraryName, address, loanAvailable, region);
        }
    }
}
