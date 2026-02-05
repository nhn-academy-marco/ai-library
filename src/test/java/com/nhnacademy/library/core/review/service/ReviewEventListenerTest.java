package com.nhnacademy.library.core.review.service;

import com.nhnacademy.library.core.book.domain.Book;
import com.nhnacademy.library.core.book.repository.BookRepository;
import com.nhnacademy.library.core.review.domain.BookReview;
import com.nhnacademy.library.core.review.domain.BookReviewSummary;
import com.nhnacademy.library.core.review.event.ReviewCreatedEvent;
import com.nhnacademy.library.core.review.repository.BookReviewRepository;
import com.nhnacademy.library.core.review.repository.BookReviewSummaryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewEventListenerTest {

    @Mock
    private BookReviewSummaryRepository bookReviewSummaryRepository;

    @Mock
    private BookReviewRepository bookReviewRepository;

    @Mock
    private ReviewSummarizer reviewSummarizer;

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private ReviewEventListener reviewEventListener;

    @Test
    @DisplayName("리뷰 요약 정보가 없으면 새로 생성하여 통계를 업데이트해야 한다")
    void handleReviewCreatedEvent_CreateNewSummary() {
        // given
        Long bookId = 1L;
        ReviewCreatedEvent event = new ReviewCreatedEvent(bookId, 100L, 5, OffsetDateTime.now());
        Book book = mock(Book.class);
        
        when(bookReviewSummaryRepository.findById(bookId)).thenReturn(Optional.empty());
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));

        // when
        reviewEventListener.handleReviewCreatedEvent(event);

        // then
        verify(bookReviewSummaryRepository).saveAndFlush(any(BookReviewSummary.class));
        verify(reviewSummarizer, never()).summarizeReviews(any());
    }

    @Test
    @DisplayName("리뷰 수가 5개 이상이고 Dirty 상태이면 통계를 업데이트하고 요약을 생성해야 한다")
    void handleReviewCreatedEvent_GenerateSummary() {
        // given
        Long bookId = 1L;
        ReviewCreatedEvent event = new ReviewCreatedEvent(bookId, 100L, 5, OffsetDateTime.now());
        BookReviewSummary summary = mock(BookReviewSummary.class);
        
        when(bookReviewSummaryRepository.findById(bookId)).thenReturn(Optional.of(summary));
        when(summary.getReviewCount()).thenReturn(5);
        when(summary.getIsSummaryDirty()).thenReturn(true);
        
        List<BookReview> reviews = List.of(mock(BookReview.class));
        when(bookReviewRepository.findAllByBookId(bookId)).thenReturn(reviews);
        when(reviewSummarizer.summarizeReviews(any())).thenReturn("New Summary");

        // when
        reviewEventListener.handleReviewCreatedEvent(event);

        // then
        verify(summary).addReview(5);
        verify(bookReviewSummaryRepository).saveAndFlush(summary);
        verify(reviewSummarizer).summarizeReviews(any());
        verify(summary).updateSummary("New Summary");
        verify(bookReviewSummaryRepository).save(summary);
    }
}
