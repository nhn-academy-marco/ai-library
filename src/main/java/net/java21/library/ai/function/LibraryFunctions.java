package net.java21.library.ai.function;

import net.java21.library.ai.function.dto.*;
import net.java21.library.core.book.dto.BookSearchResponse;
import net.java21.library.core.book.service.search.BookSearchService;
import net.java21.library.external.opennaru.client.LibraryInfoNaruApiClient;
import net.java21.library.external.opennaru.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

// DTO import 추가
import static net.java21.library.ai.function.dto.BookExistRequest.*;
import static net.java21.library.ai.function.dto.BookExistResult.*;

/**
 * 도서관정보나루 API용 Spring AI Function Calling 구현
 * <p>
 * LLM이 도서관정보나루 API를 직접 호출할 수 있게 합니다.
 * 내부 DB 검색 후 결과가 없으면 도서관정보나루 API를 Fallback으로 호출합니다.
 * </p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LibraryFunctions {

    private final LibraryInfoNaruApiClient apiClient;
    private final BookSearchService bookSearchService;

    /**
     * 1. 도서 검색 Function (내부 DB → 도서관정보나루 Fallback)
     */
    public Function<BookSearchRequest, BookSearchResult> searchBooks() {
        return request -> {
            long startTime = System.currentTimeMillis();
            log.info("┌─────────────────────────────────────────────────────┐");
            log.info("│ [Function: SearchBooks] 📚 도서 검색 시작              │");
            log.info("├─────────────────────────────────────────────────────┤");
            log.info("│ 요청 파라미터:                                        │");
            log.info("│   - query (검색어): {}                              │", request.query());
            log.info("│   - limit (최대 결과): {}                            │", request.limit());
            log.info("└─────────────────────────────────────────────────────┘");

            try {
                // 1단계: 내부 DB 검색
                log.info("┌─────────────────────────────────────────────────────┐");
                log.info("│ [Step 1] 내부 DB 검색 시작                           │");
                log.info("└─────────────────────────────────────────────────────┘");

                long dbStart = System.currentTimeMillis();
                List<BookSearchResult.BookItem> internalBooks = searchInternalDb(request.query(), request.limit());
                long dbElapsed = System.currentTimeMillis() - dbStart;

                log.info("┌─────────────────────────────────────────────────────┐");
                log.info("│ [Step 1] 내부 DB 검색 완료                           │");
                log.info("│   - 소요시간: {}ms                                   │", dbElapsed);
                log.info("│   - 검색된 도서 수: {}권                              │", internalBooks.size());
                log.info("│   - 데이터 출처: 내부 DB                             │");
                log.info("└─────────────────────────────────────────────────────┘");

                // 2단계: Fallback (내부 DB 결과가 없으면 도서관정보나루 API 호출)
                List<BookSearchResult.BookItem> items = internalBooks;
                String source = "내부 DB";

                if (internalBooks.isEmpty()) {
                    log.info("┌─────────────────────────────────────────────────────┐");
                    log.info("│ [Step 2] Fallback: 도서관정보나루 API 호출           │");
                    log.info("└─────────────────────────────────────────────────────┘");

                    long apiStart = System.currentTimeMillis();
                    List<LibraryBookInfo> libraryBooks = apiClient.searchBooksByTitle(request.query());
                    long apiElapsed = System.currentTimeMillis() - apiStart;

                    log.info("┌─────────────────────────────────────────────────────┐");
                    log.info("│ [Step 2] 도서관정보나루 API 호출 완료                 │");
                    log.info("│   - 소요시간: {}ms                                   │", apiElapsed);
                    log.info("│   - 검색된 도서 수: {}권                              │", libraryBooks.size());
                    log.info("│   - 데이터 출처: 도서관정보나루                       │");
                    log.info("└─────────────────────────────────────────────────────┘");

                    items = libraryBooks.stream()
                        .limit(request.limit())
                        .map(book -> {
                            log.debug("   도서: {} (ISBN: {})", book.title(), book.isbn13());
                            return BookSearchResult.BookItem.fromLibraryApi(
                                book.title(), book.authors(), book.publisher(), book.isbn13());
                        })
                        .toList();
                    source = "도서관정보나루";
                }

                long totalElapsed = System.currentTimeMillis() - startTime;
                log.info("┌─────────────────────────────────────────────────────┐");
                log.info("│ [Function: SearchBooks] ✅ 실행 완료                   │");
                log.info("│   - 총 소요시간: {}ms                                 │", totalElapsed);
                log.info("│   - 반환할 도서 수: {}권                              │", items.size());
                log.info("│   - 데이터 출처: {}                                   │", source);
                log.info("└─────────────────────────────────────────────────────┘");

                return BookSearchResult.from(items);

            } catch (Exception e) {
                long elapsed = System.currentTimeMillis() - startTime;
                log.error("┌─────────────────────────────────────────────────────┐");
                log.error("│ [Function: SearchBooks] ❌ 실행 실패 ({}ms)          │", elapsed);
                log.error("│   - 에러: {}                                        │", e.getMessage());
                log.error("└─────────────────────────────────────────────────────┘");
                throw e;
            }
        };
    }

    /**
     * 내부 DB에서 도서를 검색합니다.
     */
    private List<BookSearchResult.BookItem> searchInternalDb(String query, int limit) {
        try {
            net.java21.library.core.book.dto.BookSearchRequest searchRequest =
                new net.java21.library.core.book.dto.BookSearchRequest(
                    query,
                    null,
                    net.java21.library.core.book.domain.SearchType.HYBRID,
                    null,
                    false
                );

            PageRequest pageRequest = PageRequest.of(0, limit);
            var result = bookSearchService.searchBooks(pageRequest, searchRequest);

            return result.getBooks().getContent().stream()
                .map(book -> BookSearchResult.BookItem.fromInternalDb(
                    book.getId(),
                    book.getTitle(),
                    book.getAuthorName(),
                    book.getPublisherName(),
                    book.getIsbn()
                ))
                .toList();

        } catch (Exception e) {
            log.warn("[Function] 내부 DB 검색 실패: {}", e.getMessage());
            return List.of();
        }
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

            // 도서관별로 그룹화하여 중복 제거 (같은 도서관에서 최소 1권이라도 대출 가능하면 "대출 가능"으로 표시)
            Map<String, LoanAvailabilityResult.LibraryItem> libraryMap = new LinkedHashMap<>();
            for (LoanItemInfo item : loanItems) {
                String libName = item.libraryName();
                boolean isLoanAvailable = "Y".equalsIgnoreCase(item.loanAvailable());

                // 같은 도서관이 이미 있고, 현재 항목이 대출 가능하면 업데이트
                if (libraryMap.containsKey(libName)) {
                    LoanAvailabilityResult.LibraryItem existing = libraryMap.get(libName);
                    if (!existing.loanAvailable() && isLoanAvailable) {
                        libraryMap.put(libName, new LoanAvailabilityResult.LibraryItem(
                            item.libraryName(), item.location(), true, item.region()));
                    }
                } else {
                    // 새로운 도서관 추가
                    libraryMap.put(libName, new LoanAvailabilityResult.LibraryItem(
                        item.libraryName(), item.location(), isLoanAvailable, item.region()));
                }
            }

            List<LoanAvailabilityResult.LibraryItem> libraries = new ArrayList<>(libraryMap.values());
            log.info("[Function] 대출 가능 도서관 조회 완료: {}개 도서관 (중복 제거됨)", libraries.size());
            return new LoanAvailabilityResult(request.isbn13(), libraries.size(), libraries);
        };
    }

    /**
     * 3. 도서관 검색 Function
     */
    public Function<LibrarySearchRequest, LibrarySearchResult> searchLibraries() {
        return request -> {
            long startTime = System.currentTimeMillis();
            log.info("┌─────────────────────────────────────────────────────┐");
            log.info("│ [Function: SearchLibraries] 🏛️ 도서관 검색 시작         │");
            log.info("├─────────────────────────────────────────────────────┤");
            log.info("│ 요청 파라미터:                                        │");
            log.info("│   - libraryName (도서관명): {}                       │", request.libraryName());
            log.info("│   - region (지역 코드): {}                             │", request.region());
            log.info("└─────────────────────────────────────────────────────┘");

            try {
                List<LibraryInfo> libraries = apiClient.searchLibraries(request.libraryName(), request.region());

                long apiElapsed = System.currentTimeMillis() - startTime;
                log.info("┌─────────────────────────────────────────────────────┐");
                log.info("│ [Function: SearchLibraries] API 호출 완료              │");
                log.info("│   - 소요시간: {}ms                                   │", apiElapsed);
                log.info("│   - 검색된 도서관 수: {}개                             │", libraries.size());
                log.info("└─────────────────────────────────────────────────────┘");

                List<LibrarySearchResult.LibraryItem> items = libraries.stream()
                    .map(lib -> {
                        log.debug("   도서관: {} (libCode: {})", lib.libName(), lib.libCode());
                        return new LibrarySearchResult.LibraryItem(
                            lib.libCode(), lib.libName(), lib.address(), lib.tel(), lib.homepage());
                    })
                    .toList();

                long totalElapsed = System.currentTimeMillis() - startTime;
                log.info("┌─────────────────────────────────────────────────────┐");
                log.info("│ [Function: SearchLibraries] ✅ 실행 완료                │");
                log.info("│   - 총 소요시간: {}ms                                 │", totalElapsed);
                log.info("│   - 반환할 도서관 수: {}개                             │", items.size());
                log.info("└─────────────────────────────────────────────────────┘");

                return new LibrarySearchResult(items.size(), items);

            } catch (Exception e) {
                long elapsed = System.currentTimeMillis() - startTime;
                log.error("┌─────────────────────────────────────────────────────┐");
                log.error("│ [Function: SearchLibraries] ❌ 실행 실패 ({}ms)        │", elapsed);
                log.error("│   - 에러: {}                                        │", e.getMessage());
                log.error("└─────────────────────────────────────────────────────┘");
                throw e;
            }
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

    /**
     * 8. 도서 소장 여부 조회 Function (bookExist API)
     * <p>
     * 특정 도서관에 도서가 소장되어 있는지 확인합니다.
     * </p>
     */
    public Function<BookExistRequest, BookExistResult> checkBookExists() {
        return request -> {
            long startTime = System.currentTimeMillis();
            log.info("┌─────────────────────────────────────────────────────┐");
            log.info("│ [Function: CheckBookExists] ✅ 도서 소장 여부 확인       │");
            log.info("├─────────────────────────────────────────────────────┤");
            log.info("│ 요청 파라미터:                                        │");
            log.info("│   - libCode (도서관 코드): {}                         │", request.libCode());
            log.info("│   - isbn13 (ISBN): {}                                 │", request.isbn13());
            log.info("└─────────────────────────────────────────────────────┘");

            try {
                net.java21.library.external.opennaru.dto.BookExistInfo info =
                    apiClient.checkBookExists(request.libCode(), request.isbn13());

                long apiElapsed = System.currentTimeMillis() - startTime;
                log.info("┌─────────────────────────────────────────────────────┐");
                log.info("│ [Function: CheckBookExists] bookExist API 호출 완료     │");
                log.info("│   - 소요시간: {}ms                                   │", apiElapsed);
                log.info("│   - 소장 여부: {}                                       │", info != null ? info.exists() : false);
                if (info != null && info.exists()) {
                    log.info("│   - 대출 가능: {}                                       │", "Y".equals(info.loanAvailable()) ? "✅" : "❌");
                }
                log.info("└─────────────────────────────────────────────────────┘");

                BookExistResult result = BookExistResult.from(request.libCode(), null, info);

                long totalElapsed = System.currentTimeMillis() - startTime;
                log.info("┌─────────────────────────────────────────────────────┐");
                log.info("│ [Function: CheckBookExists] ✅ 실행 완료                  │");
                log.info("│   - 총 소요시간: {}ms                                 │", totalElapsed);
                log.info("│   - exists: {}                                        │", result.exists());
                log.info("│   - loanAvailable: {}                                 │", result.loanAvailable());
                log.info("└─────────────────────────────────────────────────────┘");

                return result;

            } catch (Exception e) {
                long elapsed = System.currentTimeMillis() - startTime;
                log.error("┌─────────────────────────────────────────────────────┐");
                log.error("│ [Function: CheckBookExists] ❌ 실행 실패 ({}ms)        │", elapsed);
                log.error("│   - 에러: {}                                        │", e.getMessage());
                log.error("└─────────────────────────────────────────────────────┘");
                throw e;
            }
        };
    }
}
