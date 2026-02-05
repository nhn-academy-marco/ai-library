package com.nhnacademy.library.core.review.dto;


import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;
import lombok.ToString;


import java.time.OffsetDateTime;

@Getter
@ToString
public class BookReviewSummaryStatDto {

    private final Long reviewCount;
    private final Double averageRating;
    private final Integer rating1Count;
    private final Integer rating2Count;
    private final Integer rating3Count;
    private final Integer rating4Count;
    private final Integer rating5Count;
    private final OffsetDateTime lastReviewedAt;

    @QueryProjection
    public BookReviewSummaryStatDto(Long reviewCount, Double averageRating, Integer rating1Count, Integer rating2Count, Integer rating3Count, Integer rating4Count, Integer rating5Count, OffsetDateTime lastReviewedAt) {
        this.reviewCount = reviewCount;
        this.averageRating = averageRating;
        this.rating1Count = rating1Count;
        this.rating2Count = rating2Count;
        this.rating3Count = rating3Count;
        this.rating4Count = rating4Count;
        this.rating5Count = rating5Count;
        this.lastReviewedAt = lastReviewedAt;
    }
}
