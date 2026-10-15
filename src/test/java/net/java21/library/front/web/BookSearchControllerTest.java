package net.java21.library.front.web;

import net.java21.library.core.book.domain.BrowseSort;
import net.java21.library.core.book.domain.SearchType;
import net.java21.library.core.book.dto.BookSearchRequest;
import net.java21.library.core.book.dto.BookSearchResponse;
import net.java21.library.core.book.dto.BookSearchResult;
import net.java21.library.core.book.dto.BookViewResponse;
import net.java21.library.core.book.service.cover.BookCoverService;
import net.java21.library.core.book.service.search.BookSearchService;
import net.java21.library.core.review.service.ReviewService;
import net.java21.library.core.review.service.ReviewSummaryFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BeanPropertyBindingResult;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * BookSearchController 분기 단위 테스트 (Spring Context 없이 직접 생성)
 */
class BookSearchControllerTest {

    private static final int PAGE_SIZE = 24;

    private BookSearchService bookSearchService;
    private ReviewService reviewService;
    private BookSearchController controller;
    private ExtendedModelMap model;

    @BeforeEach
    void setUp() {
        bookSearchService = mock(BookSearchService.class);
        reviewService = mock(ReviewService.class);
        controller = new BookSearchController(bookSearchService, reviewService, mock(BookCoverService.class));
        model = new ExtendedModelMap();
    }

    private String index(BookSearchRequest request, String sort, Pageable pageable) {
        return controller.index(request, new BeanPropertyBindingResult(request, "bookSearchRequest"), sort, pageable, model);
    }

    @Test
    @DisplayName("검색어와 ISBN이 비어 있으면 검색 전략 대신 browseBooks를 호출한다 (기본 정렬 RECENT)")
    void index_withoutKeyword_browsesBooks() {
        when(bookSearchService.browseBooks(any(), any())).thenReturn(Page.empty());

        String view = index(new BookSearchRequest(" ", null, null, null), null, PageRequest.of(0, PAGE_SIZE));

        assertThat(view).isEqualTo("index/index");
        verify(bookSearchService).browseBooks(PageRequest.of(0, PAGE_SIZE), BrowseSort.RECENT);
        verify(bookSearchService, never()).searchBooks(any(), any(BookSearchRequest.class));
        assertThat(model.getAttribute("browseMode")).isEqualTo(true);
        assertThat(model.getAttribute("sort")).isEqualTo(BrowseSort.RECENT);
    }

    @Test
    @DisplayName("sort=POPULAR이면 리뷰 많은 순으로 둘러보고, Pageable에 섞여 들어온 정렬은 버린다")
    void index_popularSort_passesSortAndDropsPageableSort() {
        when(bookSearchService.browseBooks(any(), any())).thenReturn(Page.empty());

        index(new BookSearchRequest(null, null, null, null), "popular", PageRequest.of(2, PAGE_SIZE, Sort.by("POPULAR")));

        verify(bookSearchService).browseBooks(PageRequest.of(2, PAGE_SIZE), BrowseSort.POPULAR);
    }

    @Test
    @DisplayName("검색어가 있으면 searchBooks를 호출하고, 하이브리드는 상위 N건 결과로 표시한다")
    void index_withKeyword_searchesBooks() {
        BookSearchRequest request = new BookSearchRequest("자바", null, SearchType.HYBRID, null);
        Page<BookSearchResponse> page = new PageImpl<>(List.of(), PageRequest.of(0, PAGE_SIZE), 0);
        when(bookSearchService.searchBooks(any(), eq(request))).thenReturn(new BookSearchResult(page, List.of()));

        index(request, null, PageRequest.of(0, PAGE_SIZE));

        verify(bookSearchService).searchBooks(PageRequest.of(0, PAGE_SIZE), request);
        verify(bookSearchService, never()).browseBooks(any(), any());
        assertThat(model.getAttribute("browseMode")).isEqualTo(false);
        assertThat(model.getAttribute("rankedResult")).isEqualTo(true);
    }

    @Test
    @DisplayName("RAG: 하이브리드 목록 전체와 AI 추천 생성 중(aiPending) 상태를 모델에 담는다")
    void index_rag_addsListAndPendingState() {
        BookSearchRequest request = new BookSearchRequest("위로", null, SearchType.RAG, null);
        List<BookSearchResponse> content = java.util.stream.LongStream.rangeClosed(1, PAGE_SIZE)
                .mapToObj(i -> new BookSearchResponse(i, null, "책" + i, null, null, null, null, null, null, null))
                .toList();
        Page<BookSearchResponse> page = new PageImpl<>(content, PageRequest.of(0, PAGE_SIZE), 30);
        when(bookSearchService.searchBooks(any(), eq(request)))
                .thenReturn(new BookSearchResult(page, List.of(), 0L, false, true));

        index(request, null, PageRequest.of(0, PAGE_SIZE));

        assertThat((List<?>) model.getAttribute("books")).hasSize(PAGE_SIZE);
        assertThat(((Page<?>) model.getAttribute("page")).getTotalElements()).isEqualTo(30);
        assertThat(model.getAttribute("aiPending")).isEqualTo(true);
        assertThat(model.getAttribute("aiRecommendations")).isEqualTo(List.of());
    }

    @Test
    @DisplayName("상세: 리뷰 요약을 정리한 텍스트와 항목 목록을 모델에 담는다")
    void view_addsCleanedSummaryAndSections() {
        BookViewResponse book = new BookViewResponse(1L, null, "제목", null, null, null,
                null, null, null, null, null, null, null, null);
        when(bookSearchService.getBook(1L)).thenReturn(book);
        when(bookSearchService.getReviewSummary(1L)).thenReturn("**[장점]** 따뜻합니다.\n**[단점]** 짧습니다.");
        when(bookSearchService.getReviewSummaryEntity(1L)).thenReturn(Optional.empty());
        when(reviewService.getReviews(eq(1L), any())).thenReturn(Page.empty());

        controller.view(1L, PageRequest.of(0, 5), model);

        assertThat((String) model.getAttribute("reviewSummary")).doesNotContain("**");
        assertThat(model.getAttribute("reviewSummarySections")).isEqualTo(List.of(
                new ReviewSummaryFormatter.Section("장점", "따뜻합니다."),
                new ReviewSummaryFormatter.Section("단점", "짧습니다.")));
    }

    @Test
    @DisplayName("검색어가 ISBN-13이면 하이픈을 제거하고 ISBN 키워드 검색으로 바꾼다")
    void resolveIsbn13Query() {
        BookSearchRequest resolved = BookSearchController.resolveIsbnQuery(
                new BookSearchRequest("978-89-6626-120-8", null, SearchType.RAG, null));

        assertThat(resolved.keyword()).isNull();
        assertThat(resolved.isbn()).isEqualTo("9788966261208");
        assertThat(resolved.searchType()).isEqualTo(SearchType.KEYWORD);
    }

    @Test
    @DisplayName("검색어가 ISBN-10이면 ISBN-13으로 변환한다")
    void resolveIsbn10Query() {
        BookSearchRequest resolved = BookSearchController.resolveIsbnQuery(
                new BookSearchRequest("89-6626-120-6", null, SearchType.RAG, null));

        assertThat(resolved.isbn()).isEqualTo("9788966261208");
    }

    @Test
    @DisplayName("ISBN 형식이 아닌 검색어는 그대로 둔다")
    void keepNonIsbnQuery() {
        BookSearchRequest request = new BookSearchRequest("자바 입문서 2024", null, SearchType.RAG, null);

        assertThat(BookSearchController.resolveIsbnQuery(request)).isSameAs(request);
    }
}
