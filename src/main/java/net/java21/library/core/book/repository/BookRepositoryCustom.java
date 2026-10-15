package net.java21.library.core.book.repository;

import net.java21.library.core.book.domain.BrowseSort;
import net.java21.library.core.book.dto.BookSearchRequest;
import net.java21.library.core.book.dto.BookSearchResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface BookRepositoryCustom {
    Page<BookSearchResponse> search(Pageable pageable, BookSearchRequest request);
    Page<BookSearchResponse> vectorSearch(Pageable pageable, BookSearchRequest request);

    /**
     * 검색어 없이 정해진 기준으로 도서를 둘러봅니다. 순서가 고정되어 페이지를 오가도 결과가 바뀌지 않습니다.
     */
    Page<BookSearchResponse> browse(Pageable pageable, BrowseSort sort);
}
