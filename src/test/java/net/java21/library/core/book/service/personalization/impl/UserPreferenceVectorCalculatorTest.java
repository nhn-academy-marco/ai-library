package net.java21.library.core.book.service.personalization.impl;

import net.java21.library.core.book.repository.BookRepository;
import net.java21.library.external.telegram.dto.FeedbackType;
import net.java21.library.external.telegram.entity.SearchFeedback;
import net.java21.library.external.telegram.repository.SearchFeedbackRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * UserPreferenceVectorCalculator 단위 테스트
 *
 * <p>사용자 선호도 벡터 계산 로직을 검증합니다.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserPreferenceVectorCalculator 단위 테스트")
class UserPreferenceVectorCalculatorTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private SearchFeedbackRepository feedbackRepository;

    @InjectMocks
    private UserPreferenceVectorCalculator calculator;

    private static final Long CHAT_ID = 12345L;

    @BeforeEach
    void setUp() {
        // @Value 기본값과 동일하게 설정
        ReflectionTestUtils.setField(calculator, "minFeedbackCount", 3);
        ReflectionTestUtils.setField(calculator, "maxFeedbackCount", 20);
    }

    @Test
    @DisplayName("피드백이 없으면 null을 반환해야 한다 (콜드 스타트)")
    void calculateUserPreferenceVector_NoFeedback_ReturnsNull() {
        // Given
        when(feedbackRepository.findByChatIdOrderByCreatedAtDesc(CHAT_ID))
            .thenReturn(new ArrayList<>());

        // When
        float[] preferenceVector = calculator.calculate(CHAT_ID);

        // Then
        assertThat(preferenceVector).isNull();
    }

    @Test
    @DisplayName("피드백이 2개만 있으면 null을 반환해야 한다 (콜드 스타트)")
    void calculateUserPreferenceVector_TwoFeedbacks_ReturnsNull() {
        // Given
        List<SearchFeedback> feedbacks = List.of(
            SearchFeedback.of(CHAT_ID, "query", 1L, FeedbackType.GOOD),
            SearchFeedback.of(CHAT_ID, "query", 2L, FeedbackType.GOOD)
        );
        when(feedbackRepository.findByChatIdOrderByCreatedAtDesc(CHAT_ID))
            .thenReturn(feedbacks);

        // When
        float[] preferenceVector = calculator.calculate(CHAT_ID);

        // Then
        assertThat(preferenceVector).isNull();
    }

    @Test
    @DisplayName("GOOD 피드백이 3개 이상이면 선호도 벡터를 계산해야 한다")
    void calculateUserPreferenceVector_ThreeGoodFeedbacks_ReturnsVector() {
        // Given
        List<SearchFeedback> feedbacks = List.of(
            SearchFeedback.of(CHAT_ID, "query1", 1L, FeedbackType.GOOD),
            SearchFeedback.of(CHAT_ID, "query2", 2L, FeedbackType.GOOD),
            SearchFeedback.of(CHAT_ID, "query3", 3L, FeedbackType.GOOD)
        );

        when(feedbackRepository.findByChatIdOrderByCreatedAtDesc(CHAT_ID))
            .thenReturn(feedbacks);

        float[][] embeddings = {
            {1.0f, 2.0f, 3.0f},
            {3.0f, 4.0f, 5.0f},
            {5.0f, 6.0f, 7.0f}
        };

        when(bookRepository.findEmbeddingsByIds(anyList()))
            .thenReturn(List.of(embeddings[0], embeddings[1], embeddings[2]));

        // When
        float[] preferenceVector = calculator.calculate(CHAT_ID);

        // Then
        assertThat(preferenceVector).isNotNull();
        assertThat(preferenceVector).hasSize(3);
        assertThat(preferenceVector).isEqualTo(new float[]{3.0f, 4.0f, 5.0f}); // 평균
    }

    @Test
    @DisplayName("BAD 피드백은 필터링되어야 한다")
    void calculateUserPreferenceVector_WithBadFeedback_FiltersBad() {
        // Given
        List<SearchFeedback> feedbacks = List.of(
            SearchFeedback.of(CHAT_ID, "query1", 1L, FeedbackType.GOOD),
            SearchFeedback.of(CHAT_ID, "query2", 2L, FeedbackType.BAD),
            SearchFeedback.of(CHAT_ID, "query3", 3L, FeedbackType.GOOD),
            SearchFeedback.of(CHAT_ID, "query4", 4L, FeedbackType.GOOD)
        );

        when(feedbackRepository.findByChatIdOrderByCreatedAtDesc(CHAT_ID))
            .thenReturn(feedbacks);

        float[][] embeddings = {
            {1.0f, 1.0f, 1.0f},
            {3.0f, 3.0f, 3.0f},
            {5.0f, 5.0f, 5.0f}
        };

        when(bookRepository.findEmbeddingsByIds(anyList()))
            .thenReturn(List.of(embeddings[0], embeddings[1], embeddings[2]));

        // When
        float[] preferenceVector = calculator.calculate(CHAT_ID);

        // Then
        assertThat(preferenceVector).isNotNull();
        assertThat(preferenceVector).isEqualTo(new float[]{3.0f, 3.0f, 3.0f}); // GOOD 피드백만 평균
    }

    @Test
    @DisplayName("최대 20개의 피드백만 사용해야 한다")
    void calculateUserPreferenceVector_MoreThanMaxFeedback_LimitsToMax() {
        // Given
        List<SearchFeedback> feedbacks = new ArrayList<>();
        for (long i = 1; i <= 30; i++) {
            feedbacks.add(SearchFeedback.of(CHAT_ID, "query", i, FeedbackType.GOOD));
        }

        when(feedbackRepository.findByChatIdOrderByCreatedAtDesc(CHAT_ID))
            .thenReturn(feedbacks);

        List<float[]> embeddings = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            embeddings.add(new float[]{(float) i, (float) i, (float) i});
        }

        when(bookRepository.findEmbeddingsByIds(anyList()))
            .thenReturn(embeddings);

        // When
        float[] preferenceVector = calculator.calculate(CHAT_ID);

        // Then
        assertThat(preferenceVector).isNotNull();
        verify(bookRepository).findEmbeddingsByIds(anyList());
    }

    @Test
    @DisplayName("임베딩 조회 시 최대 20개의 도서 ID만 전달해야 한다")
    @SuppressWarnings("unchecked")
    void calculate_MoreThanMaxFeedback_PassesAtMostMaxIds() {
        List<SearchFeedback> feedbacks = new ArrayList<>();
        for (long i = 1; i <= 30; i++) {
            feedbacks.add(SearchFeedback.of(CHAT_ID, "query", i, FeedbackType.GOOD));
        }
        when(feedbackRepository.findByChatIdOrderByCreatedAtDesc(CHAT_ID)).thenReturn(feedbacks);
        when(bookRepository.findEmbeddingsByIds(anyList())).thenReturn(List.of(new float[]{1.0f}));

        calculator.calculate(CHAT_ID);

        ArgumentCaptor<List<Long>> captor = ArgumentCaptor.forClass(List.class);
        verify(bookRepository).findEmbeddingsByIds(captor.capture());
        assertThat(captor.getValue()).hasSize(20);
    }
}
