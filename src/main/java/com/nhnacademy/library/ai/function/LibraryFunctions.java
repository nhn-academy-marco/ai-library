package com.nhnacademy.library.ai.function;

import com.nhnacademy.library.ai.function.dto.*;
import com.nhnacademy.library.external.opennaru.client.LibraryInfoNaruApiClient;
import com.nhnacademy.library.external.opennaru.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

/**
 * 도서관정보나루 API용 Spring AI Function Calling 구현
 * <p>
 * LLM이 도서관정보나루 API를 직접 호출할 수 있게 합니다.
 * </p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LibraryFunctions {

    private final LibraryInfoNaruApiClient apiClient;

    /**
     * 1. 도서 검색 Function
     */
    public Function<BookSearchRequest, BookSearchResult> searchBooks() {
        return request -> {
            log.info("[Function] 도서 검색: query={}", request.query());
            List<LibraryBookInfo> libraryBooks = apiClient.searchBooksByTitle(request.query());
            List<BookSearchResult.BookItem> items = libraryBooks.stream()
                .limit(request.limit())
                .map(book -> BookSearchResult.BookItem.from(
                    0L, book.title(), book.authors(), book.publisher(), book.isbn13()))
                .toList();
            log.info("[Function] 도서 검색 완료: {}권", items.size());
            return BookSearchResult.from(items);
        };
    }

    /**
     * 2. 대출 가능 도서관 조회 Function
     */
    public Function<LoanAvailabilityRequest, LoanAvailabilityResult> getLibrariesWithBook() {
        return request -> {
            log.info("[Function] 대출 가능 도서관 조회: isbn={}, region={}", request.isbn13(), request.region());
            List<LoanItemInfo> loanItems = request.region() != null
                ? apiClient.searchLoanItemsByIsbn(request.isbn13(), request.region())
                : apiClient.searchLoanItemsByIsbn(request.isbn13());
            List<LoanAvailabilityResult.LibraryItem> libraries = loanItems.stream()
                .map(item -> new LoanAvailabilityResult.LibraryItem(
                    item.libraryName(), item.location(),
                    "Y".equalsIgnoreCase(item.loanAvailable()), item.region()))
                .toList();
            log.info("[Function] 대출 가능 도서관 조회 완료: {}개 도서관", libraries.size());
            return new LoanAvailabilityResult(request.isbn13(), libraries.size(), libraries);
        };
    }

    /**
     * 3. 도서관 검색 Function
     */
    public Function<LibrarySearchRequest, LibrarySearchResult> searchLibraries() {
        return request -> {
            log.info("[Function] 도서관 검색: name={}, region={}", request.libraryName(), request.region());
            List<LibraryInfo> libraries = apiClient.searchLibraries(request.libraryName(), request.region());
            List<LibrarySearchResult.LibraryItem> items = libraries.stream()
                .map(lib -> new LibrarySearchResult.LibraryItem(
                    lib.libCode(), lib.libName(), lib.address(), lib.tel(), lib.homepage()))
                .toList();
            log.info("[Function] 도서관 검색 완료: {}개 도서관", items.size());
            return new LibrarySearchResult(items.size(), items);
        };
    }

    /**
     * 4. 도서 상세 정보 조회 Function
     */
    public Function<BookDetailRequest, BookDetailResult> getBookDetail() {
        return request -> {
            log.info("[Function] 도서 상세 조회: isbn={}", request.isbn13());
            BookDetailInfo detail = apiClient.getBookDetail(request.isbn13(), request.includeLoanInfo());
            if (detail == null) {
                log.warn("[Function] 도서 상세 정보 없음: isbn={}", request.isbn13());
                return null;
            }
            BookDetailResult.LoanInfo loanInfo = null;
            if (detail.loanInfo() != null) {
                loanInfo = new BookDetailResult.LoanInfo(
                    detail.loanInfo().totalLoanCount(), detail.loanInfo().maleLoanCount(),
                    detail.loanInfo().femaleLoanCount(), detail.loanInfo().teensLoanCount(),
                    detail.loanInfo().twentiesLoanCount(), detail.loanInfo().thirtiesLoanCount(),
                    detail.loanInfo().fortiesLoanCount(), detail.loanInfo().fiftiesLoanCount(),
                    detail.loanInfo().sixtiesLoanCount());
            }
            log.info("[Function] 도서 상세 조회 완료: {}", detail.bookname());
            return new BookDetailResult(detail.isbn13(), detail.bookname(), detail.authors(),
                detail.publisher(), detail.publicationDate(), detail.imageUrl(), detail.description(),
                detail.category(), detail.loanCount(), loanInfo);
        };
    }

    /**
     * 5. 인기대출도서 조회 Function
     */
    public Function<PopularBooksRequest, PopularBooksResult> getPopularBooks() {
        return request -> {
            log.info("[Function] 인기대출도서 조회: {} ~ {}, region={}",
                request.startDt(), request.endDt(), request.region());
            List<PopularBookInfo> popularBooks = apiClient.getPopularBooks(
                request.startDt(), request.endDt(), request.region());
            List<PopularBooksResult.BookItem> items = popularBooks.stream()
                .map(book -> new PopularBooksResult.BookItem(
                    book.isbn13(), book.bookName(), book.authors(), book.publisher(),
                    book.loanCount(), book.ranking()))
                .toList();
            log.info("[Function] 인기대출도서 조회 완료: {}권", items.size());
            return new PopularBooksResult(request.startDt() + " ~ " + request.endDt(), items.size(), items);
        };
    }

    /**
     * 6. 대출 급상승 도서 조회 Function
     */
    public Function<HotTrendRequest, HotTrendResult> getHotTrendBooks() {
        return request -> {
            log.info("[Function] 대출 급상승 도서 조회: date={}", request.searchDate());
            List<HotTrendBookInfo> hotBooks = apiClient.getHotTrendBooks(request.searchDate());
            List<HotTrendResult.BookItem> items = hotBooks.stream()
                .map(book -> new HotTrendResult.BookItem(
                    book.isbn13(), book.bookName(), book.authors(), book.publisher(),
                    book.currentRank(), book.rankChange()))
                .toList();
            log.info("[Function] 대출 급상승 도서 조회 완료: {}권", items.size());
            return new HotTrendResult(request.searchDate(), items.size(), items);
        };
    }

    /**
     * 7. 추천도서 조회 Function
     */
    public Function<RecommendRequest, RecommendResult> getRecommendedBooks() {
        return request -> {
            log.info("[Function] 추천도서 조회: isbn={}, type={}", request.isbn13(), request.type());
            List<RecommendedBookInfo> recommendedBooks = apiClient.getRecommendedBooks(request.isbn13(), request.type());
            List<RecommendResult.BookItem> items = recommendedBooks.stream()
                .map(book -> new RecommendResult.BookItem(
                    book.isbn13(), book.bookName(), book.authors(), book.publisher(), book.rank()))
                .toList();
            log.info("[Function] 추천도서 조회 완료: {}권", items.size());
            return new RecommendResult(request.isbn13(), request.type(), items.size(), items);
        };
    }
}
