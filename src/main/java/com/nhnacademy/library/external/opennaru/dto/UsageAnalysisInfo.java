package com.nhnacademy.library.external.opennaru.dto;

import lombok.Builder;

/**
 * 도서 이용 분석 정보 DTO
 *
 * usageAnalysisList API 응답
 */
@Builder
public record UsageAnalysisInfo(
    String isbn13,             // ISBN13
    String bookName,           // 도서명
    int totalLoanCount,        // 전체 대출 횟수
    GenderLoanInfo genderLoanInfo,
    AgeLoanInfo ageLoanInfo
) {
    @Builder
    public record GenderLoanInfo(
        int maleCount,         // 남성 대출 횟수
        int femaleCount        // 여성 대출 횟수
    ) {}

    @Builder
    public record AgeLoanInfo(
        int teensCount,        // 10대 대출 횟수
        int twentiesCount,     // 20대 대출 횟수
        int thirtiesCount,     // 30대 대출 횟수
        int fortiesCount,      // 40대 대출 횟수
        int fiftiesCount,      // 50대 대출 횟수
        int sixtiesCount       // 60대 이상 대출 횟수
    ) {}
}
