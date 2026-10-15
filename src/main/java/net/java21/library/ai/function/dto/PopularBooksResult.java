package net.java21.library.ai.function.dto;

import lombok.Builder;
import java.util.List;

/**
 * 인기대출도서 조회 결과 DTO
 */
@Builder
public record PopularBooksResult(
    String period,
    int count,
    List<BookItem> books
) {
    @Builder
    public record BookItem(
        String isbn13,
        String title,
        String authors,
        String publisher,
        int loanCount,
        String ranking
    ) {}
}
