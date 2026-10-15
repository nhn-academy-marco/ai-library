package net.java21.library.core.book.service.search.strategy;

import net.java21.library.core.book.domain.SearchType;
import net.java21.library.core.book.dto.BookAiRecommendationResponse;
import net.java21.library.core.book.dto.BookSearchRequest;
import net.java21.library.core.book.dto.BookSearchResponse;
import net.java21.library.core.book.dto.BookSearchResult;
import net.java21.library.core.book.event.BookSearchEvent;
import net.java21.library.core.book.service.ai.AiRecommendationService;
import net.java21.library.core.book.service.cache.SearchResultCache;
import net.java21.library.core.book.service.cache.SemanticCacheService;
import net.java21.library.core.config.CacheConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RagSearchStrategy: 하이브리드 목록 + AI 추천")
class RagSearchStrategyTest {

    private static final int PAGE_SIZE = 24;
    private static final int TOTAL = 30;
    private static final float[] VECTOR = {0.1f, 0.2f};

    @Mock
    private HybridSearchStrategy hybridSearchStrategy;
    @Mock
    private SemanticCacheService semanticCacheService;
    @Mock
    private AiRecommendationService aiRecommendationService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private RagSearchStrategy strategy;

    @BeforeEach
    void setUp() {
        SearchResultCache searchResultCache =
                new SearchResultCache(new ConcurrentMapCacheManager(CacheConfig.BOOK_SEARCH_RESULT_CACHE));
        strategy = new RagSearchStrategy(hybridSearchStrategy, semanticCacheService, aiRecommendationService,
                eventPublisher, searchResultCache);
    }

    private static List<BookSearchResponse> books(int count) {
        return LongStream.rangeClosed(1, count)
                .mapToObj(i -> new BookSearchResponse(i, "isbn" + i, "title" + i, null, "author" + i, "publisher" + i,
                        null, null, null, "content" + i, 0.5, 0.03))
                .toList();
    }

    private static BookSearchResult hybridPage(Pageable pageable) {
        List<BookSearchResponse> all = books(TOTAL);
        int from = (int) pageable.getOffset();
        int to = Math.min(from + pageable.getPageSize(), all.size());
        return BookSearchResult.builder().books(new PageImpl<>(all.subList(from, to), pageable, all.size())).build();
    }

    private static BookAiRecommendationResponse rec(long id) {
        BookAiRecommendationResponse rec = new BookAiRecommendationResponse();
        rec.setId(id);
        rec.setRelevance(90);
        rec.setWhy("사유");
        return rec;
    }

    private static BookSearchRequest ragRequest(boolean warmUp) {
        return new BookSearchRequest("자바", null, SearchType.RAG, VECTOR, warmUp);
    }

    @Test
    @DisplayName("캐시 적중: 목록은 하이브리드 결과 전체(30건 중 24건), AI 추천은 캐시 값을 함께 돌려주고 웜업하지 않는다")
    void cacheHit_returnsFullHybridListWithRecommendations() {
        Pageable pageable = PageRequest.of(0, PAGE_SIZE);
        when(hybridSearchStrategy.search(eq(pageable), any())).thenReturn(hybridPage(pageable));
        when(semanticCacheService.findRecommendations(any())).thenReturn(Optional.of(List.of(rec(1L))));

        BookSearchResult result = strategy.search(pageable, ragRequest(false));

        assertThat(result.getBooks().getTotalElements()).isEqualTo(TOTAL);
        assertThat(result.getBooks().getContent()).hasSize(PAGE_SIZE);
        assertThat(result.getAiResponse()).extracting(BookAiRecommendationResponse::getId).containsExactly(1L);
        assertThat(result.isAiPending()).isFalse();
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("캐시 미스: 목록은 하이브리드 결과 전체, aiPending=true, 웜업 이벤트를 발행한다")
    void cacheMiss_returnsListAndPending() {
        Pageable pageable = PageRequest.of(0, PAGE_SIZE);
        when(hybridSearchStrategy.search(eq(pageable), any())).thenReturn(hybridPage(pageable));
        when(semanticCacheService.findRecommendations(any())).thenReturn(Optional.empty());

        BookSearchResult result = strategy.search(pageable, ragRequest(false));

        assertThat(result.getBooks().getTotalElements()).isEqualTo(TOTAL);
        assertThat(result.getBooks().getContent()).hasSize(PAGE_SIZE);
        assertThat(result.getAiResponse()).isEmpty();
        assertThat(result.isAiPending()).isTrue();
        verify(eventPublisher).publishEvent(any(BookSearchEvent.class));
        verify(aiRecommendationService, never()).recommend(anyString(), anyList());
    }

    @Test
    @DisplayName("2페이지: 하이브리드 목록의 해당 페이지(6건)를 돌려준다")
    void secondPage_returnsHybridPage() {
        Pageable pageable = PageRequest.of(1, PAGE_SIZE);
        when(hybridSearchStrategy.search(eq(pageable), any())).thenReturn(hybridPage(pageable));
        when(semanticCacheService.findRecommendations(any())).thenReturn(Optional.of(List.of(rec(1L))));

        BookSearchResult result = strategy.search(pageable, ragRequest(false));

        assertThat(result.getBooks().getNumber()).isEqualTo(1);
        assertThat(result.getBooks().getContent()).hasSize(TOTAL - PAGE_SIZE);
        assertThat(result.getBooks().getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("같은 검색을 반복하면 하이브리드 목록은 결과 캐시에서 가져온다")
    void repeatedSearch_usesHybridResultCache() {
        Pageable pageable = PageRequest.of(0, PAGE_SIZE);
        when(hybridSearchStrategy.search(eq(pageable), any())).thenReturn(hybridPage(pageable));
        when(semanticCacheService.findRecommendations(any())).thenReturn(Optional.empty());

        strategy.search(pageable, ragRequest(false));
        strategy.search(pageable, ragRequest(false));

        verify(hybridSearchStrategy, times(1)).search(any(), any());
    }

    @Test
    @DisplayName("웜업: 후보로 LLM 추천을 만들어 AI 추천만 시맨틱 캐시에 저장한다")
    void warmUp_savesRecommendationsOnly() {
        when(hybridSearchStrategy.search(any(), any())).thenReturn(hybridPage(PageRequest.of(0, 100)));
        List<BookAiRecommendationResponse> recs = List.of(rec(1L), rec(2L));
        when(aiRecommendationService.recommend(eq("자바"), anyList())).thenReturn(recs);

        BookSearchRequest request = ragRequest(true);
        strategy.search(PageRequest.of(0, PAGE_SIZE), request);

        verify(semanticCacheService).save(request, recs);
    }

    @Test
    @DisplayName("웜업 중 LLM 호출이 실패하면 캐시에 저장하지 않고 예외를 전파한다(다음 요청에서 재시도)")
    void warmUp_llmFailure_notCached() {
        when(hybridSearchStrategy.search(any(), any())).thenReturn(hybridPage(PageRequest.of(0, 100)));
        when(aiRecommendationService.recommend(anyString(), anyList())).thenThrow(new IllegalStateException("LLM down"));

        assertThatThrownBy(() -> strategy.search(PageRequest.of(0, PAGE_SIZE), ragRequest(true)))
                .isInstanceOf(IllegalStateException.class);
        verify(semanticCacheService, never()).save(any(), anyList());
    }
}
