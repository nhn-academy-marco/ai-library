package com.nhnacademy.library.external.telegram.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 피드백 요청 DTO
 *
 * @param query  검색어
 * @param bookId 도서 ID
 * @param type   피드백 타입
 */
public record FeedbackRequest(
        @NotBlank(message = "검색어는 비워둘 수 없습니다.")
        String query,

        @NotNull(message = "도서 ID는 필수입니다.")
        Long bookId,

        @NotNull(message = "피드백 타입은 필수입니다.")
        FeedbackType type
) {
}
