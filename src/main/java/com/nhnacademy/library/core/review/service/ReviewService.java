package com.nhnacademy.library.core.review.service;

import com.nhnacademy.library.core.book.domain.Book;
import com.nhnacademy.library.core.book.exception.BookNotFoundException;
import com.nhnacademy.library.core.book.repository.BookRepository;
import com.nhnacademy.library.core.book.service.search.BookSearchService;
import com.nhnacademy.library.core.review.domain.BookReview;
import com.nhnacademy.library.core.review.domain.BookReviewSummary;
import com.nhnacademy.library.core.review.dto.BookReviewSummaryStatDto;
import com.nhnacademy.library.core.review.dto.ReviewCreateRequest;
import com.nhnacademy.library.core.review.dto.ReviewResponse;
import com.nhnacademy.library.core.review.event.ReviewAiSummaryEvent;
import com.nhnacademy.library.core.review.event.ReviewCreatedEvent;
import com.nhnacademy.library.core.review.repository.BookReviewRepository;
import com.nhnacademy.library.core.review.repository.BookReviewSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Slf4j
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
        eventPublisher.publishEvent(new ReviewCreatedEvent(bookId));
        
        return reviewId;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateReviewSummary(Long bookId) {
        // 이 로직은 이제 ReviewEventListener에서 처리됩니다.
        // 호환성을 위해 메서드 자체는 유지할 수 있으나, 더 이상 createReview에서 직접 호출하지 않습니다.

        Optional<BookReviewSummaryStatDto> bookReviewSummaryStatDtoOptional = bookReviewSummaryRepository.selectStat(bookId);
        BookReviewSummaryStatDto BookReviewSummaryStatDto = null;

        if(bookReviewSummaryStatDtoOptional.isPresent()) {
            BookReviewSummaryStatDto = bookReviewSummaryStatDtoOptional.get();
            log.info("bookReviewSummaryStatDto:{}", BookReviewSummaryStatDto);
        }

        Optional<BookReviewSummary> summaryOptional = bookReviewSummaryRepository.findById(bookId);
        BookReviewSummary summary = null;

        if(summaryOptional.isPresent()){
            summary = summaryOptional.get();

            summary.updateStat(
                    BookReviewSummaryStatDto.getReviewCount(),
                    BigDecimal.valueOf(BookReviewSummaryStatDto.getAverageRating()),
                    BookReviewSummaryStatDto.getRating1Count(),
                    BookReviewSummaryStatDto.getRating2Count(),
                    BookReviewSummaryStatDto.getRating3Count(),
                    BookReviewSummaryStatDto.getRating4Count(),
                    BookReviewSummaryStatDto.getRating5Count(),
                    BookReviewSummaryStatDto.getLastReviewedAt().toLocalDateTime()
            );

        }else{
            summary = new BookReviewSummary(
                    bookId,
                    BookReviewSummaryStatDto.getReviewCount(),
                    BigDecimal.valueOf(BookReviewSummaryStatDto.getAverageRating()),
                    BookReviewSummaryStatDto.getRating1Count(),
                    BookReviewSummaryStatDto.getRating2Count(),
                    BookReviewSummaryStatDto.getRating3Count(),
                    BookReviewSummaryStatDto.getRating4Count(),
                    BookReviewSummaryStatDto.getRating5Count(),
                    BookReviewSummaryStatDto.getLastReviewedAt().toLocalDateTime()
            );
        }

        log.info("update : summary:{}",summary);

        bookReviewSummaryRepository.save(summary);

        eventPublisher.publishEvent(new ReviewAiSummaryEvent(bookId));

    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviews(Long bookId, Pageable pageable) {
        return bookReviewRepository.findAllByBookIdOrderByCreatedAtDesc(bookId, pageable)
                .map(ReviewResponse::from);
    }
}
