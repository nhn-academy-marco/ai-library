package com.nhnacademy.library.front.web;
import com.nhnacademy.library.core.book.dto.BookSearchRequest;
import com.nhnacademy.library.core.book.dto.BookSearchResponse;
import com.nhnacademy.library.core.book.dto.BookViewResponse;
import com.nhnacademy.library.core.book.service.BookSearchService;
import java.util.List;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 도서 검색 웹 컨트롤러
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class BookSearchController {

    private final BookSearchService bookSearchService;

    /**
     * 메인 검색 페이지를 반환합니다.
     *
     * @param bookSearchRequest 검색 요청 조건
     * @param pageable          페이징 정보 (기본값 24)
     * @param model             뷰 모델
     * @return index 뷰 이름
     */
    @GetMapping("/")
    public String index(@Valid @ModelAttribute BookSearchRequest bookSearchRequest,
                        BindingResult bindingResult,
                        @PageableDefault(size = 24) Pageable pageable,
                        Model model) {

        log.debug("GET / with request: {}", bookSearchRequest);

        if (bindingResult.hasErrors()) {
            model.addAttribute("books", List.of());
            model.addAttribute("page", null);
            model.addAttribute("request", bookSearchRequest);
            return "index/index";
        }

        long startTime = System.currentTimeMillis();
        BookSearchService.SearchResult searchResult = bookSearchService.searchBooks(pageable, bookSearchRequest);
        long endTime = System.currentTimeMillis();

        model.addAttribute("books", searchResult.getBooks().getContent());
        model.addAttribute("page", searchResult.getBooks());
        model.addAttribute("aiRecommendations", searchResult.getAiResponse());
        model.addAttribute("request", bookSearchRequest);
        model.addAttribute("searchTime", (endTime - startTime) / 1000.0);

        return "index/index";
    }

    /**
     * 도서 상세 페이지를 반환합니다.
     *
     * @param id    도서 ID
     * @param model 뷰 모델
     * @return book-detail 뷰 이름
     */
    @GetMapping("/books/{id}")
    public String view(@PathVariable("id") Long id, Model model) {
        log.debug("GET /books/{}", id);
        BookViewResponse book = bookSearchService.getBook(id);
        model.addAttribute("book", book);
        return "index/book-detail";
    }

}
