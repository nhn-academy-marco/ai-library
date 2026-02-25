package com.nhnacademy.library.external.telegram.dto;

import com.nhnacademy.library.external.telegram.entity.SearchFeedback;

import java.util.List;

/**
 * 피드백 통계 DTO
 *
 * @param goodCount     긍정 피드백 수
 * @param badCount      부정 피드백 수
 * @param totalCount    전체 피드백 수
 * @param goodRatio     긍정 피드백 비율 (0.0 ~ 1.0)
 * @param feedbackScore 피드백 점수 (-1.0 ~ 1.0)
 */
public record FeedbackStats(
        int goodCount,
        int badCount,
        int totalCount,
        double goodRatio,
        double feedbackScore
) {
    /**
     * 피드백 목록으로부터 통계를 계산합니다.
     *
     * @param feedbacks 피드백 목록
     * @return 피드백 통계
     */
    public static FeedbackStats from(List<SearchFeedback> feedbacks) {
        if (feedbacks == null || feedbacks.isEmpty()) {
            return new FeedbackStats(0, 0, 0, 0.0, 0.0);
        }

        int goodCount = (int) feedbacks.stream()
                .filter(f -> f.getType() == FeedbackType.GOOD)
                .count();

        int badCount = (int) feedbacks.stream()
                .filter(f -> f.getType() == FeedbackType.BAD)
                .count();

        int totalCount = feedbacks.size();
        double goodRatio = totalCount > 0 ? (double) goodCount / totalCount : 0.0;
        double feedbackScore = totalCount > 0
                ? (double) (goodCount - badCount) / totalCount
                : 0.0;

        return new FeedbackStats(goodCount, badCount, totalCount, goodRatio, feedbackScore);
    }
}
