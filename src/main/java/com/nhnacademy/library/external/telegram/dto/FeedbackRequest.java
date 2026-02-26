package com.nhnacademy.library.external.telegram.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 피드백 요청 DTO
 *
 * <p>Telegram API callback_data 제한 (64 bytes)으로 인해
 * 검색어는 포함하지 않고 bookId와 type만 포함합니다.
 *
 * @param bookId 도서 ID
 * @param type   피드백 타입
 */
public record FeedbackRequest(
        String query,  // 검색어 (선택사항, Telegram API 제한으로 인해 비어있을 수 있음)

        @NotNull(message = "도서 ID는 필수입니다.")
        Long bookId,

        @NotNull(message = "피드백 타입은 필수입니다.")
        FeedbackType type
) {
}
