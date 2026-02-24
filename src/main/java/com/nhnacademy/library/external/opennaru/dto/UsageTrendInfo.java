package com.nhnacademy.library.external.opennaru.dto;

import lombok.Builder;

/**
 * 대출반납추이 정보 DTO
 *
 * usageTrend API 응답
 */
@Builder
public record UsageTrendInfo(
    String libCode,            // 도서관 코드
    String libName,            // 도서관명
    String type,               // 타입 (D=요일별, T=시간대별)
    LoanReturnTrend loanTrend  // 대출반납 추이
) {
    @Builder
    public record LoanReturnTrend(
        int mondayLoan,        // 월요일 대출
        int tuesdayLoan,       // 화요일 대출
        int wednesdayLoan,     // 수요일 대출
        int thursdayLoan,      // 목요일 대출
        int fridayLoan,        // 금요일 대출
        int saturdayLoan,      // 토요일 대출
        int sundayLoan,        // 일요일 대출
        int hourlyLoanAvg     // 시간대별 평균 (type=T일 때)
    ) {}
}
