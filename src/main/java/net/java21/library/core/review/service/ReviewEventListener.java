package net.java21.library.core.review.service;

import net.java21.library.core.review.event.ReviewCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewEventListener {

    /** 통계 갱신 중 낙관적 락 충돌 시 최대 시도 횟수 */
    private static final int MAX_STAT_UPDATE_ATTEMPTS = 3;

    private final ReviewService reviewService;

    /**
     * 리뷰 등록이 커밋된 뒤 통계를 갱신합니다.
     *
     * <p>{@code @Async}로 리뷰 등록 응답과 분리합니다. 통계는 매번 리뷰 테이블에서 다시 집계하므로,
     * 다른 트랜잭션(예: AI 요약 반영)과 낙관적 락이 충돌하면 새 트랜잭션으로 다시 시도해도 결과가 같습니다.</p>
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleReviewCreatedEvent(ReviewCreatedEvent event) {

        log.info("Handling ReviewCreatedEvent for book id: {}", event.bookId());
        for (int attempt = 1; attempt <= MAX_STAT_UPDATE_ATTEMPTS; attempt++) {
            try {
                reviewService.updateReviewSummary(event.bookId());
                return;
            } catch (ObjectOptimisticLockingFailureException e) {
                log.warn("Review stat update conflicted for book {} (attempt {}/{}). Retrying.",
                        event.bookId(), attempt, MAX_STAT_UPDATE_ATTEMPTS);
            }
        }
        log.error("Failed to update review stat for book {} after {} attempts.", event.bookId(), MAX_STAT_UPDATE_ATTEMPTS);
    }
}
