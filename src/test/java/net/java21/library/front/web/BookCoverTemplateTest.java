package net.java21.library.front.web;

import nz.net.ultraq.thymeleaf.layoutdialect.LayoutDialect;
import net.java21.library.core.book.domain.SearchType;
import net.java21.library.core.book.dto.BookAiRecommendationResponse;
import net.java21.library.core.book.dto.BookSearchRequest;
import net.java21.library.core.book.dto.BookSearchResponse;
import net.java21.library.core.book.dto.BookViewResponse;
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
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 표지 프래그먼트를 쓰는 템플릿이 Spring Context 없이 렌더링되는지 확인합니다.
 */
class BookCoverTemplateTest {

    private static final int PALETTE_SIZE = 8;
    private static final String TITLE = "너에게 목소리를 보낼게";
    private static final String COVER_URL = "https://image.aladin.co.kr/cover.jpg";

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

    private static BookSearchResponse searchBook(Long id, String imageUrl) {
        return new BookSearchResponse(id, "979115675927" + id, TITLE, null, "이용신 (지은이)", "푸른숲",
                BigDecimal.valueOf(16000), LocalDate.of(2021, 1, 1), imageUrl, "소개");
    }

    @Test
    @DisplayName("검색 결과: 이미지가 없으면 제목 해시 팔레트의 생성 표지, 있으면 lazy·no-referrer 이미지를 렌더링한다")
    void index_rendersGeneratedCoverAndImage() {
        List<BookSearchResponse> books = List.of(searchBook(1L, null), searchBook(2L, COVER_URL));
        Page<BookSearchResponse> page = new PageImpl<>(books, PageRequest.of(0, 24), books.size());
        BookAiRecommendationResponse rec = new BookAiRecommendationResponse();
        rec.setId(1L);
        rec.setRelevance(90);

        Map<String, Object> vars = new HashMap<>();
        vars.put("books", books);
        vars.put("page", page);
        vars.put("aiRecommendations", List.of(rec));
        vars.put("request", new BookSearchRequest("목소리", null, SearchType.KEYWORD, null));
        vars.put("searchTime", 0.1);

        String html = render("index/index", vars);

        int expectedPalette = Math.floorMod(TITLE.hashCode(), PALETTE_SIZE);
        assertThat(html).contains("book-cover-generated book-cover-p" + expectedPalette);
        assertThat(html).contains("<div class=\"book-cover-title\">" + TITLE + "</div>");
        assertThat(html).contains("src=\"" + COVER_URL + "\"");
        assertThat(html).contains("referrerpolicy=\"no-referrer\"", "loading=\"lazy\"", "BookCover.fallback(this)");
        assertThat(html).contains("id=\"rec-cover-1\"");
        assertThat(html).contains("/js/book-cover.js", "/css/book-cover.css");
        assertThat(html).doesNotContain("이미지 없음");
    }

    @Test
    @DisplayName("상세: 이미지가 없으면 큰 생성 표지를 렌더링한다")
    void bookDetail_rendersGeneratedCover() {
        BookViewResponse book = new BookViewResponse(1L, "9791156759270", TITLE, null, "이용신", "푸른숲",
                null, null, BigDecimal.valueOf(16000), null, "소개", "", OffsetDateTime.now(), null);

        Map<String, Object> vars = new HashMap<>();
        vars.put("book", book);
        vars.put("reviews", List.of());
        vars.put("reviewPage", Page.empty(PageRequest.of(0, 5)));

        String html = render("index/book-detail", vars);

        assertThat(html).contains("book-cover book-cover-lg");
        assertThat(html).contains("book-cover-generated book-cover-p" + Math.floorMod(TITLE.hashCode(), PALETTE_SIZE));
        assertThat(html).contains("<div class=\"book-cover-author\">이용신</div>");
    }
}
