package com.nhnacademy.library.core.review.service;

import com.nhnacademy.library.core.book.domain.Book;
import com.nhnacademy.library.core.book.repository.BookRepository;
import com.nhnacademy.library.core.book.service.search.BookSearchService;
import com.nhnacademy.library.core.review.domain.BookReview;
import com.nhnacademy.library.core.review.domain.BookReviewSummary;
import com.nhnacademy.library.core.review.dto.ReviewCreateRequest;
import com.nhnacademy.library.core.review.dto.ReviewResponse;
import com.nhnacademy.library.core.review.event.ReviewCreatedEvent;
import com.nhnacademy.library.core.review.repository.BookReviewRepository;
import com.nhnacademy.library.core.review.repository.BookReviewSummaryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private BookReviewRepository bookReviewRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BookSearchService bookSearchService;

    @Mock
    private BookReviewSummaryRepository bookReviewSummaryRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ReviewService reviewService;

    @Test
    @DisplayName("리뷰를 성공적으로 등록하고 이벤트를 발행해야 한다")
    void createReview_Success() {
        // given
        Long bookId = 1L;
        ReviewCreateRequest request = new ReviewCreateRequest("좋은 책입니다.", 5);
        Book book = mock(Book.class);
        // when(book.getId()).thenReturn(bookId); // 불필요한 스터빙 제거
        BookReview savedReview = mock(BookReview.class);
        when(savedReview.getId()).thenReturn(100L);
        
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(bookReviewRepository.save(any(BookReview.class))).thenReturn(savedReview);

        // when
        reviewService.createReview(bookId, request);

        // then
        verify(bookRepository).findById(bookId);
        verify(bookReviewRepository).save(any(BookReview.class));
        verify(eventPublisher).publishEvent(any(ReviewCreatedEvent.class));
        
        // updateReviewSummary는 더 이상 createReview에서 직접 호출되지 않음
        verify(bookReviewSummaryRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("도서의 리뷰 목록을 페이징하여 조회해야 한다")
    void getReviews_Success() {
        // given
        Long bookId = 1L;
        Pageable pageable = PageRequest.of(0, 5);
        Book book = mock(Book.class);
        List<BookReview> reviews = List.of(
            new BookReview(book, "리뷰 1", 5),
            new BookReview(book, "리뷰 2", 4)
        );
        Page<BookReview> reviewPage = new PageImpl<>(reviews, pageable, 2);

        when(bookReviewRepository.findAllByBookIdOrderByCreatedAtDesc(bookId, pageable)).thenReturn(reviewPage);

        // when
        Page<ReviewResponse> result = reviewService.getReviews(bookId, pageable);

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).content()).isEqualTo("리뷰 1");
        assertThat(result.getContent().get(0).rating()).isEqualTo(5);
        verify(bookReviewRepository).findAllByBookIdOrderByCreatedAtDesc(bookId, pageable);
    }
}
