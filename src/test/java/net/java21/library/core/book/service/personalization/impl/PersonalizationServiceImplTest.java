package net.java21.library.core.book.service.personalization.impl;

import net.java21.library.core.book.dto.BookSearchResponse;
import net.java21.library.core.book.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * PersonalizationService 단위 테스트
 *
 * <p>개인화 검색(재정렬)과 선호도 벡터 계산/캐시 위임을 검증합니다.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PersonalizationService 단위 테스트")
class PersonalizationServiceImplTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private UserPreferenceVectorCalculator userPreferenceVectorCalculator;

    @InjectMocks
    private PersonalizationServiceImpl personalizationService;

    private static final Long CHAT_ID = 12345L;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(personalizationService, "personalizationWeight", 0.3);
    }

    @Test
    @DisplayName("선호도 벡터 계산은 캐시가 적용된 별도 빈에 위임해야 한다")
    void calculateUserPreferenceVector_DelegatesToCalculator() {
        float[] vector = {1.0f, 0.0f};
        when(userPreferenceVectorCalculator.calculate(CHAT_ID)).thenReturn(vector);

        assertThat(personalizationService.calculateUserPreferenceVector(CHAT_ID)).isSameAs(vector);
    }

    @Test
    @DisplayName("선호도 벡터가 없으면 검색 결과를 그대로 반환해야 한다")
    void personalizedSearch_NoPreferenceVector_ReturnsOriginalResults() {
        // Given
        List<BookSearchResponse> results = List.of(
            createBookSearchResponse(1L, "Book 1"),
            createBookSearchResponse(2L, "Book 2")
        );
        when(userPreferenceVectorCalculator.calculate(CHAT_ID)).thenReturn(null);

        // When
        List<BookSearchResponse> personalizedResults =
            personalizationService.personalizedSearch(results, CHAT_ID);

        // Then
        assertThat(personalizedResults).isSameAs(results);
        verifyNoInteractions(bookRepository);
    }

    @Test
    @DisplayName("IN 쿼리 결과 순서가 입력과 달라도 도서 ID 기준으로 임베딩을 매핑해야 한다")
    void personalizedSearch_ResultOrderDiffers_MapsEmbeddingById() {
        // Given: 선호도 벡터는 x축 방향
        when(userPreferenceVectorCalculator.calculate(CHAT_ID)).thenReturn(new float[]{1.0f, 0.0f});

        BookSearchResponse book1 = createBookSearchResponse(1L, "Book 1");
        BookSearchResponse book2 = createBookSearchResponse(2L, "Book 2");
        BookSearchResponse book3 = createBookSearchResponse(3L, "Book 3");

        // DB가 입력(1, 2, 3)과 다른 순서(3, 1)로 반환하고, 2번은 임베딩 없음
        when(bookRepository.findIdAndEmbeddingsByIds(anyList())).thenReturn(List.of(
            new Object[]{3L, new float[]{1.0f, 0.0f}},  // 선호도와 동일 방향
            new Object[]{1L, new float[]{0.0f, 1.0f}}   // 직교
        ));

        // When
        List<BookSearchResponse> personalized =
            personalizationService.personalizedSearch(List.of(book1, book2, book3), CHAT_ID);

        // Then
        assertThat(personalized.get(0).getId()).isEqualTo(3L);
        assertThat(personalized.get(0).getSimilarity()).isEqualTo(1.0, org.assertj.core.data.Offset.offset(1e-6));
        assertThat(personalized).filteredOn(r -> r.getId().equals(1L)).singleElement()
            .satisfies(r -> assertThat(r.getSimilarity()).isEqualTo(0.0, org.assertj.core.data.Offset.offset(1e-6)));
        verify(bookRepository).findIdAndEmbeddingsByIds(List.of(1L, 2L, 3L));
    }

    @Test
    @DisplayName("같은 입력 객체로 두 번 호출해도 개인화 점수가 누적되지 않아야 한다")
    void personalizedSearch_CalledTwiceWithSameInput_ScoresDoNotAccumulate() {
        // Given: 시맨틱 캐시가 같은 객체를 다시 돌려주는 상황
        when(userPreferenceVectorCalculator.calculate(CHAT_ID)).thenReturn(new float[]{1.0f, 0.0f});
        when(bookRepository.findIdAndEmbeddingsByIds(anyList())).thenReturn(List.<Object[]>of(
            new Object[]{1L, new float[]{1.0f, 0.0f}}
        ));
        BookSearchResponse cached = createBookSearchResponse(1L, "Book 1");
        cached.setRrfScore(0.5);
        List<BookSearchResponse> cachedResults = List.of(cached);

        // When
        double first = personalizationService.personalizedSearch(cachedResults, CHAT_ID).get(0).getRrfScore();
        double second = personalizationService.personalizedSearch(cachedResults, CHAT_ID).get(0).getRrfScore();

        // Then
        assertThat(first).isEqualTo(0.8, org.assertj.core.data.Offset.offset(1e-6));
        assertThat(second).isEqualTo(first);
    }

    @Test
    @DisplayName("개인화 점수는 입력 객체를 수정하지 않고 복사본에 적용해야 한다")
    void personalizedSearch_DoesNotMutateInput() {
        // Given
        when(userPreferenceVectorCalculator.calculate(CHAT_ID)).thenReturn(new float[]{1.0f, 0.0f});
        when(bookRepository.findIdAndEmbeddingsByIds(anyList())).thenReturn(List.<Object[]>of(
            new Object[]{1L, new float[]{1.0f, 0.0f}}
        ));
        BookSearchResponse original = createBookSearchResponse(1L, "Book 1");
        original.setRrfScore(0.5);

        // When
        BookSearchResponse result = personalizationService.personalizedSearch(List.of(original), CHAT_ID).get(0);

        // Then
        assertThat(result).isNotSameAs(original);
        assertThat(original.getRrfScore()).isEqualTo(0.5);
        assertThat(original.getSimilarity()).isNull();
    }

    @Test
    @DisplayName("캐시 무효화는 별도 빈에 위임해야 한다")
    void evictUserPreferenceCache_DelegatesToCalculator() {
        // When
        personalizationService.evictUserPreferenceCache(CHAT_ID);

        // Then
        verify(userPreferenceVectorCalculator).evict(CHAT_ID);
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
