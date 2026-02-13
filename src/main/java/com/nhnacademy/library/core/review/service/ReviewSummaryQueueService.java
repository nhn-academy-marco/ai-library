package com.nhnacademy.library.core.review.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 리뷰 요약 작업 큐를 관리하는 서비스 (RabbitMQ 기반)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewSummaryQueueService {

    private final RabbitTemplate rabbitTemplate;

    private static final String EXCHANGE = "nhnacademy-library-exchange";
    private static final String ROUTING_KEY = "review.summary";

    // 같은 책에 대한 중복 요청 방지 (bookId -> enqueueTime)
    private final Map<Long, Long> pendingBooks = new ConcurrentHashMap<>();

    private static final long DEDUP_WINDOW_MS = 5000; // 5초 내 중복 요청 무시

    @Value("${rabbitmq.queue.review-summary}")
    private String queueName;

    /**
     * RabbitMQ에 작업 발행 (중복 요청 필터링)
     *
     * @param bookId 도서 ID
     * @return 큐에 실제로 추가되었으면 true, 중복으로 무시되면 false
     */
    public boolean enqueue(Long bookId) {
        Long lastEnqueueTime = pendingBooks.get(bookId);
        long now = System.currentTimeMillis();

        // 중복 요청 체크 (5초 내 같은 책의 요청은 무시)
        if (lastEnqueueTime != null && (now - lastEnqueueTime) < DEDUP_WINDOW_MS) {
            log.debug("Duplicate summary request for book {} within dedup window. Ignoring.", bookId);
            return false;
        }

        try {
            ReviewSummaryTask task = new ReviewSummaryTask(bookId);
            rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, task);
            pendingBooks.put(bookId, now);
            log.debug("Published summary task for book {} to RabbitMQ queue: {}", bookId, queueName);
            return true;
        } catch (Exception e) {
            log.error("Failed to publish summary task for book {}: {}", bookId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 특정 도서가 큐에 대기 중인지 확인
     */
    public boolean isPending(Long bookId) {
        return pendingBooks.containsKey(bookId);
    }

    /**
     * 중복 요청 추적 Map에서 제외 (소비자가 호출)
     */
    public void removePending(Long bookId) {
        pendingBooks.remove(bookId);
    }
}
