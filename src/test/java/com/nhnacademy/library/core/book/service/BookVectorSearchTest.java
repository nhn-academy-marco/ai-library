package com.nhnacademy.library.core.book.service;

import com.nhnacademy.library.core.book.domain.Book;
import com.nhnacademy.library.core.book.dto.BookSearchRequest;
import com.nhnacademy.library.core.book.dto.BookSearchResponse;
import com.nhnacademy.library.core.book.repository.BookRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
@Transactional
class BookVectorSearchTest {

    @Autowired
    private BookSearchService bookSearchService;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private EmbeddingService embeddingService;

    @Test
    @DisplayName("실제 데이터를 이용한 의미 검색(Vector Search) 기능 테스트")
    void testVectorSearch() {
        // Given: 테스트용 데이터 저장 및 임베딩 업데이트
        Book book = new Book(
                "1234567890123",
                "테스트 도서",
                "마음이 편안해지는 명상 가이드",
                "홍길동",
                "테스트출판사",
                LocalDate.now(),
                new BigDecimal("15000"),
                "",
                "이 책은 마음을 편안하게 해주는 명상 방법을 소개합니다.",
                "부제",
                LocalDate.now()
        );
        float[] embedding = embeddingService.getEmbedding(book.getTitle() + " " + book.getBookContent());
        book.updateEmbedding(embedding);
        bookRepository.saveAndFlush(book);

        String keyword = "마음이 편안해지는 책";
        BookSearchRequest request = new BookSearchRequest(keyword, null, "vector", null);
        PageRequest pageable = PageRequest.of(0, 5);

        // When
        BookSearchService.SearchResult searchResult = bookSearchService.searchBooks(pageable, request);
        Page<BookSearchResponse> results = searchResult.getBooks();

        // Then
        assertThat(results).isNotNull();
        log.debug("Results size: {}", results.getContent().size());

        assertThat(results.getContent()).isNotEmpty();
        BookSearchResponse firstBook = results.getContent().get(0);
        log.debug("First book title: {}", firstBook.getTitle());
        log.debug("First book similarity: {}", firstBook.getSimilarity());

        assertThat(firstBook.getSimilarity()).isNotNull();
        assertThat(firstBook.getSimilarity()).isGreaterThan(0.0);
    }
}
