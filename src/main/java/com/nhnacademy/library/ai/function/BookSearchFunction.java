package com.nhnacademy.library.ai.function;

import com.nhnacademy.library.ai.function.dto.BookSearchRequest;
import com.nhnacademy.library.ai.function.dto.BookSearchResult;
import com.nhnacademy.library.core.book.domain.SearchType;
import com.nhnacademy.library.core.book.dto.BookSearchResponse;
import com.nhnacademy.library.core.book.service.search.BookSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * 도서 검색 Function (Spring AI 1.1.2 FunctionToolCallback 방식)
 * <p>
 * AI Agent가 도서 검색을 수행할 때 호출하는 함수입니다.
 * 내부 DB를 검색하여 도서 목록을 반환합니다.
 * </p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BookSearchFunction {

    private final BookSearchService bookSearchService;

    private static final int DEFAULT_PAGE_SIZE = 10;

    /**
     * 도서 검색 실행 메서드 (외부에서 호출)
     */
    public BookSearchResult searchBooks(BookSearchRequest request) {
        log.info("[AI Function] 도서 검색 시작: query={}", request.query());

        try {
            // 1. 내부 DB 검색 실행 (하이브리드 검색)
            var coreRequest = new com.nhnacademy.library.core.book.dto.BookSearchRequest(
                request.query(),
                null,
                SearchType.HYBRID,
                null,
                false
            );

            var result = bookSearchService.searchBooks(
                PageRequest.of(0, request.limit()),
                coreRequest
            );

            // 2. 결과 변환
            var bookItems = result.getBooks().getContent().stream()
                .map(this::toBookItem)
                .toList();

            log.info("[AI Function] 검색 완료: {}권", bookItems.size());

            // 3. 결과 반환
            return BookSearchResult.from(bookItems);

        } catch (Exception e) {
            log.error("[AI Function] 검색 실패: query={}", request.query(), e);
            return BookSearchResult.empty();
        }
    }

    /**
     * BookSearchResponse를 BookSearchResult.BookItem으로 변환합니다.
     *
     * @param book BookSearchResponse
     * @return BookSearchResult.BookItem
     */
    private BookSearchResult.BookItem toBookItem(BookSearchResponse book) {
        return BookSearchResult.BookItem.from(
            book.getId(),
            book.getTitle(),
            book.getAuthorName(),
            book.getPublisherName(),
            book.getIsbn()
        );
    }
}
