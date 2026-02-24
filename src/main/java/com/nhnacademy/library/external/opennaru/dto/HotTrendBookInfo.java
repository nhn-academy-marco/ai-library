package com.nhnacademy.library.external.opennaru.dto;

import lombok.Builder;

/**
 * 대출 급상승 도서 정보 DTO
 *
 * hotTrend API 응답
 */
@Builder
public record HotTrendBookInfo(
    String isbn13,             // ISBN13
    String bookName,           // 도서명
    String authors,            // 저자
    String publisher,          // 출판사
    String imageUrl,           // 표지 이미지 URL
    int currentRank,           // 현재 순위
    int previousRank,          // 이전 순위
    int rankChange,            // 순위 변화
    String searchDate          // 검색일자
) {}
