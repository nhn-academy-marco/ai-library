package com.nhnacademy.library.external.opennaru.dto;

import lombok.Builder;

import java.util.List;

/**
 * 도서관별 연령대별 인기대출도서 통합 정보 DTO
 *
 * extends/loanItemSrchByLib API 응답
 */
@Builder
public record ExtendedPopularBooksInfo(
    List<PopularBookInfo> loanBooks,      // 전체 인기대출도서
    List<PopularBookInfo> age0Books,      // 0-5세
    List<PopularBookInfo> age6Books,      // 6-7세
    List<PopularBookInfo> age8Books,      // 8-13세
    List<PopularBookInfo> age14Books,     // 14-19세
    List<PopularBookInfo> age20Books      // 20세 이상
) {}
