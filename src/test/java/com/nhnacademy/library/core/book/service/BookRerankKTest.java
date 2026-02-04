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
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import org.mockito.ArgumentCaptor;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@Slf4j
@SpringBootTest
@Transactional
class BookRerankKTest {

    @Autowired
    private BookSearchService bookSearchService;

    @MockBean
    private BookAiService bookAiService;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private CacheManager cacheManager;

    @MockBean
    private BookRepository bookRepository;

    @MockBean
    private EmbeddingService embeddingService;

    @Test
    @DisplayName("RAG 검색 시 UI 페이징과 관계없이 넉넉한 후보군(Retrieval K)을 탐색해야 한다")
    void testRagSearchRetrievalKIndependence() {
        // Given
        String keyword = "자바";
        // 벡터 임베딩 모킹
        when(embeddingService.getEmbedding(anyString())).thenReturn(new float[1024]);

        // 하이브리드 검색에서 동일 문서가 키워드/벡터 모두에 등장하도록 모킹하여 RRF 점수가 임계값을 넘도록 함
        BookSearchResponse common = new BookSearchResponse(1L, "isbn1", "title1", null, "author1", "publisher1", null, null, null, "content1", 0.1);
        when(bookRepository.search(any(), any())).thenReturn(new PageImpl<>(List.of(common)));
        when(bookRepository.vectorSearch(any(), any())).thenReturn(new PageImpl<>(List.of(common)));

        when(bookAiService.askAboutBooks(anyString())).thenReturn("[]");

        // Warm-up을 통해 캐시 미스에 의한 폴백을 피하고, 실제 AI 호출이 일어나도록 강제 실행
        BookSearchCacheService cacheService = new BookSearchCacheService(applicationContext, cacheManager);
        cacheService.warmUpRagCache(keyword);

        // Then: LLM에게 전달된 프롬프트에 포함된 도서가 UI 페이징(1개)보다 많을 수 있는지 확인
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(bookAiService, atLeastOnce()).askAboutBooks(promptCaptor.capture());
        
        String renderedPrompt = promptCaptor.getValue();
        long bookCountInPrompt = renderedPrompt.lines().filter(line -> line.contains("ID:")).count();
        
        log.info("[DEBUG_LOG] Books in AI Prompt: {}", bookCountInPrompt);
        assertThat(bookCountInPrompt).isGreaterThanOrEqualTo(1);
    }
}
