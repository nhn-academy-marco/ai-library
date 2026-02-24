package com.nhnacademy.library.external.opennaru.dto;

import lombok.Builder;

/**
 * 대출 가능 도서 정보 DTO
 */
@Builder
public record LoanItemInfo(
    String isbn13,              // ISBN13
    String bookName,            // 도서명
    String authors,             // 저자
    String publisher,           // 출판사
    String libraryName,         // 도서관명
    String region,              // 지역
    String loanAvailable,       // 대출 가능 여부 (Y/N)
    String returnDueDate,       // 반납 예정일
    String callNumber,          // 청구기호
    String location             // 소장 위치
) {}
