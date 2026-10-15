package net.java21.library.ai.function.dto;

import lombok.Builder;
import java.util.List;

/**
 * 대출 급상승 도서 조회 결과 DTO
 */
@Builder
public record HotTrendResult(
    String searchDate,
    int count,
    List<BookItem> books
) {
    @Builder
    public record BookItem(
        String isbn13,
        String title,
        String authors,
        String publisher,
        int currentRank,
        int rankChange
    ) {}
}
