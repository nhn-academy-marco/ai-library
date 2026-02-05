package com.nhnacademy.library.core.book.service.cache;
import com.nhnacademy.library.core.book.domain.SearchType;
import com.nhnacademy.library.core.book.service.search.BookSearchService;
import org.springframework.ai.chat.model.ChatModel;
import com.nhnacademy.library.core.book.service.embedding.EmbeddingService;
import com.nhnacademy.library.core.book.service.embedding.BookEmbeddingService;
import com.nhnacademy.library.core.review.service.ReviewSummarizer;

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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@Slf4j
@SpringBootTest(properties = "cache.ttl.minutes=1") // 테스트를 위해 짧은 TTL 설정
class SemanticCachingTest {

    @Autowired
    private BookSearchService bookSearchService;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private org.springframework.context.ApplicationContext applicationContext;

    @MockBean
    private ChatModel chatModel;

    @MockBean
    private EmbeddingService embeddingService;

    @MockBean
    private BookRepository bookRepository;

    @Test
    @DisplayName("유사한 키워드로 검색 시 의미적 캐싱(Semantic Caching)이 작동해야 한다")
    void testSemanticCaching() throws InterruptedException {
        // Given
        String keyword1 = "자바 프로그래밍 기초";
        String keyword2 = "자바 프로그래밍 기본"; // keyword1과 유사하다고 가정
        String keyword3 = "파이썬 데이터 분석";   // keyword1과 상이하다고 가정

        float[] vector1 = {0.1f, 0.2f, 0.3f};
        float[] vector2 = {0.11f, 0.21f, 0.31f}; // vector1과 매우 유사
        float[] vector3 = {0.9f, 0.1f, 0.1f};    // vector1과 상이 (0.9, 0.8, 0.7 에서 변경)

        BookSearchRequest request1 = new BookSearchRequest(keyword1, null, SearchType.RAG, null);
        BookSearchRequest request2 = new BookSearchRequest(keyword2, null, SearchType.RAG, null);
        BookSearchRequest request3 = new BookSearchRequest(keyword3, null, SearchType.RAG, null);
        PageRequest pageable = PageRequest.of(0, 10);

        // 캐시 초기화
        cacheManager.getCache("bookSearchCache").clear();

        // Mock 설정
        when(embeddingService.getEmbedding(keyword1)).thenReturn(vector1);
        when(embeddingService.getEmbedding(keyword2)).thenReturn(vector2);
        when(embeddingService.getEmbedding(keyword3)).thenReturn(vector3);

        BookSearchResponse mockResponse = new BookSearchResponse(1L, "123456", "테스트 도서", null, "저자", "출판사", 
                BigDecimal.valueOf(10000), LocalDate.now(), null, "테스트 내용입니다.");
        when(bookRepository.search(any(), any())).thenReturn(new PageImpl<>(List.of(mockResponse)));
        when(bookRepository.vectorSearch(any(), any())).thenReturn(new PageImpl<>(List.of(mockResponse)));
        when(chatModel.call(anyString())).thenReturn("[{\"id\":1, \"relevance\":100, \"why\":\"테스트\"}]");

        // When 1: 첫 번째 키워드로 검색 (캐시 생성)
        log.info("Searching with keyword1: {}", keyword1);
        bookSearchService.searchBooks(pageable, request1);
        
        // Then: 비동기 warm-up이 완료될 때까지 대기
        boolean cached = false;
        // warm-up 과정에서 저장되는 캐시 키는 isWarmUp=true 상태임
        BookSearchRequest expectedCacheKey1 = new BookSearchRequest(keyword1, null, SearchType.RAG, vector1, true);
        for (int i = 0; i < 50; i++) {
            if (cacheManager.getCache("bookSearchCache").get(expectedCacheKey1) != null) {
                cached = true;
                break;
            }
            Thread.sleep(100);
        }
        assertThat(cached).isTrue();
        verify(chatModel, times(1)).call(anyString());

        // When 2: 유사한 두 번째 키워드로 검색 (캐시 히트 기대)
        log.info("Searching with keyword2 (similar to keyword1): {}", keyword2);
        bookSearchService.searchBooks(pageable, request2);
        
        // Then: AI 서비스가 추가로 호출되지 않아야 함
        verify(chatModel, times(1)).call(anyString());
        log.info("[TEST] Semantic cache hit verified for similar keyword.");

        // When 3: 상이한 세 번째 키워드로 검색 (캐시 미스 기대)
        log.info("Searching with keyword3 (different from keyword1): {}", keyword3);
        bookSearchService.searchBooks(pageable, request3);
        
        // Then: 비동기 warm-up이 완료될 때까지 대기
        BookSearchRequest expectedCacheKey3 = new BookSearchRequest(keyword3, null, SearchType.RAG, vector3, true);
        for (int i = 0; i < 50; i++) {
            if (cacheManager.getCache("bookSearchCache").get(expectedCacheKey3) != null) {
                break;
            }
            Thread.sleep(100);
        }

        // Then: AI 서비스가 추가로 호출되어야 함 (총 2회)
        verify(chatModel, times(2)).call(anyString());
        log.info("[TEST] Semantic cache miss verified for different keyword.");
    }

