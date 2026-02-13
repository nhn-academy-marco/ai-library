package com.nhnacademy.library.core.review.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 리뷰 요약 큐를 처리하는 RabbitMQ Consumer
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewSummaryQueueProcessor {

    private final ReviewSummaryQueueService queueService;
    private final ReviewAiSummaryService reviewAiSummaryService;

    /**
     * RabbitMQ 메시지를 수신하여 처리
     * concurrency: 3-5 (최소 3개, 최대 5개의 동시 Consumer 스레드)
     */
    @RabbitListener(
            queues = "${rabbitmq.queue.review-summary}",
            concurrency = "3-5"
    )
    public void processTask(ReviewSummaryTask task) {
        long startTime = System.currentTimeMillis();
        log.info("Processing summary task for book {} (queue wait: {}ms)",
                task.getBookId(), startTime - task.getEnqueueTime());

        try {
            reviewAiSummaryService.generateSummary(task.getBookId());
            long duration = System.currentTimeMillis() - startTime;
            log.info("Completed summary task for book {} in {}ms",
                    task.getBookId(), duration);
        } catch (Exception e) {
            log.error("Failed to process summary task for book {}: {}",
                    task.getBookId(), e.getMessage(), e);
            throw e; // RabbitMQ 재시도 또는 DLQ로 이동
        } finally {
            // 중복 추적 Map에서 제거
            queueService.removePending(task.getBookId());
        }
    }
}
