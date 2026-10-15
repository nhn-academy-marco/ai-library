package net.java21.library.front.web;

import net.java21.library.core.book.domain.BrowseSort;
import net.java21.library.core.book.domain.SearchType;
import net.java21.library.core.book.dto.BookAiRecommendationResponse;
import net.java21.library.core.book.dto.BookSearchRequest;
import net.java21.library.core.book.dto.BookSearchResponse;
import net.java21.library.core.book.dto.BookViewResponse;
import net.java21.library.core.review.service.ReviewSummaryFormatter;
import nz.net.ultraq.thymeleaf.layoutdialect.LayoutDialect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 첫 화면(둘러보기·검색 결과·AI 추천)과 상세 화면 템플릿을 Spring Context 없이 렌더링해 확인합니다.
 */
class BookSearchTemplateTest {

    private static final int PAGE_SIZE = 24;
    private static final long TOTAL_BOOKS = 1_000L;

    private SpringTemplateEngine engine;

    @BeforeEach
    void setUp() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        engine.addDialect(new LayoutDialect());
    }

    private String render(String template, Map<String, Object> variables) {
        MockServletContext servletContext = new MockServletContext();
        JakartaServletWebApplication application = JakartaServletWebApplication.buildApplication(servletContext);
        var exchange = application.buildExchange(new MockHttpServletRequest(servletContext), new MockHttpServletResponse());
        return engine.process(template, new WebContext(exchange, Locale.KOREA, variables));
    }

    private static BookSearchResponse book(long id, String title) {
        BookSearchResponse response = new BookSearchResponse(id, "97911567592" + id, title, null, "저자" + id, null,
                null, LocalDate.of(2023, 5, 1), null, "소개");
        response.setAverageRating(new BigDecimal("4.50"));
        response.setReviewCount(7L);
        return response;
    }

    private static Map<String, Object> indexVars(BookSearchRequest request, boolean browseMode, boolean ranked,
                                                 List<BookSearchResponse> books, Page<BookSearchResponse> page) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("request", request);
        vars.put("browseMode", browseMode);
        vars.put("rankedResult", ranked);
        vars.put("sort", BrowseSort.POPULAR);
        vars.put("sorts", BrowseSort.values());
        vars.put("books", books);
        vars.put("page", page);
        vars.put("aiModels", AiModelInfo.of("ollama", "qwen2.5:latest", "gemini-2.0-flash", "bge-m3"));
        return vars;
    }

    private static List<BookSearchResponse> books(int count) {
        return java.util.stream.IntStream.rangeClosed(1, count)
                .mapToObj(i -> book(i, "하이브리드 결과 " + i))
                .toList();
    }

    private static BookAiRecommendationResponse recommendation(BookSearchResponse book, int relevance, String why) {
        BookAiRecommendationResponse rec = new BookAiRecommendationResponse();
        rec.setId(book.getId());
        rec.setRelevance(relevance);
        rec.setWhy(why);
        rec.applyBook(book);
        rec.setRrfScore(0.0328);
        return rec;
    }

    private static int countOccurrences(String html, String token) {
        return html.split(java.util.regex.Pattern.quote(token), -1).length - 1;
    }

    @Test
    @DisplayName("둘러보기: 정렬 탭과 sort를 유지하는 페이지 링크, 레이아웃(로고·GitHub·출처)과 사용 모델 안내를 렌더링한다")
    void index_browseMode() {
        List<BookSearchResponse> books = List.of(book(1L, "첫 번째 책"), book(2L, "두 번째 책"));
        Page<BookSearchResponse> page = new PageImpl<>(books, PageRequest.of(0, PAGE_SIZE), TOTAL_BOOKS);

        String html = render("index/index",
                indexVars(new BookSearchRequest(null, null, null, null), true, false, books, page));

        assertThat(html).contains("찾고 싶은 책을", "둘러보기", "최근 출간", "리뷰 많은 순");
        assertThat(html).contains("href=\"/?sort=RECENT\"", "href=\"/?sort=POPULAR\"");
        assertThat(html).contains("href=\"/?page=1&amp;sort=POPULAR\"");
        assertThat(html).contains("총 <strong>1,000</strong>권");
        assertThat(html).contains("app-book-grid", "첫 번째 책", "출판사 정보 없음 · 2023", "리뷰 7", "4.5");
        // 기본 검색 방식은 RAG
        assertThat(html).containsPattern("id=\"st-rag\" value=\"RAG\"[^>]*checked=\"checked\"");
        assertThat(html).contains("AI Library", "/css/app.css",
                "href=\"https://github.com/nhn-academy-marco/ai-library\" class=\"app-nav-link\" target=\"_blank\" rel=\"noopener\" aria-label=\"GitHub 저장소\"",
                "<span class=\"d-none d-sm-inline\">GitHub</span>",
                "https://www.data4library.kr/", "Spring AI 1.1.2");
        assertThat(html).doesNotContain("관련도 상위", "tree/main/docs", "교재", "Demo", "app-nav-link active");
        // 사용 모델 안내: 세그먼트 아래 안내와 RAG·벡터 카드
        assertThat(html).contains("AI 추천 모델: Ollama · qwen2.5:latest / 임베딩: bge-m3",
                "LLM Ollama · qwen2.5:latest", "임베딩 bge-m3");
    }

    @Test
    @DisplayName("하이브리드 검색 결과: '관련도 상위 N건' 안내와 검색 조건을 유지하는 페이지 링크를 렌더링한다")
    void index_hybridSearch() {
        List<BookSearchResponse> books = List.of(book(1L, "자바의 정석"));
        Page<BookSearchResponse> page = new PageImpl<>(books, PageRequest.of(0, PAGE_SIZE), 150);
        Map<String, Object> vars = indexVars(new BookSearchRequest("자바", null, SearchType.HYBRID, null),
                false, true, books, page);
        vars.put("searchTime", 0.3);

        String html = render("index/index", vars);

        assertThat(html).contains("검색 결과", "관련도 상위 150건");
        assertThat(html).contains("searchType=HYBRID");
        assertThat(html).containsPattern("id=\"st-hybrid\" value=\"HYBRID\"[^>]*checked=\"checked\"");
        assertThat(html).doesNotContain("리뷰 많은 순", "app-ai-panel");
    }

    @Test
    @DisplayName("RAG 캐시 적중: AI 추천 패널(사유·연관성·모델) 아래에 하이브리드 결과 전체(30건 중 24건)와 페이징을 렌더링한다")
    void index_ragCacheHit_showsPanelAndFullHybridList() {
        List<BookSearchResponse> all = books(30);
        List<BookSearchResponse> firstPage = all.subList(0, PAGE_SIZE);
        Page<BookSearchResponse> page = new PageImpl<>(firstPage, PageRequest.of(0, PAGE_SIZE), all.size());
        Map<String, Object> vars = indexVars(new BookSearchRequest("위로", null, SearchType.RAG, null),
                false, true, firstPage, page);
        vars.put("aiRecommendations", List.of(recommendation(all.get(0), 92, "지친 마음을 다독이는 에세이입니다.")));
        vars.put("aiPending", false);
        vars.put("searchTime", 0.2);

        String html = render("index/index", vars);

        assertThat(html).contains("app-ai-panel", "AI 추천", "92%", "지친 마음을 다독이는 에세이입니다.", "RRF 0.0328");
        assertThat(html).contains("qwen2.5:latest로 생성", "data-pending=\"false\"");
        assertThat(html).contains("id=\"rec-cover-1\"", "<strong id=\"rec-title-1\">하이브리드 결과 1</strong>", "id=\"rec-isbn-1\">979115675921<");
        assertThat(html).doesNotContain("AI가 후보를 검토하고 있습니다");
        assertThat(html).contains("관련도 상위 30건");
        assertThat(countOccurrences(html, "class=\"app-book-card\"")).isEqualTo(PAGE_SIZE);
        assertThat(html).contains("class=\"pagination\"", "page=1&amp;isbn=&amp;keyword=%EC%9C%84%EB%A1%9C&amp;searchType=RAG");
    }

    @Test
    @DisplayName("RAG 캐시 미스: 목록과 함께 'AI가 후보를 검토하고 있습니다' 로딩 상태와 폴링 정보(data-*)를 렌더링한다")
    void index_ragPending_showsLoadingPanel() {
        List<BookSearchResponse> all = books(30);
        Page<BookSearchResponse> page = new PageImpl<>(all.subList(0, PAGE_SIZE), PageRequest.of(0, PAGE_SIZE), all.size());
        Map<String, Object> vars = indexVars(new BookSearchRequest("위로가 되는 책", null, SearchType.RAG, null),
                false, true, all.subList(0, PAGE_SIZE), page);
        vars.put("aiRecommendations", List.of());
        vars.put("aiPending", true);

        String html = render("index/index", vars);

        assertThat(html).contains("id=\"ai-panel\"", "AI가 후보를 검토하고 있습니다",
                "data-pending=\"true\"", "data-keyword=\"위로가 되는 책\"", "data-endpoint=\"/api/recommendations\"");
        assertThat(html).contains("AI 추천을 불러오지 못했습니다", "POLL_TIMEOUT_MS = 90000");
        assertThat(countOccurrences(html, "class=\"app-book-card\"")).isEqualTo(PAGE_SIZE);
        assertThat(html).contains("class=\"pagination\"");
    }

    @Test
    @DisplayName("RAG 2페이지: 하이브리드 목록의 2페이지와 이전 페이지 링크를 렌더링한다")
    void index_ragSecondPage_showsPagination() {
        List<BookSearchResponse> all = books(30);
        List<BookSearchResponse> secondPage = all.subList(PAGE_SIZE, all.size());
        Page<BookSearchResponse> page = new PageImpl<>(secondPage, PageRequest.of(1, PAGE_SIZE), all.size());
        Map<String, Object> vars = indexVars(new BookSearchRequest("위로", null, SearchType.RAG, null),
                false, true, secondPage, page);
        vars.put("aiRecommendations", List.of(recommendation(all.get(0), 92, "사유")));

        String html = render("index/index", vars);

        assertThat(countOccurrences(html, "class=\"app-book-card\"")).isEqualTo(6);
        assertThat(html).contains("class=\"pagination\"", "page=0&amp;isbn=&amp;keyword=%EC%9C%84%EB%A1%9C&amp;searchType=RAG");
        // 추천 패널은 목록 페이지와 관계없이 도서 정보를 직접 갖고 있으므로 2페이지에서도 제목이 나옵니다.
        assertThat(html).contains("<strong id=\"rec-title-1\">하이브리드 결과 1</strong>");
    }

    @Test
    @DisplayName("RAG 추천 준비됨(빈 목록): 추천할 도서가 없다는 안내를 보여 준다")
    void index_ragReadyWithoutRecommendations() {
        List<BookSearchResponse> all = books(3);
        Page<BookSearchResponse> page = new PageImpl<>(all, PageRequest.of(0, PAGE_SIZE), all.size());
        Map<String, Object> vars = indexVars(new BookSearchRequest("위로", null, SearchType.RAG, null),
                false, true, all, page);
        vars.put("aiRecommendations", List.of());
        vars.put("aiPending", false);

        String html = render("index/index", vars);

        assertThat(html).contains("app-ai-panel", "추천할 만한 도서를 찾지 못했습니다");
        assertThat(html).doesNotContain("AI가 후보를 검토하고 있습니다");
    }

    private static Map<String, Object> detailVars(BookViewResponse book, String summary,
                                                  List<ReviewSummaryFormatter.Section> sections) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("book", book);
        vars.put("reviewSummary", summary);
        vars.put("reviewSummarySections", sections);
        vars.put("reviews", List.of());
        vars.put("reviewPage", Page.empty(PageRequest.of(0, 5)));
        return vars;
    }

    private static BookViewResponse bookWithoutPrice() {
        return new BookViewResponse(1L, null, "가격 없는 책", null, "저자", null,
                null, null, null, null, null, null, null, null);
    }

    @Test
    @DisplayName("상세: 가격·출판사·ISBN이 null이면 '가격 정보 없음'과 '-'를 보여 주고, 요약 항목을 정의 목록으로 렌더링한다")
    void bookDetail_nullFieldsAndSections() {
        String raw = "물론입니다! 요약해 드리겠습니다.\n**[장점]** 문장이 **따뜻합니다**.\n**[단점]** 분량이 짧습니다.\n[추천 대상] 위로가 필요한 독자\n[총평] 추천합니다.";
        String html = render("index/book-detail",
                detailVars(bookWithoutPrice(), ReviewSummaryFormatter.clean(raw), ReviewSummaryFormatter.parse(raw)));

        assertThat(html).contains("가격 정보 없음").doesNotContain("null원").doesNotContain(">null<");
        assertThat(html).contains("<dd>-</dd>");
        assertThat(html).contains("app-summary-list", "<dt>장점</dt>", "<dd>문장이 따뜻합니다.</dd>",
                "<dt>단점</dt>", "<dt>추천 대상</dt>", "<dt>총평</dt>");
        assertThat(html).doesNotContain("**", "물론입니다");
    }

    @Test
    @DisplayName("상세: 요약 항목이 없으면 정리된 텍스트를 그대로 보여 준다")
    void bookDetail_plainSummary() {
        String raw = "## 요약\n**독자들은** 이 책을 좋아합니다.";
        String html = render("index/book-detail",
                detailVars(bookWithoutPrice(), ReviewSummaryFormatter.clean(raw), ReviewSummaryFormatter.parse(raw)));

        assertThat(html).contains("독자들은 이 책을 좋아합니다.");
        assertThat(html).doesNotContain("app-summary-list", "**", "##");
    }

    @Test
    @DisplayName("상세: 가격이 있으면 천 단위 구분 기호와 '원'을 붙인다")
    void bookDetail_price() {
        BookViewResponse book = new BookViewResponse(1L, "9791156759270", "책", null, "저자", "출판사",
                null, LocalDate.of(2021, 1, 1), BigDecimal.valueOf(16000), null, "소개", null, null, null);
        String html = render("index/book-detail", detailVars(book, "아직 등록된 리뷰가 없습니다.", List.of()));

        assertThat(html).contains("16,000원", "2021-01-01").doesNotContain("가격 정보 없음");
    }
}
