package com.nhnacademy.library.core.book.dto;

import com.nhnacademy.library.core.book.domain.Book;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record BookViewResponse(
        Long id,
        String isbn,
        String title,
        String volumeTitle,
        String authorName,
        String publisherName,
        LocalDate firstPublishDate,
        LocalDate editionPublishDate,
        BigDecimal price,
        String subtitle,
        String bookContent,
        String imageUrl,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static BookViewResponse from(Book book) {
        return new BookViewResponse(
                book.getId(),
                book.getIsbn(),
                book.getTitle(),
                book.getVolumeTitle(),
                book.getAuthorName(),
                book.getPublisherName(),
                book.getFirstPublishDate(),
                book.getEditionPublishDate(),
                book.getPrice(),
                book.getSubtitle(),
                book.getBookContent(),
                book.getImageUrl(),
                book.getCreatedAt(),
                book.getUpdatedAt()
        );
    }
}