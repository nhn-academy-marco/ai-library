package com.nhnacademy.library.external.telegram.service;

import com.nhnacademy.library.core.book.service.personalization.PersonalizationService;
import com.nhnacademy.library.external.telegram.dto.FeedbackRequest;
import com.nhnacademy.library.external.telegram.dto.FeedbackStats;
import com.nhnacademy.library.external.telegram.entity.SearchFeedback;
import com.nhnacademy.library.external.telegram.repository.SearchFeedbackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 피드백 비즈니스 로직 Service 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {

    private final SearchFeedbackRepository searchFeedbackRepository;
    private final PersonalizationService personalizationService;

    @Override
    @Transactional
    public SearchFeedback recordFeedback(Long chatId, FeedbackRequest request) {
        log.info("Recording feedback: chatId={}, query={}, bookId={}, type={}",
            chatId, request.query(), request.bookId(), request.type());

        SearchFeedback feedback = SearchFeedback.of(
            chatId,
            request.query(),
            request.bookId(),
            request.type()
        );

        SearchFeedback saved = searchFeedbackRepository.save(feedback);

        // 새 피드백이 다음 검색의 선호도 벡터에 반영되도록 캐시 무효화
        personalizationService.evictUserPreferenceCache(chatId);

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SearchFeedback> getUserFeedback(Long chatId) {
        log.debug("Fetching feedback for chatId: {}", chatId);
        return searchFeedbackRepository.findByChatIdOrderByCreatedAtDesc(chatId);
    }

    @Override
    @Transactional(readOnly = true)
    public FeedbackStats getBookFeedbackStats(Long bookId) {
        log.debug("Calculating feedback stats for bookId: {}", bookId);

        List<SearchFeedback> feedbacks = searchFeedbackRepository.findByBookId(bookId);
        return FeedbackStats.from(feedbacks);
    }

    @Override
    @Transactional(readOnly = true)
    public FeedbackStats getQueryFeedbackStats(String query) {
        log.debug("Calculating feedback stats for query: {}", query);

        List<SearchFeedback> feedbacks = searchFeedbackRepository.findByQuery(query);
        return FeedbackStats.from(feedbacks);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SearchFeedback> getRecentFeedback(int days) {
        log.debug("Fetching recent feedback for last {} days", days);

        OffsetDateTime startDate = OffsetDateTime.now().minusDays(days);
        OffsetDateTime endDate = OffsetDateTime.now();

        return searchFeedbackRepository.findByCreatedAtBetween(startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasExistingFeedback(Long chatId, String query, Long bookId) {
        log.debug("Checking for existing feedback: chatId={}, query={}, bookId={}",
                  chatId, query, bookId);

        return searchFeedbackRepository
            .findByChatIdAndQueryAndBookId(chatId, query, bookId)
            .isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SearchFeedback> getAllFeedback() {
        log.debug("Fetching all feedback for CSV export");
        return searchFeedbackRepository.findAll();
    }
}
