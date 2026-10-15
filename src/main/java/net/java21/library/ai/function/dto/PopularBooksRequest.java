package net.java21.library.ai.function.dto;

import lombok.Builder;
import java.time.LocalDate;

/**
 * 인기대출도서 조회 요청 DTO
 */
@Builder
public record PopularBooksRequest(
    String startDt,  // YYYY-MM-DD
    String endDt,    // YYYY-MM-DD
    String region    // 지역 코드 (선택)
) {
    /**
     * 기본 설정 (최근 30일)
     */
    public static PopularBooksRequest last30Days() {
        LocalDate now = LocalDate.now();
        LocalDate start = now.minusDays(30);
        return new PopularBooksRequest(
            start.toString(),
            now.toString(),
            null
        );
    }

    /**
     * 전국 기준
     */
    public static PopularBooksRequest of(String startDt, String endDt) {
        return new PopularBooksRequest(startDt, endDt, null);
    }
}
