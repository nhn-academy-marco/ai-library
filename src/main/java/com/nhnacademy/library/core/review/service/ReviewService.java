package com.nhnacademy.library.core.review.service;

import com.nhnacademy.library.core.book.domain.Book;
import com.nhnacademy.library.core.book.exception.BookNotFoundException;
import com.nhnacademy.library.core.book.repository.BookRepository;
import com.nhnacademy.library.core.book.service.search.BookSearchService;
import com.nhnacademy.library.core.review.domain.BookReview;
import com.nhnacademy.library.core.review.domain.BookReviewSummary;
import com.nhnacademy.library.core.review.dto.ReviewCreateRequest;
import com.nhnacademy.library.core.review.dto.ReviewResponse;
import com.nhnacademy.library.core.review.event.ReviewCreatedEvent;
import com.nhnacademy.library.core.review.repository.BookReviewRepository;
import com.nhnacademy.library.core.review.repository.BookReviewSummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final BookReviewRepository bookReviewRepository;
    private final BookRepository bookRepository;
    private final BookSearchService bookSearchService;
    private final BookReviewSummaryRepository bookReviewSummaryRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Long createReview(Long bookId, ReviewCreateRequest request) {
        Optional<Book> bookOptional = bookRepository.findById(bookId);
        if (bookOptional.isEmpty()) {
            throw new BookNotFoundException(bookId);
        }
        Book book = bookOptional.get();

        BookReview review = new BookReview(book, request.content(), request.rating());
        BookReview savedReview = bookReviewRepository.save(review);

        Long reviewId = savedReview.getId();
        
        // 리뷰 등록 이벤트 발행
        eventPublisher.publishEvent(new ReviewCreatedEvent(bookId, reviewId, request.rating(), savedReview.getCreatedAt()));
        
        return reviewId;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateReviewSummary(Long bookId, int rating) {
        // 이 로직은 이제 ReviewEventListener에서 처리됩니다.
        // 호환성을 위해 메서드 자체는 유지할 수 있으나, 더 이상 createReview에서 직접 호출하지 않습니다.
        try {
            Optional<BookReviewSummary> summaryOptional = bookReviewSummaryRepository.findById(bookId);
            BookReviewSummary summary;
            
            if (summaryOptional.isPresent()) {
                summary = summaryOptional.get();
            } else {
                Optional<Book> bookOptional = bookRepository.findById(bookId);
                if (bookOptional.isPresent()) {
                    summary = new BookReviewSummary(bookOptional.get());
                } else {
                    throw new BookNotFoundException(bookId);
                }
            }
            
            summary.addReview(rating);
            bookReviewSummaryRepository.saveAndFlush(summary);

        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(ReviewService.class)
                    .warn("Failed to update review summary for book {}: {}", bookId, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviews(Long bookId, Pageable pageable) {
        return bookReviewRepository.findAllByBookIdOrderByCreatedAtDesc(bookId, pageable)
                .map(ReviewResponse::from);
    }
}
