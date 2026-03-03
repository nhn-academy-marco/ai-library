package com.nhnacademy.library.ai.function.dto;

import lombok.Builder;
import java.util.List;

/**
 * 추천도서 조회 결과 DTO
 */
@Builder
public record RecommendResult(
    String isbn13,
    String type,
    int count,
    List<BookItem> books
) {
    @Builder
    public record BookItem(
        String isbn13,
        String title,
        String authors,
        String publisher,
        int rank
    ) {}
}
