package net.java21.library.core.book.dto;

/**
 * 표지 보강 대상 도서
 *
 * <p>화면에 노출된 도서의 식별자와 현재 표지 URL을 담습니다.
 * 검색 결과({@link BookSearchResponse})와 상세 조회({@link BookViewResponse}) 응답에서 만듭니다.</p>
 *
 * @param bookId   도서 ID
 * @param isbn     ISBN13
 * @param imageUrl 현재 표지 이미지 URL (비어 있으면 보강 대상)
 */
public record BookCoverTarget(Long bookId, String isbn, String imageUrl) {

    public static BookCoverTarget from(BookSearchResponse book) {
        return new BookCoverTarget(book.getId(), book.getIsbn(), book.getImageUrl());
    }

    public static BookCoverTarget from(BookViewResponse book) {
        return new BookCoverTarget(book.id(), book.isbn(), book.imageUrl());
    }

    /**
     * 표지가 비어 있고 조회에 필요한 값(ID, ISBN)이 있는지 확인합니다.
     */
    public boolean isMissingCover() {
        return bookId != null
                && isbn != null && !isbn.isBlank()
                && (imageUrl == null || imageUrl.isBlank());
    }
}
