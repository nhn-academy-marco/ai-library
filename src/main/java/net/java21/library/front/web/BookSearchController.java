package net.java21.library.front.web;
import net.java21.library.core.book.domain.BrowseSort;
import net.java21.library.core.book.domain.SearchType;
import net.java21.library.core.book.dto.BookCoverTarget;
import net.java21.library.core.book.dto.BookSearchRequest;
import net.java21.library.core.book.dto.BookViewResponse;
import net.java21.library.core.book.dto.BookSearchResponse;
import net.java21.library.core.book.dto.BookSearchResult;

import java.util.List;
import java.util.regex.Pattern;

import net.java21.library.core.book.service.cover.BookCoverService;
import net.java21.library.core.book.service.search.BookSearchService;
import net.java21.library.core.review.dto.ReviewResponse;
import net.java21.library.core.review.service.ReviewService;
import net.java21.library.core.review.service.ReviewSummaryFormatter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.util.StringUtils;

/**
 * 도서 검색 웹 컨트롤러
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class BookSearchController {

    private final BookSearchService bookSearchService;
    private final ReviewService reviewService;
    private final BookCoverService bookCoverService;

    private static final String INDEX_VIEW = "index/index";
    private static final Pattern ISBN13_PATTERN = Pattern.compile("^97[89]\\d{10}$");
    private static final Pattern ISBN10_PATTERN = Pattern.compile("^\\d{9}[\\dX]$");

    /**
     * 메인 페이지를 반환합니다.
     *
     * <p>검색어(keyword)와 ISBN이 모두 비어 있으면 검색 전략을 거치지 않고 둘러보기 목록(최근 출간·리뷰 많은 순)을
     * 보여 줍니다. 검색 전략은 관련도 순위를 매기는 용도라 검색어 없이 부르면 의미 없는 상위 후보만 나오기 때문입니다.</p>
     *
     * @param bookSearchRequest 검색 요청 조건
     * @param sort              둘러보기 정렬 기준 (RECENT 기본, POPULAR)
     * @param pageable          페이징 정보 (기본값 24)
     * @param model             뷰 모델
     * @return index 뷰 이름
     */
    @GetMapping("/")
    public String index(@Valid @ModelAttribute BookSearchRequest bookSearchRequest,
                        BindingResult bindingResult,
                        @RequestParam(name = "sort", required = false) String sort,
                        @PageableDefault(size = 24) Pageable pageable,
                        Model model) {

        log.debug("GET / with request: {}, sort: {}", bookSearchRequest, sort);

        // 검색창에 입력한 값을 그대로 다시 보여 주고, ISBN 형식이면 ISBN 검색으로 바꿉니다.
        model.addAttribute("query", StringUtils.hasText(bookSearchRequest.keyword())
                ? bookSearchRequest.keyword() : bookSearchRequest.isbn());
        bookSearchRequest = resolveIsbnQuery(bookSearchRequest);

        BrowseSort browseSort = BrowseSort.from(sort);
        boolean browseMode = isBrowseMode(bookSearchRequest);
        model.addAttribute("request", bookSearchRequest);
        model.addAttribute("browseMode", browseMode);
        model.addAttribute("sort", browseSort);
        model.addAttribute("sorts", BrowseSort.values());
        model.addAttribute("rankedResult", !browseMode && isRankedSearch(bookSearchRequest.searchType()));

        if (bindingResult.hasErrors()) {
            model.addAttribute("books", List.of());
            model.addAttribute("page", null);
            return INDEX_VIEW;
        }

        // 'sort' 파라미터는 둘러보기 정렬 기준이므로 Pageable의 정렬(Sort)로 해석된 값은 버립니다.
        Pageable pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        long startTime = System.currentTimeMillis();
        Page<BookSearchResponse> books;
        if (browseMode) {
            books = bookSearchService.browseBooks(pageRequest, browseSort);
        } else {
            BookSearchResult searchResult = bookSearchService.searchBooks(pageRequest, bookSearchRequest);
            books = searchResult.getBooks();
            model.addAttribute("aiRecommendations", searchResult.getAiResponse());
            model.addAttribute("keywordFallback", searchResult.isKeywordFallback());
            model.addAttribute("searchTime", (System.currentTimeMillis() - startTime) / 1000.0);
        }

        // 표지가 비어 있는 도서는 비동기로 보강 요청 (응답 시간에 영향 없음)
        bookCoverService.requestEnrichment(books.getContent().stream()
                .map(BookCoverTarget::from)
                .toList());

        model.addAttribute("books", books.getContent());
        model.addAttribute("page", books);
        return INDEX_VIEW;
    }

    /**
     * 검색어가 ISBN(10자리 또는 13자리, 하이픈·공백 허용)이면 ISBN 검색 요청으로 바꿉니다.
     * DB에는 13자리 ISBN이 저장되어 있으므로 10자리는 13자리로 변환하고, 정확히 일치하는 키워드 검색으로 처리합니다.
     */
    static BookSearchRequest resolveIsbnQuery(BookSearchRequest request) {
        if (StringUtils.hasText(request.isbn()) || !StringUtils.hasText(request.keyword())) {
            return request;
        }
        String compact = request.keyword().replaceAll("[\\s-]", "").toUpperCase();
        String isbn13;
        if (ISBN13_PATTERN.matcher(compact).matches()) {
            isbn13 = compact;
        } else if (ISBN10_PATTERN.matcher(compact).matches()) {
            isbn13 = toIsbn13(compact);
        } else {
            return request;
        }
        return new BookSearchRequest(null, isbn13, SearchType.KEYWORD, null);
    }

    /**
     * ISBN-10을 ISBN-13으로 변환합니다. (앞에 978을 붙이고 체크 숫자를 다시 계산)
     */
    private static String toIsbn13(String isbn10) {
        String body = "978" + isbn10.substring(0, 9);
        int sum = 0;
        for (int i = 0; i < body.length(); i++) {
            int digit = body.charAt(i) - '0';
            sum += (i % 2 == 0) ? digit : digit * 3;
        }
        int check = (10 - sum % 10) % 10;
        return body + check;
    }

    private static boolean isBrowseMode(BookSearchRequest request) {
        return !StringUtils.hasText(request.keyword()) && !StringUtils.hasText(request.isbn());
    }

    /**
     * 하이브리드·RAG는 상위 후보(최대 100건씩)를 RRF로 합친 결과만 다루므로 전체 건수가 아닌 상위 N건입니다.
     */
    private static boolean isRankedSearch(SearchType searchType) {
        return searchType == SearchType.HYBRID || searchType == SearchType.RAG;
    }

    /**
     * 도서 상세 페이지를 반환합니다.
     *
     * @param id    도서 ID
     * @param model 뷰 모델
     * @return book-detail 뷰 이름
     */
    @GetMapping("/books/{id}")
    public String view(@PathVariable("id") Long id,
                        @PageableDefault(size = 5) Pageable pageable,
                        Model model) {
        log.debug("GET /books/{}", id);
        BookViewResponse book = bookSearchService.getBook(id);
        model.addAttribute("book", book);
        bookCoverService.requestEnrichment(List.of(BookCoverTarget.from(book)));

        // 리뷰 요약 정보 조회 (캐싱 테이블 우선 참조)
        // LLM이 덧붙인 Markdown 기호·인사말을 걷어 내고, [장점]/[단점] 같은 항목이 있으면 나눠서 보여 줍니다.
        String reviewSummary = bookSearchService.getReviewSummary(id);
        model.addAttribute("reviewSummary", ReviewSummaryFormatter.clean(reviewSummary));
        model.addAttribute("reviewSummarySections", ReviewSummaryFormatter.parse(reviewSummary));

        // 리뷰 통계 정보 추가
        bookSearchService.getReviewSummaryEntity(id).ifPresent(summary -> 
            model.addAttribute("bookSummary", summary)
        );

        // 리뷰 목록 추가 (페이징)
        Page<ReviewResponse> reviews = reviewService.getReviews(id, pageable);
        model.addAttribute("reviews", reviews.getContent());
        model.addAttribute("reviewPage", reviews);
        
        return "index/book-detail";
    }

}
