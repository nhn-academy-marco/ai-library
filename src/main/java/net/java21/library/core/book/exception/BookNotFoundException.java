package net.java21.library.core.book.exception;

/**
 * 도서를 찾을 수 없을 때 발생하는 예외
 */
public class BookNotFoundException extends RuntimeException {
    public BookNotFoundException(Long id) {
        super("Book not found with id: " + id);
    }
}