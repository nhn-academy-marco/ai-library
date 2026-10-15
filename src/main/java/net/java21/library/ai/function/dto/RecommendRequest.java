package net.java21.library.ai.function.dto;

import lombok.Builder;

/**
 * 추천도서 조회 요청 DTO
 */
@Builder
public record RecommendRequest(
    String isbn13,
    String type  // "mania" or "reader"
) {
    public static RecommendRequest forMania(String isbn13) {
        return new RecommendRequest(isbn13, "mania");
    }

    public static RecommendRequest forReader(String isbn13) {
        return new RecommendRequest(isbn13, "reader");
    }
}
