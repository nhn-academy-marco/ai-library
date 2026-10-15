package net.java21.library.core.book.dto;

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
    /** 임베딩 서버 장애 등으로 의미 검색 대신 키워드 검색 결과를 돌려준 경우 true */
    private final boolean keywordFallback;
    /**
     * RAG 검색에서 AI 추천이 아직 준비되지 않아 백그라운드에서 생성 중인 경우 true.
     * 화면은 목록을 먼저 보여 주고 추천은 {@code GET /api/recommendations}로 받아 채웁니다.
     */
    private final boolean aiPending;

    public BookSearchResult(Page<BookSearchResponse> books, List<BookAiRecommendationResponse> aiResponse) {
        this(books, aiResponse, 0L, false, false);
    }

    public BookSearchResult(Page<BookSearchResponse> books, List<BookAiRecommendationResponse> aiResponse, long createdAt) {
        this(books, aiResponse, createdAt, false, false);
    }

    public BookSearchResult(Page<BookSearchResponse> books, List<BookAiRecommendationResponse> aiResponse,
                            long createdAt, boolean keywordFallback) {
        this(books, aiResponse, createdAt, keywordFallback, false);
    }

    @JsonCreator
    public BookSearchResult(
            @JsonProperty("books") Page<BookSearchResponse> books,
            @JsonProperty("aiResponse") List<BookAiRecommendationResponse> aiResponse,
            @JsonProperty("createdAt") long createdAt,
            @JsonProperty("keywordFallback") boolean keywordFallback,
            @JsonProperty("aiPending") boolean aiPending) {
        this.books = books;
        this.aiResponse = aiResponse;
        this.createdAt = createdAt == 0 ? System.currentTimeMillis() : createdAt;
        this.keywordFallback = keywordFallback;
        this.aiPending = aiPending;
    }

    /**
     * 의미 검색을 할 수 없어 키워드 검색 결과로 대체한 결과를 만듭니다.
     */
    public static BookSearchResult keywordFallback(Page<BookSearchResponse> books) {
        return new BookSearchResult(books, List.of(), 0L, true, false);
    }
}
