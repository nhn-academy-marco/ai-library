package com.nhnacademy.library.core.review.service;

import com.nhnacademy.library.core.review.event.ReviewCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewEventListener {

    private final ReviewService reviewService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleReviewCreatedEvent(ReviewCreatedEvent event) {

        log.info("Handling ReviewCreatedEvent for book id: {}", event.bookId());
        reviewService.updateReviewSummary(event.bookId());
    }
}
