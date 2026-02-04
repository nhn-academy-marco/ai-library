package com.nhnacademy.library.core.book.service;

import com.nhnacademy.library.core.book.domain.Book;
import com.nhnacademy.library.core.book.dto.BookSearchRequest;
import com.nhnacademy.library.core.book.dto.BookSearchResponse;
import com.nhnacademy.library.core.book.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
@Transactional
class BookSearchCachingTest {

    @Autowired
    private BookSearchService bookSearchService;

    @MockBean
    private BookAiService bookAiService;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private EmbeddingService embeddingService;

    @BeforeEach
    void setUp() {
        // 임계값을 넘기기 위해 실제 DB에 책 데이터를 저장
        String title = "캐싱테스트 도서";
        Book book = new Book(
                "9999999999999", null, title, "저자", "출판사",
                LocalDate.now(), new BigDecimal("10000"), "", "캐싱테스트 내용", null, LocalDate.now()
        );
        // 하이브리드 검색 시 RRF 점수가 생성되도록 임베딩 설정
        book.updateEmbedding(embeddingService.getEmbedding(title));
        bookRepository.save(book);
    }

    @Test
    @DisplayName("동일한 RAG 검색 요청 시 두 번째 호출은 캐시된 결과를 반환해야 한다")
    void testRagSearchCaching() {
        // Given
        String keyword = "캐싱테스트 도서";
        BookSearchRequest request = new BookSearchRequest(keyword, null, "rag", null);
        PageRequest pageable = PageRequest.of(0, 10);

        // AI 서비스가 JSON 형태의 응답을 반환하도록 설정
        when(bookAiService.askAboutBooks(anyString())).thenReturn("[{\"id\": 1, \"relevance\": 90, \"why\": \"테스트\"}]");

        // When
        // 0. 백그라운드 캐시 워밍 (테스트에서는 동기 실행) - 첫 AI 호출이 여기서 발생
        BookSearchCacheService cacheService = new BookSearchCacheService(applicationContext, cacheManager);
        cacheService.warmUpRagCache(keyword);

        // 1. 첫 번째 호출 (이미 캐시되어 있어 AI 서비스 호출되지 않음)
        bookSearchService.searchBooks(pageable, request);
        
        // 2. 두 번째 호출 (캐시 히트, AI 서비스 호출되지 않음)
        bookSearchService.searchBooks(pageable, request);

        // Then
        // AI 서비스는 단 한 번만 호출되어야 함
        verify(bookAiService, times(1)).askAboutBooks(anyString());
        
        // 캐시에 데이터가 존재하는지 확인
        assertThat(Objects.requireNonNull(cacheManager.getCache("bookSearchCache")).get(request)).isNotNull();
    }
}
