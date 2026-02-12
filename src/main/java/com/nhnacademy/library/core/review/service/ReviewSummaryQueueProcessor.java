package com.nhnacademy.library.core.review.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 리뷰 요약 큐를 처리하는 워커
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewSummaryQueueProcessor {

    private final ReviewSummaryQueueService queueService;
    private final ReviewAiSummaryService reviewAiSummaryService;

    private static final long POLL_TIMEOUT_MS = 1000; // 1초
    private volatile boolean running = true;

    /**
     * 애플리케이션 시작 시 큐 처리 워커 스레드 시작
     */
    @EventListener(ApplicationReadyEvent.class)
    @Async("summaryQueueExecutor")
    public void start() {
        log.info("Review summary queue processor started");
        processQueue();
    }

    /**
     * 큐에서 작업을 가져와 처리
     */
    private void processQueue() {
        while (running) {
            try {
                ReviewSummaryTask task = queueService.dequeue(POLL_TIMEOUT_MS);
                if (task != null) {
                    processTask(task);
                }
            } catch (InterruptedException e) {
                log.warn("Queue processor interrupted: {}", e.getMessage());
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Error processing queue: {}", e.getMessage(), e);
                // 계속 진행 (단일 작업 실패가 전체 처리를 멈추지 않도록)
            }
        }
        log.info("Review summary queue processor stopped");
    }

    /**
     * 개별 작업 처리
     */
    private void processTask(ReviewSummaryTask task) {
        long startTime = System.currentTimeMillis();
        log.info("Processing summary task for book {} (queue wait: {}ms)",
                task.getBookId(), startTime - task.getEnqueueTime());

        try {
            reviewAiSummaryService.generateSummary(task.getBookId());
            long duration = System.currentTimeMillis() - startTime;
            log.info("Completed summary task for book {} in {}ms. Remaining queue size: {}",
                    task.getBookId(), duration, queueService.getQueueSize());
        } catch (Exception e) {
            log.error("Failed to process summary task for book {}: {}",
                    task.getBookId(), e.getMessage(), e);
        }
    }

    /**
     * 워커 중지 (테스트 또는 종료 시 사용)
     */
    public void stop() {
        running = false;
    }
}
