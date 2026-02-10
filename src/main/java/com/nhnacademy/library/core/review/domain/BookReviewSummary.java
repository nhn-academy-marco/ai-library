package com.nhnacademy.library.core.review.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "book_review_summary")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
public class BookReviewSummary {

    @Id
    @Column(name = "book_id")
    private Long bookId;

    @Column(nullable = false)
    private Long reviewCount = 0l;

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

    @Column(name = "last_summarized_count")
    private Long lastSummarizedCount = 0L;

    @Column(name = "is_generating")
    private Boolean isGenerating = false;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private long version;

    public BookReviewSummary(long bookId) {
        this.bookId = bookId;
        this.updatedAt = LocalDateTime.now();
        this.isSummaryDirty = true;
    }

    public BookReviewSummary(Long bookId, Long reviewCount, BigDecimal averageRating, Integer rating1Count, Integer rating2Count, Integer rating3Count, Integer rating4Count, Integer rating5Count, LocalDateTime lastReviewedAt) {
        this.bookId = bookId;
        this.reviewCount = reviewCount;
        this.averageRating = averageRating;
        this.rating1Count = rating1Count;
        this.rating2Count = rating2Count;
        this.rating3Count = rating3Count;
        this.rating4Count = rating4Count;
        this.rating5Count = rating5Count;
        this.lastReviewedAt = lastReviewedAt;
    }

    public void updateStat(Long reviewCount, BigDecimal averageRating, Integer rating1Count, Integer rating2Count, Integer rating3Count, Integer rating4Count, Integer rating5Count, LocalDateTime lastReviewedAt){
        this.reviewCount = reviewCount;
        this.averageRating = averageRating;
        this.rating1Count = rating1Count;
        this.rating2Count = rating2Count;
        this.rating3Count = rating3Count;
        this.rating4Count = rating4Count;
        this.rating5Count = rating5Count;
    }

    public void updateSummary(String summary) {
        this.reviewSummary = summary;
        this.isSummaryDirty = false;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateSummaryWithCount(String summary, long lastSummarizedCount) {
        this.reviewSummary = summary;
        this.lastSummarizedCount = lastSummarizedCount;
        this.isSummaryDirty = false;
        this.updatedAt = LocalDateTime.now();
    }

    public void setGenerating(boolean generating) {
        this.isGenerating = generating;
    }

    public void setSummaryDirty(boolean summaryDirty) {
        this.isSummaryDirty = summaryDirty;
    }
}
