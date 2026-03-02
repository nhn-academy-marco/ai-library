package com.nhnacademy.library.ai.function.dto;

import java.util.List;

/**
 * AI Function 호출용 도서 검색 결과 DTO
 *
 * @param count 검색된 도서 수
 * @param books 도서 목록
 */
public record BookSearchResult(
    int count,
    List<BookItem> books
) {
    /**
     * AI Function 호출용 도서 항목 DTO
     *
     * @param id         도서 ID
     * @param title      제목
     * @param author     저자명
     * @param publisher  출판사
     * @param isbn       ISBN
     */
    public record BookItem(
        long id,
        String title,
        String author,
        String publisher,
        String isbn
    ) {
        public static BookItem from(long id, String title, String author, String publisher, String isbn) {
            return new BookItem(id, title, author, publisher, isbn);
        }
    }

    /**
     * 빈 결과를 생성합니다.
     *
     * @return BookSearchResult
     */
    public static BookSearchResult empty() {
        return new BookSearchResult(0, List.of());
    }

    /**
     * 결과를 생성합니다.
     *
     * @param books 도서 목록
     * @return BookSearchResult
     */
    public static BookSearchResult from(List<BookItem> books) {
        return new BookSearchResult(books.size(), books);
    }
}
