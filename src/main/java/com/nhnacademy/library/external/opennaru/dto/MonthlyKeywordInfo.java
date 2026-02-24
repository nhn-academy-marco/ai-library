package com.nhnacademy.library.external.opennaru.dto;

import lombok.Builder;

/**
 * 월간 키워드 정보 DTO
 *
 * monthlyKeywords API 응답
 */
@Builder
public record MonthlyKeywordInfo(
    String month,              // 월 (YYYY-MM)
    String keyword,            // 키워드
    int rank,                  // 순위
    int score                  // 점수
) {}
