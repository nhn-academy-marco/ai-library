package com.nhnacademy.library.external.telegram.service;

import com.nhnacademy.library.core.book.service.personalization.PersonalizationService;
import com.nhnacademy.library.external.telegram.dto.FeedbackRequest;
import com.nhnacademy.library.external.telegram.dto.FeedbackType;
import com.nhnacademy.library.external.telegram.entity.SearchFeedback;
import com.nhnacademy.library.external.telegram.repository.SearchFeedbackRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FeedbackServiceImpl 단위 테스트")
class FeedbackServiceImplTest {

    @Mock
    private SearchFeedbackRepository searchFeedbackRepository;

    @Mock
    private PersonalizationService personalizationService;

    @InjectMocks
    private FeedbackServiceImpl feedbackService;

    @Test
    @DisplayName("피드백 저장 후 해당 사용자의 선호도 벡터 캐시를 무효화해야 한다")
    void recordFeedback_EvictsPreferenceCacheAfterSave() {
        Long chatId = 100L;
        FeedbackRequest request = new FeedbackRequest("query", 1L, FeedbackType.GOOD);
        when(searchFeedbackRepository.save(any(SearchFeedback.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        SearchFeedback saved = feedbackService.recordFeedback(chatId, request);

        assertThat(saved.getBookId()).isEqualTo(1L);
        InOrder order = inOrder(searchFeedbackRepository, personalizationService);
        order.verify(searchFeedbackRepository).save(any(SearchFeedback.class));
        order.verify(personalizationService).evictUserPreferenceCache(chatId);
    }
}
