package com.nhnacademy.library.core.review.service;

import com.nhnacademy.library.core.review.event.ReviewCreatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewEventListenerTest {

    @Mock
    private ReviewService reviewService;

    @InjectMocks
    private ReviewEventListener reviewEventListener;

    @Test
    @DisplayName("ReviewCreatedEvent를 처리하면 ReviewService의 updateReviewSummary를 호출해야 한다")
    void handleReviewCreatedEvent_ShouldCallReviewService() {
        // given
        Long bookId = 1L;
        ReviewCreatedEvent event = new ReviewCreatedEvent(bookId);

        // when
        reviewEventListener.handleReviewCreatedEvent(event);

        // then
        verify(reviewService).updateReviewSummary(bookId);
    }

    @Test
    @DisplayName("ReviewCreatedEvent를 처리할 때 전달된 bookId로 ReviewService를 호출해야 한다")
    void handleReviewCreatedEvent_WithSpecificBookId() {
        // given
        Long bookId = 1L;
        ReviewCreatedEvent event = new ReviewCreatedEvent(bookId);

        // when
        reviewEventListener.handleReviewCreatedEvent(event);

        // then
        verify(reviewService).updateReviewSummary(bookId);
        verifyNoMoreInteractions(reviewService);
    }
}
