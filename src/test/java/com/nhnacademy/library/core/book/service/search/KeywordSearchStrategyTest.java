package com.nhnacademy.library.core.book.service.search;

import com.nhnacademy.library.core.book.domain.Book;
import com.nhnacademy.library.core.book.domain.SearchType;
import com.nhnacademy.library.core.book.dto.BookSearchRequest;
import com.nhnacademy.library.core.book.dto.BookSearchResult;
import com.nhnacademy.library.core.book.repository.BookRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class KeywordSearchStrategyTest {

    @Autowired
    private BookSearchService bookSearchService;

    @Autowired
    private BookRepository bookRepository;

    @Test
    @DisplayName("Initial page search with empty request should return all books")
    void testInitialSearchReturnsAllBooks() {
        // Given: Save some books
        bookRepository.save(new Book("1234567890123", "Volume 1", "Test Book 1", "Author 1", "Pub 1", LocalDate.now(), BigDecimal.TEN, null, "Content 1", null, LocalDate.now()));
        bookRepository.save(new Book("1234567890124", "Volume 2", "Test Book 2", "Author 2", "Pub 2", LocalDate.now(), BigDecimal.TEN, null, "Content 2", null, LocalDate.now()));
        bookRepository.flush();

        // When: Search with empty keyword and KEYWORD type
        BookSearchRequest request = new BookSearchRequest(null, null, SearchType.KEYWORD, null);
        BookSearchResult result = bookSearchService.searchBooks(PageRequest.of(0, 10), request);

        // Then: Should return books even if keyword is empty
        assertThat(result.getBooks().getContent()).isNotEmpty();
        assertThat(result.getBooks().getTotalElements()).isGreaterThanOrEqualTo(2);
    }
}
