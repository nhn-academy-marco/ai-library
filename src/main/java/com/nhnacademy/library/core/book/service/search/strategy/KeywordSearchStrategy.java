package com.nhnacademy.library.core.book.service.search.strategy;
import com.nhnacademy.library.core.book.domain.Book;
import com.nhnacademy.library.core.book.dto.BookSearchRequest;
import com.nhnacademy.library.core.book.dto.BookSearchResponse;
import com.nhnacademy.library.core.book.dto.BookSearchResult;
import com.nhnacademy.library.core.book.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 키워드 기반 검색 전략
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KeywordSearchStrategy implements SearchStrategy {

    private final BookRepository bookRepository;

    @Override
    public BookSearchResult search(Pageable pageable, BookSearchRequest request) {
        log.info("[KEYWORD_STRATEGY] Request: keyword={}, isbn={}", request.keyword(), request.isbn());
        
        if (!StringUtils.hasText(request.keyword()) && !StringUtils.hasText(request.isbn())) {
            log.info("[KEYWORD_STRATEGY] Both keyword and isbn are empty. Returning all books.");
            Page<Book> allBooks = bookRepository.findAll(pageable);
            Page<BookSearchResponse> results = allBooks.map(BookSearchResponse::from);
            return BookSearchResult.builder()
                    .books(results)
                    .build();
        }

        log.info("[KEYWORD_STRATEGY] Delegating to repository search.");
        Page<BookSearchResponse> results = bookRepository.search(pageable, request);
        log.info("[KEYWORD_STRATEGY] Found {} results.", results.getTotalElements());
        
        return BookSearchResult.builder()
                .books(results)
                .build();
    }
}
