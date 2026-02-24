package com.nhnacademy.library.external.opennaru.dto;

import lombok.Builder;

/**
 * 도서관정보나루 추천도서 DTO
 *
 * recommandList API 응답
 */
@Builder
public record RecommendedBookInfo(
    String isbn13,             // ISBN13
    String bookName,           // 도서명
    String authors,            // 저자
    String publisher,          // 출판사
    String imageUrl,           // 표지 이미지 URL
    String description,        // 책 소개
    String recommendationReason, // 추천 사유
    int rank                  // 추천 순위
) {}
