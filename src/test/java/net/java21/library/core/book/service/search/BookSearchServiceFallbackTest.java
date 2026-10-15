package net.java21.library.core.book.service.search;

import net.java21.library.core.book.domain.SearchType;
import net.java21.library.core.book.dto.BookSearchRequest;
import net.java21.library.core.book.dto.BookSearchResult;
import net.java21.library.core.book.service.embedding.EmbeddingService;
import net.java21.library.core.book.service.search.strategy.HybridSearchStrategy;
import net.java21.library.core.book.service.search.strategy.KeywordSearchStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookSearchService 임베딩 장애 대체 테스트")
class BookSearchServiceFallbackTest {

    @Mock
    private EmbeddingService embeddingService;
    @Mock
    private KeywordSearchStrategy keywordSearchStrategy;
    @Mock
    private HybridSearchStrategy hybridSearchStrategy;

    @InjectMocks
    private BookSearchService bookSearchService;

    @Test
    @DisplayName("임베딩 서버에 연결할 수 없으면 하이브리드 검색 대신 키워드 검색 결과를 돌려준다")
    void fallsBackToKeywordSearchWhenEmbeddingFails() {
        Pageable pageable = PageRequest.of(0, 24);
        when(embeddingService.getEmbedding("spring")).thenThrow(new ResourceAccessException("No route to host"));
        when(keywordSearchStrategy.search(eq(pageable), any()))
                .thenReturn(new BookSearchResult(Page.empty(pageable), List.of()));

        BookSearchResult result = bookSearchService.searchBooks(pageable,
                new BookSearchRequest("spring", null, SearchType.HYBRID, null));

        assertThat(result.isKeywordFallback()).isTrue();
        ArgumentCaptor<BookSearchRequest> captor = ArgumentCaptor.forClass(BookSearchRequest.class);
        verify(keywordSearchStrategy).search(eq(pageable), captor.capture());
        assertThat(captor.getValue().searchType()).isEqualTo(SearchType.KEYWORD);
        assertThat(captor.getValue().keyword()).isEqualTo("spring");
        verify(hybridSearchStrategy, never()).search(any(), any());
    }

    @Test
    @DisplayName("임베딩이 정상이면 선택한 검색 전략을 그대로 사용한다")
    void usesSelectedStrategyWhenEmbeddingWorks() {
        Pageable pageable = PageRequest.of(0, 24);
        when(embeddingService.getEmbedding("spring")).thenReturn(new float[]{0.1f, 0.2f});
        when(hybridSearchStrategy.search(eq(pageable), any()))
                .thenReturn(new BookSearchResult(Page.empty(pageable), List.of()));

        BookSearchResult result = bookSearchService.searchBooks(pageable,
                new BookSearchRequest("spring", null, SearchType.HYBRID, null));

        assertThat(result.isKeywordFallback()).isFalse();
        verify(keywordSearchStrategy, never()).search(any(), any());
    }
}
