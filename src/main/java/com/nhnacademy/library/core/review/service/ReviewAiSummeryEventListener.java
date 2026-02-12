package com.nhnacademy.library.core.review.service;

import com.nhnacademy.library.core.review.event.ReviewAiSummaryEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 리뷰 AI 요약 이벤트를 수신하여 큐에 작업을 추가하는 리스너
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewAiSummeryEventListener {

    private final ReviewSummaryQueueService queueService;

    @Async
    @EventListener
    public void handleReviewAiSummeryEvent(ReviewAiSummaryEvent event) {
        log.info("Received ReviewAiSummaryEvent for book id: {}", event.bookId());
        boolean enqueued = queueService.enqueue(event.bookId());
        if (enqueued) {
            log.info("Enqueued AI summary task for book {}", event.bookId());
        } else {
            log.debug("Summary task for book {} already queued (dedup)", event.bookId());
        }
    }
}
