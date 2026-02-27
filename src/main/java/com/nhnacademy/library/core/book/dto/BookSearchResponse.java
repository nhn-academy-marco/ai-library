package com.nhnacademy.library.core.book.dto;

import com.nhnacademy.library.core.book.domain.Book;
import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 도서 검색 응답 DTO
 */
@Getter
@NoArgsConstructor
public class BookSearchResponse {

    private Long id;
    private String isbn;
    private String title;
    private String volumeTitle;
    private String authorName;
    private String publisherName;
    private BigDecimal price;
    private LocalDate editionPublishDate;
    private String imageUrl;
    private String bookContent;
    private Double similarity;
    private Double rrfScore;

    // 리뷰 정보 (05.review-rag-integration.md)
    private BigDecimal averageRating;
    private Long reviewCount;
    private String reviewSummary;

    @QueryProjection // 빌드 시 QBookSearchResponse를 생성하게 함
    public BookSearchResponse(Long id, String isbn, String title, String volumeTitle,
                              String authorName, String publisherName, BigDecimal price,
                              LocalDate editionPublishDate, String imageUrl, String bookContent) {
        this.id = id;
        this.isbn = isbn;
        this.title = title;
        this.volumeTitle = volumeTitle;
        this.authorName = authorName;
        this.publisherName = publisherName;
        this.price = price;
        this.editionPublishDate = editionPublishDate;
        this.imageUrl = imageUrl;
        this.bookContent = bookContent;
    }

    @QueryProjection
    public BookSearchResponse(Long id, String isbn, String title, String volumeTitle,
                              String authorName, String publisherName, BigDecimal price,
                              LocalDate editionPublishDate, String imageUrl, String bookContent, Double similarity) {
        this(id, isbn, title, volumeTitle, authorName, publisherName, price, editionPublishDate, imageUrl, bookContent);
        this.similarity = similarity;
    }

    public BookSearchResponse(Long id, String isbn, String title, String volumeTitle,
                              String authorName, String publisherName, BigDecimal price,
                              LocalDate editionPublishDate, String imageUrl, String bookContent, Double similarity, Double rrfScore) {
        this(id, isbn, title, volumeTitle, authorName, publisherName, price, editionPublishDate, imageUrl, bookContent, similarity);
        this.rrfScore = rrfScore;
    }

    /**
     * 리뷰 정보를 포함하는 생성자 (05.review-rag-integration.md)
     * QueryDSL @QueryProjection에서 LEFT JOIN으로 리뷰 정보를 가져올 때 사용
     */
    @QueryProjection
    public BookSearchResponse(Long id, String isbn, String title, String volumeTitle,
                              String authorName, String publisherName, BigDecimal price,
                              LocalDate editionPublishDate, String imageUrl, String bookContent,
                              Double similarity, Double rrfScore,
                              BigDecimal averageRating, Long reviewCount, String reviewSummary) {
        this(id, isbn, title, volumeTitle, authorName, publisherName, price, editionPublishDate, imageUrl, bookContent, similarity, rrfScore);
        this.averageRating = averageRating;
        this.reviewCount = reviewCount;
        this.reviewSummary = reviewSummary;
    }

    // 기존 Entity 변환 로직 유지
    public static BookSearchResponse from(Book book) {
        return new BookSearchResponse(
                book.getId(),
                book.getIsbn(),
                book.getTitle(),
                book.getVolumeTitle(),
                book.getAuthorName(),
                book.getPublisherName(),
                book.getPrice(),
                book.getEditionPublishDate(),
                book.getImageUrl(),
                book.getBookContent()
        );
    }

    // 리뷰 정보 Setter (05.review-rag-integration.md)

    public void setAverageRating(BigDecimal averageRating) {
        this.averageRating = averageRating;
    }

    public void setReviewCount(Long reviewCount) {
        this.reviewCount = reviewCount;
    }

    public void setReviewSummary(String reviewSummary) {
        this.reviewSummary = reviewSummary;
    }

    // 개인화 관련 Setter (Step 6)

    /**
     * 코사인 유사도를 설정합니다.
     * <p>개인화 계산 시 사용됩니다.</p>
     *
     * @param similarity 코사인 유사도
     */
    public void setSimilarity(Double similarity) {
        this.similarity = similarity;
    }

    /**
     * RRF 점수를 설정합니다.
     * <p>개인화 계산 시 사용됩니다.</p>
     *
     * @param rrfScore RRF 점수
     */
    public void setRrfScore(Double rrfScore) {
        this.rrfScore = rrfScore;
    }
}