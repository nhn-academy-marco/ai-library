package net.java21.library.core.book.service.search;

import net.java21.library.core.book.domain.SearchType;
import net.java21.library.core.book.dto.BookSearchRequest;
import net.java21.library.core.book.dto.BookSearchResult;
import net.java21.library.core.book.service.cache.SearchResultCache;
import net.java21.library.core.book.service.embedding.EmbeddingService;
import net.java21.library.core.book.service.search.strategy.HybridSearchStrategy;
import net.java21.library.core.book.service.search.strategy.KeywordSearchStrategy;
import net.java21.library.core.book.service.search.strategy.RagSearchStrategy;
import net.java21.library.core.config.CacheConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookSearchService 검색 결과 캐시 테스트")
class BookSearchServiceCacheTest {

    private static final Pageable PAGEABLE = PageRequest.of(0, 24);

    @Mock
    private EmbeddingService embeddingService;
    @Mock
    private KeywordSearchStrategy keywordSearchStrategy;
    @Mock
    private HybridSearchStrategy hybridSearchStrategy;
    @Mock
    private RagSearchStrategy ragSearchStrategy;

    private BookSearchService bookSearchService;

    @BeforeEach
    void setUp() {
        SearchResultCache searchResultCache =
                new SearchResultCache(new ConcurrentMapCacheManager(CacheConfig.BOOK_SEARCH_RESULT_CACHE));
        bookSearchService = new BookSearchService(null, embeddingService, null, null, null, null,
                searchResultCache, keywordSearchStrategy, null, hybridSearchStrategy, ragSearchStrategy);
    }

    @Test
    @DisplayName("같은 하이브리드 검색을 반복하면 두 번째는 캐시 결과를 돌려주고 임베딩·검색을 다시 하지 않는다")
    void hybridSearch_repeated_servedFromCache() {
        BookSearchResult expected = new BookSearchResult(Page.empty(PAGEABLE), List.of());
        when(embeddingService.getEmbedding("spring")).thenReturn(new float[]{0.1f});
        when(hybridSearchStrategy.search(eq(PAGEABLE), any())).thenReturn(expected);

        BookSearchResult first = bookSearchService.searchBooks(PAGEABLE, new BookSearchRequest("spring", null, SearchType.HYBRID, null));
        BookSearchResult second = bookSearchService.searchBooks(PAGEABLE, new BookSearchRequest("spring", null, SearchType.HYBRID, null));

        assertThat(second).isSameAs(first);
        verify(embeddingService, times(1)).getEmbedding(anyString());
        verify(hybridSearchStrategy, times(1)).search(any(), any());
    }

    @Test
    @DisplayName("검색어 앞뒤·연속 공백은 정규화되어 같은 캐시 키를 쓰고, 정규화된 검색어로 검색한다")
    void keywordSearch_whitespaceNormalized() {
        when(keywordSearchStrategy.search(eq(PAGEABLE), any()))
                .thenReturn(new BookSearchResult(Page.empty(PAGEABLE), List.of()));

        bookSearchService.searchBooks(PAGEABLE, new BookSearchRequest(" spring  boot ", null, SearchType.KEYWORD, null));
        bookSearchService.searchBooks(PAGEABLE, new BookSearchRequest("spring boot", null, SearchType.KEYWORD, null));

        ArgumentCaptor<BookSearchRequest> captor = ArgumentCaptor.forClass(BookSearchRequest.class);
        verify(keywordSearchStrategy, times(1)).search(eq(PAGEABLE), captor.capture());
        assertThat(captor.getValue().keyword()).isEqualTo("spring boot");
    }

    @Test
    @DisplayName("페이지가 다르면 별도로 검색한다")
    void differentPage_notShared() {
        when(keywordSearchStrategy.search(any(), any()))
                .thenReturn(new BookSearchResult(Page.empty(PAGEABLE), List.of()));

        bookSearchService.searchBooks(PageRequest.of(0, 24), new BookSearchRequest("spring", null, SearchType.KEYWORD, null));
        bookSearchService.searchBooks(PageRequest.of(1, 24), new BookSearchRequest("spring", null, SearchType.KEYWORD, null));

        verify(keywordSearchStrategy, times(2)).search(any(), any());
    }

    @Test
    @DisplayName("임베딩 장애로 키워드 검색으로 대체된 결과는 캐시하지 않는다")
    void keywordFallback_notCached() {
        when(embeddingService.getEmbedding("spring")).thenThrow(new ResourceAccessException("down"));
        when(keywordSearchStrategy.search(eq(PAGEABLE), any()))
                .thenReturn(new BookSearchResult(Page.empty(PAGEABLE), List.of()));

        bookSearchService.searchBooks(PAGEABLE, new BookSearchRequest("spring", null, SearchType.HYBRID, null));
        bookSearchService.searchBooks(PAGEABLE, new BookSearchRequest("spring", null, SearchType.HYBRID, null));

        verify(embeddingService, times(2)).getEmbedding("spring");
    }

    @Test
    @DisplayName("RAG 검색은 이 캐시를 쓰지 않는다(시맨틱 캐시가 담당)")
    void ragSearch_notCached() {
        when(embeddingService.getEmbedding("spring")).thenReturn(new float[]{0.1f});
        when(ragSearchStrategy.search(eq(PAGEABLE), any()))
                .thenReturn(new BookSearchResult(Page.empty(PAGEABLE), List.of()));

        bookSearchService.searchBooks(PAGEABLE, new BookSearchRequest("spring", null, SearchType.RAG, null));
        bookSearchService.searchBooks(PAGEABLE, new BookSearchRequest("spring", null, SearchType.RAG, null));

        verify(ragSearchStrategy, times(2)).search(any(), any());
    }
}
