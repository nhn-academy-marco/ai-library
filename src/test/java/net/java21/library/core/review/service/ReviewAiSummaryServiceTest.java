package net.java21.library.core.review.service;

import net.java21.library.core.review.domain.BookReview;
import net.java21.library.core.review.domain.BookReviewSummary;
import net.java21.library.core.review.repository.BookReviewRepository;
import net.java21.library.core.review.repository.BookReviewSummaryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewAiSummaryService 단위 테스트")
class ReviewAiSummaryServiceTest {

    private static final long BOOK_ID = 1L;

    @Mock
    private BookReviewSummaryRepository summaryRepository;
    @Mock
    private BookReviewRepository reviewRepository;
    @Mock
    private ReviewSummarizer summarizer;
    @Mock
    private PlatformTransactionManager transactionManager;

    private ReviewAiSummaryService service;
    private BookReviewSummary summary;

    @BeforeEach
    void setUp() {
        lenient().when(transactionManager.getTransaction(any())).thenAnswer(invocation -> new SimpleTransactionStatus());
        service = new ReviewAiSummaryService(summaryRepository, reviewRepository, summarizer, transactionManager);

        summary = new BookReviewSummary(BOOK_ID);
        summary.updateStat(8L, BigDecimal.valueOf(3.0), 0, 0, 8, 0, 0, LocalDateTime.now());
        lenient().when(summaryRepository.findById(BOOK_ID)).thenReturn(Optional.of(summary));
    }

    @Test
    @DisplayName("LLM은 첫 트랜잭션이 커밋된 뒤, 트랜잭션 밖에서 호출한다")
    void callsLlmAfterFirstTransactionCommits() {
        BookReview review = mock(BookReview.class);
        when(review.getContent()).thenReturn("좋은 책입니다.");
        when(summarizer.getReduceThreshold()).thenReturn(5);
        when(reviewRepository.findAllByBookId(BOOK_ID)).thenReturn(List.of(review));
        when(summarizer.summarizeReviews(anyList())).thenReturn("[장점] 예제가 많습니다.");

        service.generateSummary(BOOK_ID);

        InOrder order = inOrder(transactionManager, summarizer);
        order.verify(transactionManager).commit(any());      // 1단계 커밋 (잠금 해제)
        order.verify(summarizer).summarizeReviews(anyList()); // 2단계 LLM 호출
        order.verify(transactionManager).commit(any());      // 3단계 커밋

        assertThat(summary.getReviewSummary()).isEqualTo("[장점] 예제가 많습니다.");
        assertThat(summary.getIsGenerating()).isFalse();
        assertThat(summary.getIsSummaryDirty()).isFalse();
    }

    @Test
    @DisplayName("LLM 호출 중 리뷰가 늘었다면 요약 후에도 다음 요약 대상으로 남긴다")
    void staysDirtyWhenReviewsAddedDuringGeneration() {
        BookReview review = mock(BookReview.class);
        when(review.getContent()).thenReturn("좋은 책입니다.");
        when(summarizer.getReduceThreshold()).thenReturn(5);
        when(reviewRepository.findAllByBookId(BOOK_ID)).thenReturn(List.of(review));
        when(summarizer.summarizeReviews(anyList())).thenAnswer(invocation -> {
            // LLM이 응답하는 동안 리뷰가 9건으로 늘어난 상황
            summary.updateStat(9L, BigDecimal.valueOf(3.0), 0, 0, 9, 0, 0, LocalDateTime.now());
            return "[장점] 예제가 많습니다.";
        });

        service.generateSummary(BOOK_ID);

        assertThat(summary.getLastSummarizedCount()).isEqualTo(8L);
        assertThat(summary.getIsSummaryDirty()).isTrue();
    }

    @Test
    @DisplayName("LLM 호출이 실패하면 생성 중 표시를 해제한다")
    void releasesGeneratingFlagOnFailure() {
        BookReview review = mock(BookReview.class);
        when(review.getContent()).thenReturn("좋은 책입니다.");
        when(summarizer.getReduceThreshold()).thenReturn(5);
        when(reviewRepository.findAllByBookId(BOOK_ID)).thenReturn(List.of(review));
        when(summarizer.summarizeReviews(anyList())).thenThrow(new IllegalStateException("LLM timeout"));

        service.generateSummary(BOOK_ID);

        assertThat(summary.getIsGenerating()).isFalse();
        assertThat(summary.getReviewSummary()).isNull();
    }

    @Test
    @DisplayName("리뷰 수가 기준(5건) 미만이면 LLM을 호출하지 않는다")
    void skipsWhenNotEnoughReviews() {
        summary.updateStat(3L, BigDecimal.valueOf(3.0), 0, 0, 3, 0, 0, LocalDateTime.now());

        service.generateSummary(BOOK_ID);

        verify(summarizer, never()).summarizeReviews(anyList());
    }

    @Test
    @DisplayName("요약 후 새 리뷰가 등록되면 다시 요약 대상이 된다")
    void updateStatMarksDirtyAfterSummary() {
        summary.completeSummary("기존 요약", 8L);
        assertThat(summary.getIsSummaryDirty()).isFalse();

        summary.updateStat(9L, BigDecimal.valueOf(3.0), 0, 0, 9, 0, 0, LocalDateTime.now());

        assertThat(summary.getIsSummaryDirty()).isTrue();
    }
}
