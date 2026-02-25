package com.nhnacademy.library.external.telegram.service;

import com.nhnacademy.library.external.telegram.dto.FeedbackRequest;
import com.nhnacademy.library.external.telegram.dto.FeedbackStats;
import com.nhnacademy.library.external.telegram.entity.SearchFeedback;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 피드백 비즈니스 로직 Service 인터페이스
 */
public interface FeedbackService {

    /**
     * 피드백을 저장합니다.
     *
     * @param chatId  Telegram 사용자 ID
     * @param request 피드백 요청
     * @return 저장된 피드백 Entity
     */
    SearchFeedback recordFeedback(Long chatId, FeedbackRequest request);

    /**
     * 특정 사용자의 피드백 목록을 조회합니다.
     *
     * @param chatId Telegram 사용자 ID
     * @return 해당 사용자의 피드백 목록
     */
    List<SearchFeedback> getUserFeedback(Long chatId);

    /**
     * 특정 도서의 피드백 통계를 조회합니다.
     *
     * @param bookId 도서 ID
     * @return 피드백 통계
     */
    FeedbackStats getBookFeedbackStats(Long bookId);

    /**
     * 특정 검색어의 피드백 통계를 조회합니다.
     *
     * @param query 검색어
     * @return 피드백 통계
     */
    FeedbackStats getQueryFeedbackStats(String query);

    /**
     * 최근 N일간의 피드백 목록을 조회합니다.
     *
     * @param days 일수
     * @return 최근 N일간의 피드백 목록
     */
    List<SearchFeedback> getRecentFeedback(int days);
}
