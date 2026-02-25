package com.nhnacademy.library.external.telegram.dto;

/**
 * 피드백 타입 열거형
 *
 * <p>사용자가 검색 결과에 대해 남긴 피드백의 종류를 나타냅니다.
 */
public enum FeedbackType {
    /**
     * 긍정 피드백 (좋았음)
     */
    GOOD(1, "좋았음"),

    /**
     * 부정 피드백 (별로였음)
     */
    BAD(-1, "별로였음");

    private final int scoreValue;
    private final String description;

    FeedbackType(int scoreValue, String description) {
        this.scoreValue = scoreValue;
        this.description = description;
    }

    /**
     * 피드백 점수 값을 반환합니다.
     *
     * @return 긍정은 +1, 부정은 -1
     */
    public int getScoreValue() {
        return scoreValue;
    }

    /**
     * 피드백 설명을 반환합니다.
     *
     * @return 피드백 설명
     */
    public String getDescription() {
        return description;
    }
}
