package com.nhnacademy.library.ai.function.dto;

import lombok.Builder;

/**
 * 대출 가능 도서관 조회 요청 DTO
 */
@Builder
public record LoanAvailabilityRequest(
    String isbn13,
    String region  // 지역 코드 (예: 11=서울, 29=광주)
) {
    public static LoanAvailabilityRequest of(String isbn13) {
        return new LoanAvailabilityRequest(isbn13, null);
    }
}
