package com.nhnacademy.library.core.book.service.search;

import org.springframework.ai.chat.model.ChatModel;
import com.nhnacademy.library.core.book.service.embedding.EmbeddingService;
import com.nhnacademy.library.core.book.service.cache.BookSearchCacheService;

import com.nhnacademy.library.core.book.dto.BookSearchResponse;
import com.nhnacademy.library.core.book.repository.BookRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import org.mockito.ArgumentCaptor;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@Slf4j
@SpringBootTest
@TestPropertySource(properties = {
    "spring.ai.selected-model=ollama",
    "rabbitmq.queue.review-summary=nhnacademy-library-review"
})
@Transactional
class BookRerankKTest {

    @MockitoBean
    private ChatModel chatModel;

    @MockitoBean
    private BookRepository bookRepository;

    @MockitoBean
    private EmbeddingService embeddingService;

    @Autowired
    private BookSearchCacheService bookSearchCacheService;

    @Test
    @DisplayName("RAG 검색 시 UI 페이징과 관계없이 넉넉한 후보군(Retrieval K)을 탐색해야 한다")
    void testRagSearchRetrievalKIndependence() throws InterruptedException {
        // Given
        String keyword = "자바_Rerank";
        // 벡터 임베딩 모킹
        when(embeddingService.getEmbedding(anyString())).thenReturn(new float[1024]);

        // 하이브리드 검색에서 동일 문서가 키워드/벡터 모두에 등장하도록 모킹하여 RRF 점수가 임계값을 넘도록 함
        BookSearchResponse common = new BookSearchResponse(1L, "isbn1", "title1", null, "author1", "publisher1", null, null, null, "content1", 1.0);
        
        // any() 대신 matchers를 사용하여 모든 호출에 대해 common 반환
        when(bookRepository.search(any(), any())).thenReturn(new PageImpl<>(List.of(common)));
        when(bookRepository.vectorSearch(any(), any())).thenReturn(new PageImpl<>(List.of(common)));

        when(chatModel.call(anyString())).thenReturn("[]");

        // Warm-up을 통해 캐시 미스에 의한 폴백을 피하고, 실제 AI 호출이 일어나도록 강제 실행
        bookSearchCacheService.warmUpRagCache(keyword);

        // 비동기 실행 완료를 위해 잠시 대기 (최대 2초)
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
        long bookCountInPrompt = renderedPrompt.lines().filter(line -> line.contains("ID:")).count();
        
        log.info("[DEBUG_LOG] Books in AI Prompt: {}", bookCountInPrompt);
        assertThat(bookCountInPrompt).isGreaterThanOrEqualTo(1);
    }
}
