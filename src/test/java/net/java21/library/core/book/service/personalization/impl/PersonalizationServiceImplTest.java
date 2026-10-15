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
        assertThat(book3.getSimilarity()).isEqualTo(1.0, org.assertj.core.data.Offset.offset(1e-6));
        assertThat(book1.getSimilarity()).isEqualTo(0.0, org.assertj.core.data.Offset.offset(1e-6));
        assertThat(personalized.get(0).getId()).isEqualTo(3L);
        verify(bookRepository).findIdAndEmbeddingsByIds(List.of(1L, 2L, 3L));
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
