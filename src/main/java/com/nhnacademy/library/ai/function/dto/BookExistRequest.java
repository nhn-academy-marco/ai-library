package com.nhnacademy.library.ai.function.dto;

/**
 * 도서 소장 여부 조회 요청
 *
 * @param libCode 도서관 코드 (6자리)
 * @param isbn13 ISBN-13 (13자리)
 */
public record BookExistRequest(
    String libCode,
    String isbn13
) {
    public BookExistRequest {
        if (libCode == null || libCode.isBlank()) {
            throw new IllegalArgumentException("libCode는 필수입니다.");
        }
        if (isbn13 == null || isbn13.isBlank()) {
            throw new IllegalArgumentException("isbn13는 필수입니다.");
        }
    }
}
