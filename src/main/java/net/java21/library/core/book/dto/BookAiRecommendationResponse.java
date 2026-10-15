package net.java21.library.core.book.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * AI 도서 추천 응답 항목 DTO
 *
 * <p>LLM은 id·relevance·why만 돌려주고, 나머지 도서 정보(제목·저자·표지 등)는 후보 도서에서 채웁니다
 * ({@link #applyBook(BookSearchResponse)}). 추천 패널은 아래 검색 결과 목록과 독립적으로 이 정보만으로 그립니다.</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class BookAiRecommendationResponse {
    private Long id;
    private Integer relevance;
    private String why;
    private Double similarity;
    private Double rrfScore;

    // 추천 패널 표시용 도서 정보 (후보 도서에서 복사)
    private String isbn;
    private String title;
    private String authorName;
    private String publisherName;
    private LocalDate editionPublishDate;
    private String imageUrl;

    /**
     * 후보 도서의 점수와 표시용 정보를 이 추천 항목에 복사합니다.
     *
     * @param book 같은 ID의 후보 도서
     */
    public void applyBook(BookSearchResponse book) {
        this.similarity = book.getSimilarity();
        this.rrfScore = book.getRrfScore();
        this.isbn = book.getIsbn();
        this.title = book.getTitle();
        this.authorName = book.getAuthorName();
        this.publisherName = book.getPublisherName();
        this.editionPublishDate = book.getEditionPublishDate();
        this.imageUrl = book.getImageUrl();
    }
}
