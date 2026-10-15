package net.java21.library.front.api.dto;

import net.java21.library.core.book.dto.BookAiRecommendationResponse;

import java.util.List;

/**
 * AI 추천 준비 상태 응답
 *
 * @param status          {@value #PENDING}(생성 중) | {@value #READY}(준비됨, 빈 목록이면 추천할 도서 없음) | {@value #UNAVAILABLE}(조회 불가)
 * @param recommendations 준비된 AI 추천 (준비 전에는 빈 목록)
 */
public record AiRecommendationStatusResponse(String status, List<BookAiRecommendationResponse> recommendations) {

    public static final String PENDING = "pending";
    public static final String READY = "ready";
    public static final String UNAVAILABLE = "unavailable";

    public static AiRecommendationStatusResponse pending() {
        return new AiRecommendationStatusResponse(PENDING, List.of());
    }

    public static AiRecommendationStatusResponse ready(List<BookAiRecommendationResponse> recommendations) {
        return new AiRecommendationStatusResponse(READY, recommendations);
    }

    public static AiRecommendationStatusResponse unavailable() {
        return new AiRecommendationStatusResponse(UNAVAILABLE, List.of());
    }
}
