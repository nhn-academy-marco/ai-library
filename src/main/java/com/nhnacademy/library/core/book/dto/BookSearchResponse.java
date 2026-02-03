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
}