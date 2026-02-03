package com.nhnacademy.library.core.book.service;

import com.nhnacademy.library.core.book.domain.Book;
import com.nhnacademy.library.core.book.dto.BookSearchRequest;
import com.nhnacademy.library.core.book.dto.BookSearchResponse;
import com.nhnacademy.library.core.book.repository.BookRepository;
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

@SpringBootTest
@Transactional
class BookHybridSearchTest {

    @Autowired
    private BookSearchService bookSearchService;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private EmbeddingService embeddingService;

    @Test
    @DisplayName("하이브리드 검색(Keyword + Vector) 기능 테스트")
    void testHybridSearch() {
        // Given: 1. 키워드에 강점이 있는 책
        Book keywordBook = new Book(
                "9990000000001",
                "스프링 프로그래밍",
                "Spring Boot 실전 가이드",
                "김철수",
                "자바출판사",
                LocalDate.now(),
                new BigDecimal("30000"),
                "",
                "스프링 부트와 JPA를 활용한 웹 애플리케이션 개발",
                "실무 중심",
                LocalDate.now()
        );
        float[] keywordEmbedding = embeddingService.getEmbedding("Spring Boot 실전 가이드");
        keywordBook.updateEmbedding(keywordEmbedding);
        bookRepository.save(keywordBook);

        // 2. 의미상 유사한 책 (키워드는 겹치지 않음)
        Book semanticBook = new Book(
                "9990000000002",
                "백엔드 마스터",
                "서버 프로그래밍의 정석",
                "이영희",
                "기술출판사",
                LocalDate.now(),
                new BigDecimal("25000"),
                "",
                "안정적인 서버를 구축하기 위한 설계 기법과 자바 프레임워크 활용법",
                "입문용",
                LocalDate.now()
        );
        float[] semanticEmbedding = embeddingService.getEmbedding("서버 프로그래밍의 정석");
        semanticBook.updateEmbedding(semanticEmbedding);
        bookRepository.save(semanticBook);
        
        bookRepository.flush();

        // When: "스프링 서버"로 검색 (키워드는 첫 번째 책에 가깝고, 의미는 둘 다 포함될 수 있음)
        String keyword = "스프링 서버";
        BookSearchRequest request = new BookSearchRequest(keyword, null, "hybrid", null);
        PageRequest pageable = PageRequest.of(0, 10);
        
        BookSearchService.SearchResult searchResult = bookSearchService.searchBooks(pageable, request);
        Page<BookSearchResponse> results = searchResult.getBooks();

        // Then
        assertThat(results).isNotNull();
        System.out.println("[DEBUG_LOG] Hybrid results size: " + results.getContent().size());
        
        results.getContent().forEach(b -> {
            System.out.println("[DEBUG_LOG] Title: " + b.getTitle() + ", Similarity: " + b.getSimilarity() + ", RRF Score: " + b.getRrfScore());
        });

        assertThat(results.getContent()).isNotEmpty();
        // RRF 점수가 포함되어 있는지 확인
        assertThat(results.getContent().get(0).getRrfScore()).isNotNull();
        assertThat(results.getContent().get(0).getRrfScore()).isGreaterThan(0.0);
        // 적어도 하나는 우리가 저장한 책이어야 함 (다른 책이 있을 수도 있으니)
        boolean found = results.getContent().stream()
                .anyMatch(b -> b.getTitle().contains("Spring Boot") || b.getTitle().contains("서버"));
        assertThat(found).isTrue();
    }
}
