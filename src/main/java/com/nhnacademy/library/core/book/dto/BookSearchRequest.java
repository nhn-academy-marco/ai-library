package com.nhnacademy.library.core.book.dto;

import com.nhnacademy.library.core.book.domain.SearchType;
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

    SearchType searchType,
    float[] vector,
    Boolean isWarmUp
) {
    public BookSearchRequest {
        if (searchType == null) {
            searchType = SearchType.RAG;
        }
        if (isWarmUp == null) {
            isWarmUp = false;
        }
    }

    public BookSearchRequest(String keyword, String isbn, SearchType searchType, float[] vector) {
        this(keyword, isbn, searchType, vector, false);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BookSearchRequest that = (BookSearchRequest) o;
        return isWarmUp == that.isWarmUp &&
                Objects.equals(keyword, that.keyword) &&
                Objects.equals(isbn, that.isbn) &&
                Objects.equals(searchType, that.searchType) &&
                Arrays.equals(vector, that.vector);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(keyword, isbn, searchType, isWarmUp);
        result = 31 * result + Arrays.hashCode(vector);
        return result;
    }
}