package com.nhnacademy.library.core.review.domain;

import com.nhnacademy.library.core.book.domain.Book;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "book_review_summary")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BookReviewSummary {

    @Id
    private Long bookId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "book_id")
    private Book book;

    @Column(nullable = false)
    private Integer reviewCount = 0;

    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal averageRating = BigDecimal.ZERO;

    @Column(name ="rating_1_count", nullable = false)
    private Integer rating1Count = 0;

    @Column(name ="rating_2_count", nullable = false)
    private Integer rating2Count = 0;

    @Column(name ="rating_3_count", nullable = false)
    private Integer rating3Count = 0;

    @Column(name ="rating_4_count", nullable = false)
    private Integer rating4Count = 0;

    @Column(name ="rating_5_count", nullable = false)
    private Integer rating5Count = 0;

    @Column
    private LocalDateTime lastReviewedAt;

    @Column(columnDefinition = "TEXT")
    private String reviewSummary;

    @Column(nullable = false)
    private Boolean isSummaryDirty = true;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private long version;

    public BookReviewSummary(Book book) {
        this.book = book;
        this.bookId = book.getId();
        this.updatedAt = LocalDateTime.now();
        this.isSummaryDirty = true;
    }

    public void addReview(int rating) {
        this.reviewCount++;
        updateRatingCount(rating);
        calculateAverageRating();
        this.lastReviewedAt = LocalDateTime.now();
        this.isSummaryDirty = true;
        this.updatedAt = LocalDateTime.now();
    }

    private void updateRatingCount(int rating) {
        switch (rating) {
            case 1 -> this.rating1Count++;
            case 2 -> this.rating2Count++;
            case 3 -> this.rating3Count++;
            case 4 -> this.rating4Count++;
            case 5 -> this.rating5Count++;
        }
    }

    private void calculateAverageRating() {
        long totalRating = (long) rating1Count + (rating2Count * 2L) + (rating3Count * 3L) + (rating4Count * 4L) + (rating5Count * 5L);
        this.averageRating = BigDecimal.valueOf(totalRating).divide(BigDecimal.valueOf(reviewCount), 2, BigDecimal.ROUND_HALF_UP);
    }

    public void updateSummary(String summary) {
        this.reviewSummary = summary;
        this.isSummaryDirty = false;
        this.updatedAt = LocalDateTime.now();
    }
}
