package com.nhnacademy.library.core.review.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 리뷰 요약 작업 큐를 관리하는 서비스
 */
@Slf4j
@Service
public class ReviewSummaryQueueService {

    private final BlockingQueue<ReviewSummaryTask> queue = new LinkedBlockingQueue<>();

    // 같은 책에 대한 중복 요청 방지 (bookId -> enqueueTime)
    private final Map<Long, Long> pendingBooks = new ConcurrentHashMap<>();

    private static final long DEDUP_WINDOW_MS = 5000; // 5초 내 중복 요청 무시

    /**
     * 큐에 작업 추가 (중복 요청 필터링)
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

        boolean offered = queue.offer(new ReviewSummaryTask(bookId));
        if (offered) {
            pendingBooks.put(bookId, now);
            log.debug("Enqueued summary task for book {}. Queue size: {}", bookId, queue.size());
        }
        return offered;
    }

    /**
     * 큐에서 작업을 꺼냄 (대기 가능)
     *
     * @param timeout 대기 시간 (millisecond)
     * @return 작업, 없으면 null
     */
    public ReviewSummaryTask dequeue(long timeout) throws InterruptedException {
        ReviewSummaryTask task = queue.poll(timeout, TimeUnit.MILLISECONDS);
        if (task != null) {
            pendingBooks.remove(task.getBookId());
        }
        return task;
    }

    /**
     * 큐 크기 반환
     */
    public int getQueueSize() {
        return queue.size();
    }

    /**
     * 특정 도서가 큐에 대기 중인지 확인
     */
    public boolean isPending(Long bookId) {
        return pendingBooks.containsKey(bookId);
    }
}
