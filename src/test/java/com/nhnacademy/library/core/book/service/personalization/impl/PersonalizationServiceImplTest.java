package com.nhnacademy.library.core.book.service.personalization.impl;

import com.nhnacademy.library.core.book.dto.BookSearchRequest;
import com.nhnacademy.library.core.book.dto.BookSearchResponse;
import com.nhnacademy.library.core.book.repository.BookRepository;
import com.nhnacademy.library.external.telegram.dto.FeedbackType;
import com.nhnacademy.library.external.telegram.entity.SearchFeedback;
import com.nhnacademy.library.external.telegram.repository.SearchFeedbackRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PersonalizationService 단위 테스트
 *
 * <p>사용자 선호도 계산 및 개인화 검색 기능을 검증합니다.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PersonalizationService 단위 테스트")
class PersonalizationServiceImplTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private SearchFeedbackRepository feedbackRepository;

    @InjectMocks
    private PersonalizationServiceImpl personalizationService;

    private static final Long CHAT_ID = 12345L;

    @Test
    @DisplayName("피드백이 없으면 null을 반환해야 한다 (콜드 스타트)")
    void calculateUserPreferenceVector_NoFeedback_ReturnsNull() {
        // Given
        when(feedbackRepository.findByChatIdOrderByCreatedAtDesc(CHAT_ID))
            .thenReturn(new ArrayList<>());

        // When
        float[] preferenceVector = personalizationService.calculateUserPreferenceVector(CHAT_ID);

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
        float[] preferenceVector = personalizationService.calculateUserPreferenceVector(CHAT_ID);

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
        float[] preferenceVector = personalizationService.calculateUserPreferenceVector(CHAT_ID);

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
        float[] preferenceVector = personalizationService.calculateUserPreferenceVector(CHAT_ID);

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
        float[] preferenceVector = personalizationService.calculateUserPreferenceVector(CHAT_ID);

        // Then
        assertThat(preferenceVector).isNotNull();
        verify(bookRepository).findEmbeddingsByIds(anyList());
    }

    @Test
    @DisplayName("선호도 벡터가 없으면 검색 결과를 그대로 반환해야 한다")
    void personalizedSearch_NoPreferenceVector_ReturnsOriginalResults() {
        // Given
        List<BookSearchResponse> results = List.of(
            createBookSearchResponse(1L, "Book 1"),
            createBookSearchResponse(2L, "Book 2")
        );

        when(feedbackRepository.findByChatIdOrderByCreatedAtDesc(CHAT_ID))
            .thenReturn(new ArrayList<>());

        // When
        List<BookSearchResponse> personalizedResults =
            personalizationService.personalizedSearch(results, CHAT_ID);

        // Then
        assertThat(personalizedResults).hasSameSizeAs(results);
    }

    @Test
    @DisplayName("캐시 무효화를 수행해야 한다")
    void evictUserPreferenceCache_ShouldSucceed() {
        // When
        personalizationService.evictUserPreferenceCache(CHAT_ID);

        // Then (예외 없이 완료)
        // @CacheEvict가 적용되므로 별도 검증 없음
    }

    private BookSearchResponse createBookSearchResponse(Long id, String title) {
        return new BookSearchResponse(
            id,
            "isbn" + id,
            title,
            null,
            "author",
            "publisher",
            BigDecimal.valueOf(15000),
            LocalDate.now(),
            null,
            null
        );
    }
}

