package com.nhnacademy.library.core.review.event;

import java.time.OffsetDateTime;

/**
 * 리뷰가 등록되었을 때 발생하는 이벤트
 *
 * @param bookId   도서 ID
 * @param reviewId 리뷰 ID
 * @param createdAt 등록 일시
 */
public record ReviewCreatedEvent(
    Long bookId,
    Long reviewId,
    Integer rating,
    OffsetDateTime createdAt
) {
}
