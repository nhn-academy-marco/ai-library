package com.nhnacademy.library.external.telegram.service;

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

        return searchFeedbackRepository.save(feedback);
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
}
