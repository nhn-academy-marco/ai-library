package com.nhnacademy.library.core.book.service.search.strategy;

import com.nhnacademy.library.core.book.dto.BookSearchRequest;
import com.nhnacademy.library.core.book.dto.BookSearchResponse;
import com.nhnacademy.library.core.book.dto.BookSearchResult;
import com.nhnacademy.library.core.book.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * 벡터 기반 검색 전략
 */
@Component
@RequiredArgsConstructor
public class VectorSearchStrategy implements SearchStrategy {

    private final BookRepository bookRepository;

    @Override
    public BookSearchResult search(Pageable pageable, BookSearchRequest request) {
        Page<BookSearchResponse> results = bookRepository.vectorSearch(pageable, request);
        return BookSearchResult.builder()
                .books(results)
                .build();
    }
}
