package com.nhnacademy.library.batch.init.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class BookRawData {

    private Long id;
    private String isbn;
    private String volumeTitle;
    private String title;
    private String authorName;
    private String publisherName;
    private LocalDate firstPublishDate;
    private BigDecimal price;
    private String imageUrl;
    private String bookContent;
    private String subtitle;
    private LocalDate editionPublishDate;

    public BookRawData(Long id, String isbn, String volumeTitle, String title, String authorName, String publisherName, LocalDate firstPublishDate, BigDecimal price, String imageUrl, String bookContent, String subtitle, LocalDate editionPublishDate) {
        this.id = id;
        this.isbn = isbn;
        this.volumeTitle = volumeTitle;
        this.title = title;
        this.authorName = authorName;
        this.publisherName = publisherName;
        this.firstPublishDate = firstPublishDate;
        this.price = price;
        this.imageUrl = imageUrl;
        this.bookContent = bookContent;
        this.subtitle = subtitle;
        this.editionPublishDate = editionPublishDate;
    }

}
