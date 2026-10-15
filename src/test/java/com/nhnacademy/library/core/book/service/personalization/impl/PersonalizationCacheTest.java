package com.nhnacademy.library.core.book.service.personalization.impl;

import com.nhnacademy.library.core.book.repository.BookRepository;
import com.nhnacademy.library.core.book.service.personalization.PersonalizationService;
import com.nhnacademy.library.external.telegram.dto.FeedbackType;
import com.nhnacademy.library.external.telegram.entity.SearchFeedback;
import com.nhnacademy.library.external.telegram.repository.SearchFeedbackRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 선호도 벡터 캐시가 실제로 적용되는지 검증 (self-invocation 회귀 테스트)
 *
 * <p>DB 없이 최소한의 스프링 컨텍스트(캐시 프록시)만 띄워 검증합니다.</p>
 */
@SpringJUnitConfig(PersonalizationCacheTest.Config.class)
@DisplayName("PersonalizationService 캐시 적용 테스트")
class PersonalizationCacheTest {

    @Configuration
    @EnableCaching
    @Import({PersonalizationServiceImpl.class, UserPreferenceVectorCalculator.class})
    static class Config {
        @Bean("personalizationCacheManager")
        CacheManager personalizationCacheManager() {
            return new ConcurrentMapCacheManager("userPreferenceVectors");
        }
    }

    private static final Long CHAT_ID = 1L;

    @MockitoBean
    private BookRepository bookRepository;

    @MockitoBean
    private SearchFeedbackRepository feedbackRepository;

    @Autowired
    private PersonalizationService personalizationService;

    @Test
    @DisplayName("personalizedSearch를 반복 호출해도 선호도 벡터는 한 번만 계산되고, evict 후에는 다시 계산되어야 한다")
    void personalizedSearch_UsesCachedPreferenceVector() {
        when(feedbackRepository.findByChatIdOrderByCreatedAtDesc(CHAT_ID)).thenReturn(List.of(
            SearchFeedback.of(CHAT_ID, "q", 1L, FeedbackType.GOOD),
            SearchFeedback.of(CHAT_ID, "q", 2L, FeedbackType.GOOD),
            SearchFeedback.of(CHAT_ID, "q", 3L, FeedbackType.GOOD)
        ));
        when(bookRepository.findEmbeddingsByIds(anyList()))
            .thenReturn(List.of(new float[]{1.0f, 0.0f}));
        when(bookRepository.findIdAndEmbeddingsByIds(anyList())).thenReturn(List.of());

        personalizationService.personalizedSearch(List.of(), CHAT_ID);
        personalizationService.personalizedSearch(List.of(), CHAT_ID);

        verify(feedbackRepository, times(1)).findByChatIdOrderByCreatedAtDesc(CHAT_ID);

        personalizationService.evictUserPreferenceCache(CHAT_ID);
        personalizationService.personalizedSearch(List.of(), CHAT_ID);

        verify(feedbackRepository, times(2)).findByChatIdOrderByCreatedAtDesc(CHAT_ID);
    }
}
