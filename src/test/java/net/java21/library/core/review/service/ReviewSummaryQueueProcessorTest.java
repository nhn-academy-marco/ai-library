package net.java21.library.core.review.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewSummaryQueueProcessor 단위 테스트")
class ReviewSummaryQueueProcessorTest {

    @Mock
    private ReviewSummaryQueueService queueService;

    @Mock
    private ReviewAiSummaryService reviewAiSummaryService;

    @InjectMocks
    private ReviewSummaryQueueProcessor processor;

    @Test
    @DisplayName("처리 실패 시 예외를 다시 던져 재시도/DLQ 흐름으로 넘기고, pending 표시는 해제해야 한다")
    void processTask_Failure_RethrowsAndRemovesPending() {
        doThrow(new IllegalStateException("boom")).when(reviewAiSummaryService).generateSummary(1L);

        assertThatThrownBy(() -> processor.processTask(new ReviewSummaryTask(1L)))
            .isInstanceOf(IllegalStateException.class);
        verify(queueService).removePending(1L);
    }
}
