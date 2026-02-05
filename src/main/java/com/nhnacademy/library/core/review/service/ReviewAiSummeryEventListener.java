package com.nhnacademy.library.core.review.service;

import com.nhnacademy.library.core.review.domain.BookReview;
import com.nhnacademy.library.core.review.domain.BookReviewSummary;
import com.nhnacademy.library.core.review.event.ReviewAiSummaryEvent;
import com.nhnacademy.library.core.review.repository.BookReviewRepository;
import com.nhnacademy.library.core.review.repository.BookReviewSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewAiSummeryEventListener {
    private static final int MIN_REVIEW_COUNT_FOR_SUMMARY = 5;

    private final BookReviewSummaryRepository bookReviewSummaryRepository;
    private final BookReviewRepository bookReviewRepository;
    private final ReviewSummarizer reviewSummarizer;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleReviewAiSummeryEvent(ReviewAiSummaryEvent event) {
        // 2. AI 요약 조건 확인 및 생성
        // 조건 1: 충분한 리뷰 수 (N개 이상)
        // 조건 2: 기존 요약 최신성 (is_summary_dirty == TRUE)

        Optional<BookReviewSummary> bookReviewSummaryOptional =  bookReviewSummaryRepository.findById(event.bookId());

        if (bookReviewSummaryOptional.isEmpty()) {
            log.info("No summary found for book {}. Skipping AI summary generation.", event.bookId());
            return;
        }

        BookReviewSummary summary = bookReviewSummaryOptional.get();

        if (summary.getReviewCount() >= MIN_REVIEW_COUNT_FOR_SUMMARY && summary.getIsSummaryDirty()) {
            log.info("Conditions met for book {}. Generating LLM review summary.", event.bookId());

            List<String> reviews = bookReviewRepository.findAllByBookId(event.bookId()).stream()
                    .map(BookReview::getContent)
                    .toList();

            String newSummary = reviewSummarizer.summarizeReviews(reviews);

            summary.updateSummary(newSummary);
            bookReviewSummaryRepository.save(summary);

            log.info("Successfully updated review summary for book {}.", event.bookId());
        } else {
            log.info("Conditions not met for book {}. Review count: {}, is_dirty: {}",
                    event.bookId(), summary.getReviewCount(), summary.getIsSummaryDirty());
        }

    }
}
