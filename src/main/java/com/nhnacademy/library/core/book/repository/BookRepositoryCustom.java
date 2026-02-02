package com.nhnacademy.library.core.book.repository;

import com.nhnacademy.library.core.book.dto.BookSearchRequest;
import com.nhnacademy.library.core.book.dto.BookSearchResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.NoRepositoryBean;


import java.util.List;

@NoRepositoryBean
public interface BookRepositoryCustom {
    List<BookSearchResponse> search(Pageable pageable, BookSearchRequest request);
}
