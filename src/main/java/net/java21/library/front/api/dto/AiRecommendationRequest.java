package net.java21.library.front.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * AI 추천 준비 상태 조회 요청
 *
 * @param keyword RAG 검색어 (검색 화면과 같은 값)
 */
public record AiRecommendationRequest(
        @NotBlank
        @Size(max = 100)
        String keyword
) {
}
