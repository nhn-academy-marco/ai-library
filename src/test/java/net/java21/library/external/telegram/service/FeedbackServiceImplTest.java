package net.java21.library.external.telegram.service;

import net.java21.library.core.book.service.personalization.PersonalizationService;
import net.java21.library.external.telegram.dto.FeedbackRequest;
import net.java21.library.external.telegram.dto.FeedbackType;
import net.java21.library.external.telegram.entity.SearchFeedback;
import net.java21.library.external.telegram.repository.SearchFeedbackRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
    @DisplayName("트랜잭션이 없으면 피드백 저장 직후 선호도 벡터 캐시를 무효화해야 한다")
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
    @Test
    @DisplayName("트랜잭션 안에서는 커밋 후에만 선호도 벡터 캐시를 무효화해야 한다")
    void recordFeedback_InTransaction_EvictsOnlyAfterCommit() {
        Long chatId = 200L;
        FeedbackRequest request = new FeedbackRequest("query", 1L, FeedbackType.GOOD);
        when(searchFeedbackRepository.save(any(SearchFeedback.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        TransactionSynchronizationManager.initSynchronization();
        try {
            feedbackService.recordFeedback(chatId, request);

            // 커밋 전에는 무효화하지 않음
            verify(personalizationService, never()).evictUserPreferenceCache(chatId);

            // 커밋 시뮬레이션
            TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);

            verify(personalizationService).evictUserPreferenceCache(chatId);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
