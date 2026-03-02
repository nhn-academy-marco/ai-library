package com.nhnacademy.library.ai.function.dto;

import java.util.List;

/**
 * AI Function 호출용 리뷰 조회 결과 DTO
 *
 * @param bookId      도서 ID
 * @param summary     리뷰 요약
 * @param rating      평균 평점
 * @param reviewCount 전체 리뷰 개수
 * @param reviews     개별 리뷰 목록 (상위 N개)
 */
public record ReviewResult(
    long bookId,
    String summary,
    double rating,
    int reviewCount,
    List<ReviewItem> reviews
) {
    /**
     * AI Function 호출용 개별 리뷰 항목 DTO
     *
     * @param content 리뷰 내용
     * @param rating  평점
     */
    public record ReviewItem(
        String content,
        int rating
    ) {
        public static ReviewItem of(String content, int rating) {
            return new ReviewItem(content, rating);
        }
    }

    /**
     * 빈 결과를 생성합니다.
     *
     * @param bookId 도서 ID
     * @return ReviewResult
     */
    public static ReviewResult empty(long bookId) {
        return new ReviewResult(bookId, "리뷰가 없습니다.", 0.0, 0, List.of());
    }

    /**
     * 결과를 생성합니다.
     *
     * @param bookId      도서 ID
     * @param summary     리뷰 요약
     * @param rating      평균 평점
     * @param reviewCount 전체 리뷰 개수
     * @param reviews     개별 리뷰 목록
     * @return ReviewResult
     */
    public static ReviewResult of(long bookId, String summary, double rating, int reviewCount, List<ReviewItem> reviews) {
        return new ReviewResult(bookId, summary, rating, reviewCount, reviews);
    }
}
