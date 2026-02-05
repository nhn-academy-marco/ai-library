package com.nhnacademy.library.core.review.service;

import com.nhnacademy.library.core.book.domain.Book;
import com.nhnacademy.library.core.book.exception.BookNotFoundException;
import com.nhnacademy.library.core.book.repository.BookRepository;
import com.nhnacademy.library.core.review.domain.BookReview;
import com.nhnacademy.library.core.review.domain.BookReviewSummary;
import com.nhnacademy.library.core.review.dto.BookReviewSummaryStatDto;
import com.nhnacademy.library.core.review.event.ReviewCreatedEvent;
import com.nhnacademy.library.core.review.repository.BookReviewRepository;
import com.nhnacademy.library.core.review.repository.BookReviewSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewEventListener {

    private static final int MIN_REVIEW_COUNT_FOR_SUMMARY = 5;

    private final BookReviewSummaryRepository bookReviewSummaryRepository;
    private final BookReviewRepository bookReviewRepository;
    private final ReviewSummarizer reviewSummarizer;
    private final BookRepository bookRepository;
    private final ReviewService reviewService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleReviewCreatedEvent(ReviewCreatedEvent event) {

        log.info("Handling ReviewCreatedEvent for book id: {}", event.bookId());
        reviewService.updateReviewSummary(event.bookId());

//        try {
//            // 1. 리뷰 통계 업데이트 (ReviewService의 updateReviewSummary 로직을 이관)
//            BookReviewSummary summary = bookReviewSummaryRepository.findById(event.bookId())
//                    .orElseGet(() -> {
//                        Book book = bookRepository.findById(event.bookId())
//                                .orElseThrow(() -> new BookNotFoundException(event.bookId()));
//                        return new BookReviewSummary(book);
//                    });
//
//            summary.addReview(event.rating());
//            bookReviewSummaryRepository.saveAndFlush(summary);
//            log.info("Updated review statistics for book {}.", event.bookId());
//
//            // 2. AI 요약 조건 확인 및 생성
//            // 조건 1: 충분한 리뷰 수 (N개 이상)
//            // 조건 2: 기존 요약 최신성 (is_summary_dirty == TRUE)
//            if (summary.getReviewCount() >= MIN_REVIEW_COUNT_FOR_SUMMARY && summary.getIsSummaryDirty()) {
//                log.info("Conditions met for book {}. Generating LLM review summary.", event.bookId());
//
//                List<String> reviews = bookReviewRepository.findAllByBookId(event.bookId()).stream()
//                        .map(BookReview::getContent)
//                        .toList();
//
//                String newSummary = reviewSummarizer.summarizeReviews(reviews);
//
//                summary.updateSummary(newSummary);
//                bookReviewSummaryRepository.save(summary);
//
//                log.info("Successfully updated review summary for book {}.", event.bookId());
//            } else {
//                log.info("Conditions not met for book {}. Review count: {}, is_dirty: {}",
//                        event.bookId(), summary.getReviewCount(), summary.getIsSummaryDirty());
//            }
//        } catch (Exception e) {
//            log.error("Failed to process ReviewCreatedEvent for book {}: {}", event.bookId(), e.getMessage());
//        }
    }
}
