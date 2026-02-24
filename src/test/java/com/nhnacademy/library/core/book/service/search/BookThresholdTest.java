package com.nhnacademy.library.core.book.service.search;
import com.nhnacademy.library.core.book.domain.SearchType;
import com.nhnacademy.library.core.book.service.search.BookSearchService;
import org.springframework.ai.chat.model.ChatModel;
import com.nhnacademy.library.core.book.service.embedding.EmbeddingService;
import com.nhnacademy.library.core.book.service.embedding.BookEmbeddingService;
import com.nhnacademy.library.core.review.service.ReviewSummarizer;
import com.nhnacademy.library.core.book.service.cache.BookSearchCacheService;

import com.nhnacademy.library.core.book.dto.BookSearchRequest;
import com.nhnacademy.library.core.book.dto.BookSearchResponse;
import com.nhnacademy.library.core.book.repository.BookRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;
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
@TestPropertySource(properties = {
    "spring.ai.selected-model=ollama",
    "rabbitmq.queue.review-summary=nhnacademy-library-review"
})
@Transactional
class BookThresholdTest {

    @Autowired
    private BookSearchService bookSearchService;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private BookRepository bookRepository;

    @MockitoBean
    private ChatModel chatModel;

    @MockitoBean
    private EmbeddingService embeddingService;

    @Autowired
    private BookSearchCacheService bookSearchCacheService;

    @Test
    @DisplayName("RRF 점수가 0.02 미만인 경우 LLM 추천이 생략되어야 한다 (isWarmUp=false)")
    void testRagSearchWithThreshold() {
        // Given
        String keyword = "테스트_Threshold";
        float[] dummyVector = new float[1024];
        when(embeddingService.getEmbedding(anyString())).thenReturn(dummyVector);
        
        List<BookSearchResponse> keywordResults = IntStream.range(0, 10)
                .mapToObj(i -> new BookSearchResponse((long)i, "isbn"+i, "title"+i, null, "author"+i, "publisher"+i, null, null, null, "content"+i, 0.0))
                .toList();
        
        when(bookRepository.search(any(), any())).thenReturn(new PageImpl<>(keywordResults));
        when(bookRepository.vectorSearch(any(), any())).thenReturn(new PageImpl<>(List.of()));

        when(chatModel.call(anyString())).thenReturn("[]");

        // When
        // isWarmUp=false (기본값) 이므로 fallback이 작동하지 않아야 함
        BookSearchRequest request = new BookSearchRequest(keyword, null, SearchType.RAG, null, false);
        bookSearchService.searchBooks(PageRequest.of(0, 10), request);

        // Then
        verify(chatModel, never()).call(anyString());
    }

    @Test
    @DisplayName("RRF 점수가 0.02 미만이어도 isWarmUp=true이면 AI 호출이 발생해야 한다 (Fallback)")
    void testRagSearchWarmUpFallback() throws InterruptedException {
        // Given
        String keyword = "테스트_웜업_Fallback";
        when(embeddingService.getEmbedding(anyString())).thenReturn(new float[1024]);
        when(bookRepository.search(any(), any())).thenReturn(new PageImpl<>(List.of(new BookSearchResponse(0L, "isbn0", "title0", null, "author0", "publisher0", null, null, null, "content0", 0.0))));
        when(bookRepository.vectorSearch(any(), any())).thenReturn(new PageImpl<>(List.of()));
        when(chatModel.call(anyString())).thenReturn("[]");

        // When
        BookSearchRequest request = new BookSearchRequest(keyword, null, SearchType.RAG, null, true);
        bookSearchService.searchBooks(PageRequest.of(0, 10), request);

        // Then
        verify(chatModel, atLeastOnce()).call(anyString());
    }
    
    @Test
    @DisplayName("RRF 점수가 0.02 이상인 도서만 LLM 컨텍스트에 포함되어야 한다")
    void testRagSearchWithHighScores() throws InterruptedException {
        // Given
        String keyword = "테스트_High";
        float[] dummyVector = new float[1024];
        when(embeddingService.getEmbedding(anyString())).thenReturn(dummyVector);
        
        BookSearchResponse commonBook = new BookSearchResponse(1L, "isbn1", "title1", null, "author1", "publisher1", null, null, null, "content1", 1.0);
        
        when(bookRepository.search(any(), any())).thenReturn(new PageImpl<>(List.of(commonBook)));
        when(bookRepository.vectorSearch(any(), any())).thenReturn(new PageImpl<>(List.of(commonBook)));

        when(chatModel.call(anyString())).thenReturn("[]");

        // 캐시가 비어있는지 확인 (이전 테스트 영향 방지)
        cacheManager.getCache("bookSearchCache").clear();

        // When & Then
        // BookSearchCacheService를 사용하여 warm-up을 수행하면 
        // searchBooks 내부에서 isFromWarmUp=true가 되어 AI 분석 로직이 강제로 실행됨.
        bookSearchCacheService.warmUpRagCache(keyword);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        boolean invoked = false;
        for (int i = 0; i < 20; i++) {
            try {
                verify(chatModel, atLeastOnce()).call(promptCaptor.capture());
                invoked = true;
                break;
            } catch (Throwable e) {
                Thread.sleep(100);
            }
        }
        assertThat(invoked).as("AI 호출이 발생해야 함").isTrue();
        
        String renderedPrompt = promptCaptor.getValue();
        long bookCount = renderedPrompt.lines().filter(line -> line.contains("ID:")).count();
        
        // 1/61 + 1/61 = 0.032... >= 0.02 이므로 포함되어야 함.
        assertThat(bookCount).isEqualTo(1);
    }
}
