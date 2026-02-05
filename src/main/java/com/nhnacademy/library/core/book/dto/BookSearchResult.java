package com.nhnacademy.library.core.book.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 도서 검색 결과 DTO
 */
@Getter
@Builder
public class BookSearchResult {
    private final Page<BookSearchResponse> books;
    private final List<BookAiRecommendationResponse> aiResponse;
    private final long createdAt;

    public BookSearchResult(Page<BookSearchResponse> books, List<BookAiRecommendationResponse> aiResponse) {
        this.books = books;
        this.aiResponse = aiResponse;
        this.createdAt = System.currentTimeMillis();
    }

    @JsonCreator
    public BookSearchResult(
            @JsonProperty("books") Page<BookSearchResponse> books,
            @JsonProperty("aiResponse") List<BookAiRecommendationResponse> aiResponse,
            @JsonProperty("createdAt") long createdAt) {
        this.books = books;
        this.aiResponse = aiResponse;
        this.createdAt = createdAt == 0 ? System.currentTimeMillis() : createdAt;
    }
}