    @Test
    @DisplayName("캐시 TTL이 만료되면 새로운 AI 응답을 생성해야 한다")
    void testCacheExpiration() throws InterruptedException {
        // Given
        String keyword = "TTL 테스트";
        float[] vector = {0.5f, 0.5f, 0.5f};
        BookSearchRequest request = new BookSearchRequest(keyword, null, SearchType.RAG, null);
        PageRequest pageable = PageRequest.of(0, 10);

        cacheManager.getCache("bookSearchCache").clear();
        when(embeddingService.getEmbedding(keyword)).thenReturn(vector);
        
        BookSearchResponse mockResponseExp = new BookSearchResponse(1L, "123456", "테스트 도서", null, "저자", "출판사", 
                BigDecimal.valueOf(10000), LocalDate.now(), null, "테스트 내용입니다.");
        when(bookRepository.search(any(), any())).thenReturn(new PageImpl<>(List.of(mockResponseExp)));
        when(bookRepository.vectorSearch(any(), any())).thenReturn(new PageImpl<>(List.of(mockResponseExp)));
        when(chatModel.call(anyString())).thenReturn("[{\"id\":1, \"relevance\":100, \"why\":\"테스트\"}]");

        // When 1: 첫 번째 검색 (캐시 생성)
        bookSearchService.searchBooks(pageable, request);
        
        // 캐시 생성 대기
        BookSearchRequest cacheKey = new BookSearchRequest(keyword, null, SearchType.RAG, vector, true);
        for (int i = 0; i < 50; i++) {
            if (cacheManager.getCache("bookSearchCache").get(cacheKey) != null) break;
            Thread.sleep(100);
        }
        verify(chatModel, atLeastOnce()).call(anyString());
        long firstCallCount = mockingDetails(chatModel).getInvocations().stream()
                .filter(inv -> inv.getMethod().getName().equals("call"))
                .count();

        // 캐시 강제 만료 처리 (createdAt을 과거로 변경)
        com.nhnacademy.library.core.book.dto.BookSearchResult cachedResult = (com.nhnacademy.library.core.book.dto.BookSearchResult) cacheManager.getCache("bookSearchCache").get(cacheKey).get();
        // createdAt을 2분 전으로 설정 (TTL은 1분)
        com.nhnacademy.library.core.book.dto.BookSearchResult expiredResult = new com.nhnacademy.library.core.book.dto.BookSearchResult(
                cachedResult.getBooks(), cachedResult.getAiResponse(), System.currentTimeMillis() - 120000);
        
        // Redis 캐시 리포지토리에서도 만료 처리 (FindSimilarResultInCache는 이걸 먼저 봄)
        for (com.nhnacademy.library.core.book.domain.BookSearchCache entry : ((com.nhnacademy.library.core.book.repository.BookSearchCacheRepository)applicationContext.getBean(com.nhnacademy.library.core.book.repository.BookSearchCacheRepository.class)).findAll()) {
            if (entry.getKeyword().equals(keyword)) {
                entry.setCreatedAt(System.currentTimeMillis() - 120000);
            }
        }
        
        cacheManager.getCache("bookSearchCache").put(cacheKey, expiredResult);

        // When 2: TTL 만료 후 다시 검색
        log.info("Searching after cache expiration...");
        bookSearchService.searchBooks(pageable, request);

        // Then: 캐시가 만료되었으므로 AI 서비스가 다시 호출되어야 함
        // 비동기 warm-up 대기
        Thread.sleep(2000); 
        long secondCallCount = mockingDetails(chatModel).getInvocations().stream()
                .filter(inv -> inv.getMethod().getName().equals("call"))
                .count();
        
        assertThat(secondCallCount).isGreaterThan(firstCallCount);
        log.info("[TEST] Cache expiration verified successfully. (Calls: {} -> {})", firstCallCount, secondCallCount);
    }
}
