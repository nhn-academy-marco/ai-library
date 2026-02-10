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
    }
}
