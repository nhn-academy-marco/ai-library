package com.nhnacademy.library.external.opennaru.dto;

import lombok.Builder;

/**
 * 도서 소장여부 정보 DTO
 *
 * bookExist API 응답
 */
@Builder
public record BookExistInfo(
    String libCode,            // 도서관 코드
    String libName,            // 도서관명
    String isbn13,             // ISBN13
    boolean exists,            // 소장 여부
    String loanAvailable,      // 대출 가능 여부 (Y/N)
    String callNumber,         // 청구기호
    String location            // 소장 위치
) {}
