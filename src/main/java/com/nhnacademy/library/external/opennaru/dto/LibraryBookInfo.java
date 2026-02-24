package com.nhnacademy.library.external.opennaru.dto;

import lombok.Builder;

import java.time.LocalDate;

/**
 * 도서관정보나루 API 응답 DTO
 *
 * 도서관정보나루 API에서 반환하는 도서 정보를 담습니다.
 */
@Builder
public record LibraryBookInfo(
    String title,              // 도서명
    String authors,            // 저자
    String publisher,          // 출판사
    String isbn13,             // ISBN13
    boolean loanAvailable,     // 대출 가능 여부
    int loanCount,             // 대출 횟수
    String libraryName,        // 도서관명
    LocalDate publicationDate  // 발행일
) {}
