package com.nhnacademy.library.core.book.dto;

import jakarta.validation.constraints.Size;
import java.util.Arrays;
import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BookSearchRequest that = (BookSearchRequest) o;
        return Objects.equals(keyword, that.keyword) &&
                Objects.equals(isbn, that.isbn) &&
                Objects.equals(searchType, that.searchType) &&
                Arrays.equals(vector, that.vector);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(keyword, isbn, searchType);
        result = 31 * result + Arrays.hashCode(vector);
        return result;
    }
}