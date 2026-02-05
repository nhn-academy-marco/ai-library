package com.nhnacademy.library.core.review.event;

/**
 * 리뷰가 등록되었을 때 발생하는 이벤트
 *
 * @param bookId   도서 ID
 */
public record ReviewCreatedEvent(
    Long bookId
) {
}
