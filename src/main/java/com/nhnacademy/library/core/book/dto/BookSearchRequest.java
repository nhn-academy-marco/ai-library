package com.nhnacademy.library.core.book.dto;

import jakarta.validation.constraints.Size;

/**
 * 도서 검색 요청 DTO
 *
 * @param keyword 검색 키워드 (제목, 저자, 출판사 등)
 * @param isbn    도서 ISBN (13자리)
 */
public record BookSearchRequest(
        @Size(max = 100)
        String keyword,

        @Size(max = 20)
        String isbn,

        String searchType,
        float[] vector
) {
    public BookSearchRequest {
        if (searchType == null) {
            searchType = "keyword";
        }
    }
}