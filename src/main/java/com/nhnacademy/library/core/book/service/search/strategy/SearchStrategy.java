package com.nhnacademy.library.core.book.service.search.strategy;

import com.nhnacademy.library.core.book.dto.BookSearchRequest;
import com.nhnacademy.library.core.book.dto.BookSearchResult;
import org.springframework.data.domain.Pageable;

/**
 * 도서 검색 전략 인터페이스
 */
public interface SearchStrategy {
    /**
     * 검색을 수행합니다.
     */
    BookSearchResult search(Pageable pageable, BookSearchRequest request);
}
