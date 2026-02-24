package com.nhnacademy.library.core.book.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * AI 도서 추천 응답 항목 DTO
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
}
