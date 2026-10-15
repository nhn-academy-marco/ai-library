package net.java21.library.core.book.exception;

/**
 * 하이브리드 검색의 후보 조회가 제한 시간 안에 끝나지 않았을 때 발생하는 예외
 */
public class SearchTimeoutException extends RuntimeException {
    public SearchTimeoutException(String keyword, long timeoutMillis) {
        super("Hybrid search timed out after " + timeoutMillis + "ms for keyword: " + keyword);
    }
}
