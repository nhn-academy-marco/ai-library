package com.nhnacademy.library.ai.agent.dto;

import java.util.List;

/**
 * 사용자 쿼리 파싱 결과
 * <p>
 * LLM 기반 쿼리 분석 결과를 구조화한 Record입니다.
 * 도서명, 지역명, 검색 의도 등을 포함합니다.
 * </p>
 *
 * @param bookName 도서명 (없으면 null)
 * @param authorName 저자명 (없으면 null)
 * @param regionName 지역명 (예: "서울", "광주", "부산" - 없으면 null)
 * @param intent 검색 의도 (SEARCH, CHECK_AVAILABILITY, RECOMMEND)
 * @param keywords 추가 키워드 목록
 * @param originalQuery 원본 사용자 쿼리
 */
public record ParsedQuery(
    String bookName,
    String authorName,
    String regionName,
    SearchIntent intent,
    List<String> keywords,
    String originalQuery
) {
    /**
     * 검색 의도 enum
     */
    public enum SearchIntent {
        /** 도서 검색 */
        SEARCH("도서 검색"),

        /** 대출 가능 여부 확인 */
        CHECK_AVAILABILITY("대출 가능 여부 확인"),

        /** 도서 추천 */
        RECOMMEND("도서 추천"),

        /** 도서관 검색 */
        LIBRARY_SEARCH("도서관 검색"),

        /** 알 수 없음 */
        UNKNOWN("알 수 없음");

        private final String description;

        SearchIntent(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 파싱 결과가 유효한지 확인합니다.
     */
    public boolean isValid() {
        return intent != SearchIntent.UNKNOWN;
    }

    /**
     * 도서명이 있는지 확인합니다.
     */
    public boolean hasBookName() {
        return bookName != null && !bookName.isBlank();
    }

    /**
     * 지역명이 있는지 확인합니다.
     */
    public boolean hasRegionName() {
        return regionName != null && !regionName.isBlank();
    }

    /**
     * 디버깅용 문자열 반환
     */
    public String toDebugString() {
        return String.format(
            "ParsedQuery{bookName='%s', authorName='%s', regionName='%s', intent=%s, keywords=%s}",
            bookName, authorName, regionName, intent, keywords
        );
    }
}
