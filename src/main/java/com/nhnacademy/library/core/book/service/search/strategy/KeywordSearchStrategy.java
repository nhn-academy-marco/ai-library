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
 * 키워드 기반 검색 전략 구현체입니다.
 * 도서 제목, 저자명, ISBN 등 텍스트 기반의 전통적인 검색 기능을 제공합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KeywordSearchStrategy implements SearchStrategy {

    private final BookRepository bookRepository;

    /**
     * 키워드 또는 ISBN을 기반으로 도서를 검색합니다.
     * 1. 요청 검증: 키워드와 ISBN이 모두 없는 경우 전체 도서 목록을 반환합니다.
     * 2. 검색 수행: 도메인 저장소(Repository)에 검색을 위임하여 결과를 가져옵니다.
     *
     * @param pageable 페이징 정보
     * @param request  검색 요청 DTO (키워드, ISBN 등 포함)
     * @return 검색된 도서 목록을 포함한 BookSearchResult
     */
    @Override
    public BookSearchResult search(Pageable pageable, BookSearchRequest request) {
        log.info("[KEYWORD_STRATEGY] Request: keyword={}, isbn={}", request.keyword(), request.isbn());
        
        // 검색 조건이 없는 경우 전체 도서 조회
        if (!StringUtils.hasText(request.keyword()) && !StringUtils.hasText(request.isbn())) {
            log.info("[KEYWORD_STRATEGY] Both keyword and isbn are empty. Returning all books.");
            Page<Book> allBooks = bookRepository.findAll(pageable);
            Page<BookSearchResponse> results = allBooks.map(BookSearchResponse::from);
            return BookSearchResult.builder()
                    .books(results)
                    .build();
        }

        // 도메인 저장소 검색 위임
        log.info("[KEYWORD_STRATEGY] Delegating to repository search.");
        Page<BookSearchResponse> results = bookRepository.search(pageable, request);
        log.info("[KEYWORD_STRATEGY] Found {} results.", results.getTotalElements());
        
        return BookSearchResult.builder()
                .books(results)
                .build();
    }
}
