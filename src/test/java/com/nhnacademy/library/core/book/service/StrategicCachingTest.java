package com.nhnacademy.library.core.book.service;

import com.nhnacademy.library.core.book.dto.BookSearchRequest;
import com.nhnacademy.library.core.book.dto.BookSearchResponse;
import com.nhnacademy.library.core.book.repository.BookRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@Slf4j
@SpringBootTest
class StrategicCachingTest {

    @Autowired
    private BookSearchService bookSearchService;

    @Autowired
    private CacheManager cacheManager;

    @MockBean
    private BookAiService bookAiService;

    @MockBean
    private EmbeddingService embeddingService;

    @MockBean
    private BookRepository bookRepository;

    @Test
    @DisplayName("하이브리드 검색 시 백그라운드에서 RAG 결과가 캐싱되어야 한다 (미션 2)")
    void testStrategicCaching() throws InterruptedException {
        // Given
        String keyword = "스프링"; // 실제 데이터가 있을만한 키워드로 변경
        BookSearchRequest hybridRequest = new BookSearchRequest(keyword, null, "hybrid", null);
        BookSearchRequest ragRequest = new BookSearchRequest(keyword, null, "rag", null);
        PageRequest pageable = PageRequest.of(0, 24);

        // 캐시 초기화
        cacheManager.getCache("bookSearchCache").clear();

        // Mock 설정
        when(embeddingService.getEmbedding(anyString())).thenReturn(new float[]{0.1f, 0.2f});
        
        BookSearchResponse mockResponse = new BookSearchResponse(1L, "123456", "테스트 도서", null, "저자", "출판사", 
                BigDecimal.valueOf(10000), LocalDate.now(), null, "테스트 내용입니다.");
        when(bookRepository.search(any(), any())).thenReturn(new PageImpl<>(List.of(mockResponse)));
        when(bookRepository.vectorSearch(any(), any())).thenReturn(new PageImpl<>(List.of(mockResponse)));

        // AI 응답 설정
        when(bookAiService.askAboutBooks(anyString())).thenReturn("[{\"id\":1, \"relevance\":100, \"why\":\"테스트\"}]");

        // When: 하이브리드 검색 수행
        bookSearchService.searchBooks(pageable, hybridRequest);

        // Then: 비동기 작업이 완료될 때까지 잠시 대기 (최대 5초)
        boolean cached = false;
        BookSearchRequest expectedCacheRequest = new BookSearchRequest(keyword, null, "rag", new float[]{0.1f, 0.2f});
        for (int i = 0; i < 50; i++) {
            if (cacheManager.getCache("bookSearchCache").get(expectedCacheRequest) != null) {
                cached = true;
                break;
            }
            Thread.sleep(100);
        }

        assertThat(cached).as("RAG 결과가 캐시에 생성되어야 함").isTrue();
        
        // 다시 RAG 검색을 수행했을 때 AI 서비스가 호출되지 않아야 함 (이미 캐시되었으므로)
        // 주의: 첫 번째 warmUp 시점에 이미 호출되었으므로 verify 횟수 체크
        verify(bookAiService, times(1)).askAboutBooks(anyString());
        
        bookSearchService.searchBooks(pageable, ragRequest);
        
        // 추가 호출이 없어야 함
        verify(bookAiService, times(1)).askAboutBooks(anyString());
        
        log.info("[STRATEGIC_CACHE_TEST] Strategic caching verified successfully.");
    }
}
