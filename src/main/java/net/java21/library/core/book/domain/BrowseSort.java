package net.java21.library.core.book.domain;

/**
 * 검색어 없이 도서를 둘러볼 때의 정렬 기준
 *
 * <p>검색 전략(키워드·벡터·하이브리드·RAG)은 관련도 순위가 필요하므로 검색어가 있을 때만 사용합니다.
 * 첫 화면처럼 검색어가 없을 때는 순서가 고정된 둘러보기 목록을 보여 줍니다.</p>
 */
public enum BrowseSort {

    /** 최근 출간 순 (출간일 없는 도서는 뒤로) */
    RECENT("최근 출간"),

    /** 리뷰가 많은 순 (리뷰가 있는 도서만) */
    POPULAR("리뷰 많은 순");

    private final String label;

    BrowseSort(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * 요청 파라미터를 정렬 기준으로 바꿉니다. 비어 있거나 알 수 없는 값이면 {@link #RECENT}를 씁니다.
     *
     * @param value 요청 파라미터 값 (대소문자 무시)
     * @return 정렬 기준
     */
    public static BrowseSort from(String value) {
        if (value == null || value.isBlank()) {
            return RECENT;
        }
        for (BrowseSort sort : values()) {
            if (sort.name().equalsIgnoreCase(value.strip())) {
                return sort;
            }
        }
        return RECENT;
    }
}
