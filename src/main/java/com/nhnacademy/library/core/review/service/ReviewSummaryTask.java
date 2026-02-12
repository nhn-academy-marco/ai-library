package com.nhnacademy.library.core.review.service;

import lombok.Value;

/**
 * 리뷰 요약 작업을 나타내는 불변 객체
 */
@Value
public class ReviewSummaryTask {
    Long bookId;
    long enqueueTime;

    public ReviewSummaryTask(Long bookId) {
        this.bookId = bookId;
        this.enqueueTime = System.currentTimeMillis();
    }
}
