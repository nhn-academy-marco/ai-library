package com.nhnacademy.library.core.review.service;

import com.nhnacademy.library.core.review.event.ReviewAiSummaryEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewAiSummeryEventListener {

    private final ReviewAiSummaryService reviewAiSummaryService;

    @Async
    @EventListener
    public void handleReviewAiSummeryEvent(ReviewAiSummaryEvent event) {
        log.info("Handling ReviewAiSummaryEvent for book id: {}", event.bookId());
        reviewAiSummaryService.generateSummary(event.bookId());
    }
}
