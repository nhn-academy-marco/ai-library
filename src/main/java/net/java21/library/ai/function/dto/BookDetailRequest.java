package net.java21.library.ai.function.dto;

import lombok.Builder;

/**
 * 도서 상세 정보 조회 요청 DTO
 */
@Builder
public record BookDetailRequest(
    String isbn13,
    boolean includeLoanInfo
) {
    public static BookDetailRequest of(String isbn13) {
        return new BookDetailRequest(isbn13, true);
    }
}
