package com.nhnacademy.library.external.opennaru.dto;

import lombok.Builder;

import java.util.List;

/**
 * 도서관 통합정보 DTO
 *
 * extends/libSrch API 응답
 */
@Builder
public record LibraryExtendedInfo(
    String libCode,
    String libName,
    String address,
    String tel,
    String fax,
    String latitude,
    String longitude,
    String homepage,
    String closed,
    String operatingTime,
    int bookCount,
    List<HourlyLoanInfo> loanByHours,
    List<WeeklyLoanInfo> loanByDayOfWeek,
    List<NewBookInfo> newBooks
) {
    @Builder
    public record HourlyLoanInfo(
        String hour,
        int loan,
        int returnCount
    ) {}

    @Builder
    public record WeeklyLoanInfo(
        String dayOfWeek,
        int loan,
        int returnCount
    ) {}

    @Builder
    public record NewBookInfo(
        String bookName,
        String authors,
        String publisher,
        String publicationYear,
        String isbn13,
        String imageUrl,
        String regDate
    ) {}
}
