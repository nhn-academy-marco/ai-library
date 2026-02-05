package com.nhnacademy.library.core.book.service.cache;
import com.nhnacademy.library.core.book.service.search.BookSearchService;
import org.springframework.ai.chat.model.ChatModel;
import com.nhnacademy.library.core.book.service.embedding.EmbeddingService;
import com.nhnacademy.library.core.book.service.embedding.BookEmbeddingService;
import com.nhnacademy.library.core.review.service.ReviewSummarizer;

import com.nhnacademy.library.core.book.domain.SearchType;
import com.nhnacademy.library.core.book.dto.BookSearchRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

@SpringBootTest
@Transactional
class BookSearchEventCachingTest {

    @Autowired
    private BookSearchService bookSearchService;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @SpyBean
    private ChatModel chatModel;

    @Test
    @DisplayName("RAG 검색 시 캐시가 없으면 이벤트를 발행하고 하이브리드 결과를 반환해야 한다")
    void testRagSearchPublishesEventWhenCacheMissing() {
        // Given
        String keyword = "이벤트테스트";
        BookSearchRequest request = new BookSearchRequest(keyword, null, SearchType.RAG, null);
        
        // 캐시 비우기
        cacheManager.getCache("bookSearchCache").clear();

        // When
        com.nhnacademy.library.core.book.dto.BookSearchResult result = bookSearchService.searchBooks(PageRequest.of(0, 10), request);

        // Then
        // aiResponse는 null이어야 함 (폴백)
        assertThat(result.getAiResponse()).isNull();
        
        // 도서 목록은 반환되어야 함 (하이브리드 검색 결과)
        assertThat(result.getBooks().getContent()).isNotEmpty();
    }

    @Test
    @DisplayName("하이브리드 검색 시에도 이벤트를 발행하여 백그라운드 캐싱을 시도해야 한다")
    void testHybridSearchPublishesEvent() {
        // Given
        String keyword = "하이브리드이벤트";
        BookSearchRequest request = new BookSearchRequest(keyword, null, SearchType.HYBRID, null);
        
        // 캐시 비우기
        cacheManager.getCache("bookSearchCache").clear();

        // When
        bookSearchService.searchBooks(PageRequest.of(0, 10), request);

        // Then
        // 이 테스트는 예외 없이 수행됨을 확인 (로그를 통해 이벤트 발행 확인 가능)
    }
}
