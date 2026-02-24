package com.nhnacademy.library.external.opennaru.dto;

import lombok.Builder;

import java.util.List;

/**
 * 도서 키워드 정보 DTO
 *
 * keywordList API 응답
 */
@Builder
public record BookKeywordInfo(
    String isbn13,             // ISBN13
    String bookName,           // 도서명
    List<Keyword> keywords     // 키워드 목록
) {
    @Builder
    public record Keyword(
        String word,           // 키워드
        int rank,              // 순위
        int score              // 점수
    ) {}
}
