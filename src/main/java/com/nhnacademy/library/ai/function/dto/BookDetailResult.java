package com.nhnacademy.library.ai.function.dto;

/**
 * 도서 상세 정보 조회 결과 DTO
 */
public record BookDetailResult(
    String isbn13,
    String title,
    String authors,
    String publisher,
    String publicationDate,
    String imageUrl,
    String description,
    String category,
    int loanCount,
    LoanInfo loanInfo
) {
    public record LoanInfo(
        int totalLoanCount,
        int maleLoanCount,
        int femaleLoanCount,
        int teensLoanCount,
        int twentiesLoanCount,
        int thirtiesLoanCount,
        int fortiesLoanCount,
        int fiftiesLoanCount,
        int sixtiesLoanCount
    ) {}
}
