package com.nhnacademy.library.ai.function.dto;

import lombok.Builder;
import java.time.LocalDate;

/**
 * 대출 급상승 도서 조회 요청 DTO
 */
@Builder
public record HotTrendRequest(
    String searchDate  // YYYY-MM-DD
) {
    /**
     * 오늘 기준
     */
    public static HotTrendRequest today() {
        return new HotTrendRequest(LocalDate.now().toString());
    }
}
