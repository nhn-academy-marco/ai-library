package com.nhnacademy.library.external.opennaru.dto;

import lombok.Builder;

import java.util.List;

/**
 * 도서관정보나루 도서 상세 정보 DTO
 *
 * srchDtlList API 응답
 */
@Builder
public record BookDetailInfo(
    String isbn13,             // ISBN13
    String bookname,           // 도서명
    String authors,            // 저자
    String publisher,          // 출판사
    String publicationDate,    // 발행일
    String imageUrl,           // 표지 이미지 URL
    String description,        // 책 소개
    String category,           // KDC 분류
    int loanCount,             // 대출 횟수
    LoanInfo loanInfo,         // 대출 정보
    List<KeywordInfo> keywords // 키워드 목록
) {
    @Builder
    public record LoanInfo(
        int totalLoanCount,        // 전체 대출 횟수
        int maleLoanCount,         // 남성 대출 횟수
        int femaleLoanCount,       // 여성 대출 횟수
        int teensLoanCount,        // 10대 대출 횟수
        int twentiesLoanCount,     // 20대 대출 횟수
        int thirtiesLoanCount,     // 30대 대출 횟수
        int fortiesLoanCount,      // 40대 대출 횟수
        int fiftiesLoanCount,      // 50대 대출 횟수
        int sixtiesLoanCount       // 60대 이상 대출 횟수
    ) {}

    @Builder
    public record KeywordInfo(
        String keyword,        // 키워드
        int rank              // 순위
    ) {}
}
