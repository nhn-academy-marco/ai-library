package com.nhnacademy.library.external.opennaru.dto;

import lombok.Builder;

/**
 * 인기대출도서 정보 DTO
 *
 * loanItemSrch, loanItemSrchByLib API 응답
 */
@Builder
public record PopularBookInfo(
    String isbn13,             // ISBN13
    String bookName,           // 도서명
    String authors,            // 저자
    String publisher,          // 출판사
    String publicationYear,    // 발행년도
    String imageUrl,           // 표지 이미지 URL
    int loanCount,             // 대출 횟수
    String ranking             // 순위
) {}
