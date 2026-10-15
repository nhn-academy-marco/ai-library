package net.java21.library.core.book.repository;

import net.java21.library.core.book.dto.BookSearchRequest;
import net.java21.library.core.book.dto.BookSearchResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface BookRepositoryCustom {
    Page<BookSearchResponse> search(Pageable pageable, BookSearchRequest request);
    Page<BookSearchResponse> vectorSearch(Pageable pageable, BookSearchRequest request);
}
