package com.nhnacademy.library.core.book.service;

import com.nhnacademy.library.core.book.dto.BookAiRecommendationResponse;
import com.nhnacademy.library.core.book.dto.BookSearchRequest;
import com.nhnacademy.library.core.book.dto.BookSearchResponse;
import com.nhnacademy.library.core.book.repository.BookRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
@Transactional
class BookThresholdTest {

    @Autowired
    private BookSearchService bookSearchService;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private CacheManager cacheManager;

    @MockBean
    private BookRepository bookRepository;

    @MockBean
    private BookAiService bookAiService;

    @MockBean
    private EmbeddingService embeddingService;

    @Test
    @DisplayName("RAG 검색 시 RRF 점수가 0.02 미만인 도서는 LLM 컨텍스트에서 제외되어야 한다 (Mission 2)")
    void testRagSearchWithThreshold() {
        // Given
        String keyword = "테스트";
        float[] dummyVector = new float[1024];
        when(embeddingService.getEmbedding(anyString())).thenReturn(dummyVector);
        
        // 10개의 검색 결과 생성 (5개는 점수 0.02 이상, 5개는 0.02 미만)
        // RRF 점수는 hybridSearch 내부에서 계산되지만, 여기서는 hybridSearch가 호출하는 repository를 모킹하여 
        // 최종적으로 반환되는 results의 RRF 점수를 직접 조작하거나, hybridSearch 자체를 모킹해야 할 수도 있음.
        // 현재 BookSearchService.searchBooks는 hybridSearch를 직접 호출하므로, 
        // hybridSearch가 사용하는 bookRepository.search와 bookRepository.vectorSearch를 모킹함.
        
        List<BookSearchResponse> keywordResults = IntStream.range(0, 10)
                .mapToObj(i -> new BookSearchResponse((long)i, "isbn"+i, "title"+i, null, "author"+i, "publisher"+i, null, null, null, "content"+i, 0.0))
                .toList();
        
        when(bookRepository.search(any(), any())).thenReturn(new PageImpl<>(keywordResults));
        when(bookRepository.vectorSearch(any(), any())).thenReturn(new PageImpl<>(List.of())); // 벡터 검색은 비워둠

        // BookAiService 모킹
        when(bookAiService.askAboutBooks(anyString())).thenReturn("[]");

        // When
        BookSearchRequest request = new BookSearchRequest(keyword, null, "rag", null);
        bookSearchService.searchBooks(PageRequest.of(0, 10), request);

        // Then
        // RRF 점수 계산 방식: 1 / (60 + rank + 1)
        // Rank 0: 1/61 = 0.01639... -> 0.02 미만임!
        // 모든 결과가 0.02 미만이 되므로 LLM에게는 아무것도 전달되지 않아야 함. (askAboutBooks 미호출)
        verify(bookAiService, never()).askAboutBooks(anyString());
    }
    
    @Test
    @DisplayName("RRF 점수가 0.02 이상인 도서만 LLM 컨텍스트에 포함되어야 한다")
    void testRagSearchWithHighScores() {
        // Given
        String keyword = "테스트";
        float[] dummyVector = new float[1024];
        when(embeddingService.getEmbedding(anyString())).thenReturn(dummyVector);
        
        BookSearchResponse commonBook = new BookSearchResponse(1L, "isbn1", "title1", null, "author1", "publisher1", null, null, null, "content1", 0.1);
        
        when(bookRepository.search(any(), any())).thenReturn(new PageImpl<>(List.of(commonBook)));
        when(bookRepository.vectorSearch(any(), any())).thenReturn(new PageImpl<>(List.of(commonBook)));

        when(bookAiService.askAboutBooks(anyString())).thenReturn("[]");

        // 캐시가 비어있는지 확인 (이전 테스트 영향 방지)
        cacheManager.getCache("bookSearchCache").clear();

        // When & Then
        // BookSearchCacheService를 사용하여 warm-up을 수행하면 
        // searchBooks 내부에서 isFromWarmUp=true가 되어 AI 분석 로직이 강제로 실행됨.
        BookSearchCacheService bookSearchCacheService = new BookSearchCacheService(applicationContext, cacheManager);
        bookSearchCacheService.warmUpRagCache(keyword);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(bookAiService).askAboutBooks(promptCaptor.capture());
        
        String renderedPrompt = promptCaptor.getValue();
        long bookCount = renderedPrompt.lines().filter(line -> line.contains("ID:")).count();
        
        // 1/61 + 1/61 = 0.032... >= 0.02 이므로 포함되어야 함.
        assertThat(bookCount).isEqualTo(1);
    }
}
