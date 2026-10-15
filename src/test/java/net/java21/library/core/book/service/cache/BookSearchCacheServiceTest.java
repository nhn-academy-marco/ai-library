package net.java21.library.core.book.service.cache;

import net.java21.library.core.book.dto.BookSearchRequest;
import net.java21.library.core.book.service.embedding.EmbeddingService;
import net.java21.library.core.book.service.search.BookSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("BookSearchCacheService: 웜업 실패 후 재시도")
class BookSearchCacheServiceTest {

    private BookSearchService bookSearchService;
    private BookSearchCacheService service;

    @BeforeEach
    void setUp() {
        ApplicationContext context = mock(ApplicationContext.class);
        bookSearchService = mock(BookSearchService.class);
        EmbeddingService embeddingService = mock(EmbeddingService.class);
        SemanticCacheService semanticCacheService = mock(SemanticCacheService.class);
        when(context.getBean(BookSearchService.class)).thenReturn(bookSearchService);
        when(context.getBean(EmbeddingService.class)).thenReturn(embeddingService);
        when(embeddingService.getEmbedding(anyString())).thenReturn(new float[]{0.1f});
        when(semanticCacheService.findRecommendations(any())).thenReturn(Optional.empty());
        service = new BookSearchCacheService(context, semanticCacheService);
    }

    @Test
    @DisplayName("웜업이 실패해도 중복 방지 표시가 남지 않아 같은 검색어의 다음 웜업이 다시 실행된다")
    void failedWarmUp_canRetry() {
        when(bookSearchService.searchBooks(any(Pageable.class), any(BookSearchRequest.class)))
                .thenThrow(new IllegalStateException("LLM 연결 불가"));

        service.warmUpRagCache("자바");
        service.warmUpRagCache("자바");

        verify(bookSearchService, times(2)).searchBooks(any(Pageable.class), any(BookSearchRequest.class));
    }
}
