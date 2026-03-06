package com.nhnacademy.library.ai.function;

import com.nhnacademy.library.ai.function.dto.*;
import com.nhnacademy.library.core.book.dto.BookSearchResponse;
import com.nhnacademy.library.core.book.service.search.BookSearchService;
import com.nhnacademy.library.external.opennaru.client.LibraryInfoNaruApiClient;
import com.nhnacademy.library.external.opennaru.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 도서관정보나루 API용 Spring AI @Tool 애노테이션 기반 구현
 * <p>
 * Spring AI 1.1.2의 @Tool 애노테이션을 사용하여 Function Calling을 구현합니다.
 * LLM이 도서관정보나루 API를 직접 호출할 수 있게 합니다.
 * 내부 DB 검색 후 결과가 없으면 도서관정보나루 API를 Fallback으로 호출합니다.
 * </p>
 *
 * @see org.springframework.ai.model.function.FunctionCallback
 * @see org.springframework.ai.model.function.ToolContext
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LibraryTools {

    private final LibraryInfoNaruApiClient apiClient;
    private final BookSearchService bookSearchService;

    /**
     * 1. 도서 검색 Tool (내부 DB → 도서관정보나루 Fallback)
     * <p>
     * 도서관 시스템에서 도서를 검색합니다.
     * 제목으로 검색할 수 있으며, 검색어를 받아 도서 목록과 ISBN을 반환합니다.
     * 먼저 내부 DB를 검색하고, 결과가 없으면 도서관정보나루 API를 호출합니다.
     * </p>
     *
     * @param title 검색할 도서 제목 (검색어)
     * @param limit 최대 결과 수 (기본값: 10)
     * @return 도서 검색 결과 목록 (BookSearchResult)
     * @see org.springframework.ai.tool.annotation.Tool
     * @see org.springframework.ai.tool.annotation.ToolParam
     */
    @org.springframework.ai.tool.annotation.Tool(
        description = "도서관 시스템에서 도서를 검색합니다. " +
                     "제목으로 검색할 수 있으며, 검색어를 받아 도서 목록과 ISBN을 반환합니다. " +
                     "먼저 내부 DB를 검색하고, 결과가 없으면 도서관정보나루 API를 호출합니다."
    )
    public List<BookSearchResult.BookItem> searchBooks(
        @org.springframework.ai.tool.annotation.ToolParam(
            description = "검색할 도서 제목 또는 검색어"
        ) String title,
        @org.springframework.ai.tool.annotation.ToolParam(
            description = "최대 결과 수 (기본값: 10, 최대: 100)",
            required = false
        ) Integer limit
    ) {
        long startTime = System.currentTimeMillis();
        int actualLimit = (limit != null && limit > 0) ? Math.min(limit, 100) : 10;

        log.info("┌─────────────────────────────────────────────────────┐");
        log.info("│ [@Tool: SearchBooks] 📚 도서 검색 시작               │");
        log.info("├─────────────────────────────────────────────────────┤");
        log.info("│ 요청 파라미터:                                        │");
        log.info("│   - title (검색어): {}                               │", title);
        log.info("│   - limit (최대 결과): {}                            │", actualLimit);
        log.info("└─────────────────────────────────────────────────────┘");

        try {
            // 1단계: 내부 DB 검색
            log.info("┌─────────────────────────────────────────────────────┐");
            log.info("│ [Step 1] 내부 DB 검색 시작                           │");
            log.info("└─────────────────────────────────────────────────────┘");

            long dbStart = System.currentTimeMillis();
            List<BookSearchResult.BookItem> internalBooks = searchInternalDb(title, actualLimit);
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
                List<LibraryBookInfo> libraryBooks = apiClient.searchBooksByTitle(title);
                long apiElapsed = System.currentTimeMillis() - apiStart;

                log.info("┌─────────────────────────────────────────────────────┐");
                log.info("│ [Step 2] 도서관정보나루 API 호출 완료                 │");
                log.info("│   - 소요시간: {}ms                                   │", apiElapsed);
                log.info("│   - 검색된 도서 수: {}권                              │", libraryBooks.size());
                log.info("│   - 데이터 출처: 도서관정보나루                       │");
                log.info("└─────────────────────────────────────────────────────┘");

                items = libraryBooks.stream()
                    .limit(actualLimit)
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
            log.info("│ [@Tool: SearchBooks] ✅ 실행 완료                     │");
            log.info("│   - 총 소요시간: {}ms                                 │", totalElapsed);
            log.info("│   - 반환할 도서 수: {}권                              │", items.size());
            log.info("│   - 데이터 출처: {}                                   │", source);
            log.info("└─────────────────────────────────────────────────────┘");

            return items;

        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("┌─────────────────────────────────────────────────────┐");
            log.error("│ [@Tool: SearchBooks] ❌ 실행 실패 ({}ms)             │", elapsed);
            log.error("│   - 에러: {}                                        │", e.getMessage());
            log.error("└─────────────────────────────────────────────────────┘");
            throw e;
        }
    }

    /**
     * 내부 DB에서 도서를 검색합니다.
     */
    private List<BookSearchResult.BookItem> searchInternalDb(String query, int limit) {
        try {
            com.nhnacademy.library.core.book.dto.BookSearchRequest searchRequest =
                new com.nhnacademy.library.core.book.dto.BookSearchRequest(
                    query,
                    null,
                    com.nhnacademy.library.core.book.domain.SearchType.HYBRID,
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
            log.warn("[@Tool] 내부 DB 검색 실패: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 2. 대출 가능 도서관 조회 Tool
     * <p>
     * 특정 ISBN의 도서를 대출할 수 있는 도서관 목록을 조회합니다.
     * 지역 코드를 제공하면 해당 지역의 도서관만 필터링합니다.
     * </p>
     *
     * @param isbn13 도서 ISBN13
     * @param region 지역 코드 (예: 11=서울, 24=광주, 선택 사항)
     * @return 대출 가능 도서관 목록 (LoanAvailabilityResult)
     */
    @org.springframework.ai.tool.annotation.Tool(
        description = "특정 ISBN의 도서를 대출할 수 있는 도서관 목록을 조회합니다. " +
                     "지역 코드를 제공하면 해당 지역의 도서관만 필터링합니다. " +
                     "**중요**: region 파라미터에는 한글 지역명(광주, 서울 등)을 사용하지 말고 " +
                     "반드시 숫자 지역 코드를 사용하세요. (서울=11, 부산=21, 대구=22, 인천=23, 광주=24, 대전=25, 울산=26, 세종=29, 경기=31)"
    )
    public List<LoanAvailabilityResult.LibraryItem> getLibrariesWithBook(
        @org.springframework.ai.tool.annotation.ToolParam(
            description = "도서 ISBN13 (13자리 ISBN)"
        ) String isbn13,
        @org.springframework.ai.tool.annotation.ToolParam(
            description = "지역 코드 (숫자). 예: 서울=11, 부산=21, 대구=22, 인천=23, 광주=24, 대전=25, 울산=26, 세종=29, 경기=31. " +
                         "한글 지역명 사용 금지! 반드시 숫자 코드 사용할 것.",
            required = false
        ) String region
    ) {
        log.info("┌─────────────────────────────────────────────────────┐");
        log.info("│ [@Tool: GetLibrariesWithBook] 🏛️ 대출 가능 도서관 조회 │");
        log.info("├─────────────────────────────────────────────────────┤");
        log.info("│ 요청 파라미터:                                        │");
        log.info("│   - isbn13 (ISBN): {}                                │", isbn13);
        log.info("│   - region (지역 코드): {}                             │", region);
        log.info("└─────────────────────────────────────────────────────┘");

        try {
            List<LoanItemInfo> loanItems = region != null && !region.isBlank()
                ? apiClient.searchLoanItemsByIsbn(isbn13, region)
                : apiClient.searchLoanItemsByIsbn(isbn13);

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

            log.info("┌─────────────────────────────────────────────────────┐");
            log.info("│ [@Tool: GetLibrariesWithBook] ✅ 실행 완료             │");
            log.info("│   - 검색된 도서관 수: {}개                             │", libraries.size());
            log.info("└─────────────────────────────────────────────────────┘");

            return libraries;

        } catch (Exception e) {
            log.error("┌─────────────────────────────────────────────────────┐");
            log.error("│ [@Tool: GetLibrariesWithBook] ❌ 실행 실패             │");
            log.error("│   - 에러: {}                                        │", e.getMessage());
            log.error("└─────────────────────────────────────────────────────┘");
            throw e;
        }
    }

    /**
     * 3. 도서관 검색 Tool
     * <p>
     * 지역별 도서관을 검색합니다.
     * **중요**: 반드시 숫자 지역 코드를 사용하세요.
     * 한글 지역명(광주, 서울 등)을 사용하면 API 에러가 발생합니다.
     * </p>
     *
     * @param region 지역 코드 (예: 11=서울, 24=광주)
     * @param libraryName 도서관명 (선택 사항, 부분 검색 가능)
     * @return 도서관 검색 결과 목록
     */
    @org.springframework.ai.tool.annotation.Tool(
        description = "지역별 도서관을 검색합니다. " +
                     "**중요**: 반드시 숫자 지역 코드를 사용하세요. " +
                     "한글 지역명(광주, 서울 등)을 사용하면 API 에러가 발생합니다. " +
                     "지역 코드 예시: 서울=11, 부산=21, 대구=22, 인천=23, 광주=24, 대전=25, 울산=26, 세종=29, 경기=31. " +
                     "libraryName은 선택 사항이며, 부분 검색이 가능합니다."
    )
    public List<LibrarySearchResult.LibraryItem> searchLibraries(
        @org.springframework.ai.tool.annotation.ToolParam(
            description = "지역 코드 (숫자). 예: 서울=11, 부산=21, 대구=22, 인천=23, 광주=24, 대전=25, 울산=26, 세종=29, 경기=31. " +
                         "한글 지역명 사용 금지! 반드시 숫자 코드 사용할 것."
        ) String region,
        @org.springframework.ai.tool.annotation.ToolParam(
            description = "도서관명 (선택 사항, 부분 검색 가능)",
            required = false
        ) String libraryName
    ) {
        long startTime = System.currentTimeMillis();
        log.info("┌─────────────────────────────────────────────────────┐");
        log.info("│ [@Tool: SearchLibraries] 🏛️ 도서관 검색 시작          │");
        log.info("├─────────────────────────────────────────────────────┤");
        log.info("│ 요청 파라미터:                                        │");
        log.info("│   - libraryName (도서관명): {}                       │", libraryName);
        log.info("│   - region (지역 코드): {}                             │", region);
        log.info("└─────────────────────────────────────────────────────┘");

        try {
            List<LibraryInfo> libraries = apiClient.searchLibraries(libraryName, region);

            long apiElapsed = System.currentTimeMillis() - startTime;
            log.info("┌─────────────────────────────────────────────────────┐");
            log.info("│ [@Tool: SearchLibraries] API 호출 완료               │");
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
            log.info("│ [@Tool: SearchLibraries] ✅ 실행 완료                  │");
            log.info("│   - 총 소요시간: {}ms                                 │", totalElapsed);
            log.info("│   - 반환할 도서관 수: {}개                             │", items.size());
            log.info("└─────────────────────────────────────────────────────┘");

            return items;

        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("┌─────────────────────────────────────────────────────┐");
            log.error("│ [@Tool: SearchLibraries] ❌ 실행 실패 ({}ms)          │", elapsed);
            log.error("│   - 에러: {}                                        │", e.getMessage());
            log.error("└─────────────────────────────────────────────────────┘");
            throw e;
        }
    }

    /**
     * 4. 도서 상세 정보 조회 Tool
     * <p>
     * ISBN으로 도서의 상세 정보를 조회합니다.
     * 대출 정보 포함 여부를 선택할 수 있습니다.
     * </p>
     *
     * @param isbn13 도서 ISBN13
     * @param includeLoanInfo 대출 정보 포함 여부 (기본값: true)
     * @return 도서 상세 정보 (BookDetailResult)
     */
    @org.springframework.ai.tool.annotation.Tool(
        description = "ISBN으로 도서의 상세 정보를 조회합니다. " +
                     "대출 정보 포함 여부를 선택할 수 있습니다."
    )
    public BookDetailResult getBookDetail(
        @org.springframework.ai.tool.annotation.ToolParam(
            description = "도서 ISBN13 (13자리 ISBN)"
        ) String isbn13,
        @org.springframework.ai.tool.annotation.ToolParam(
            description = "대출 정보 포함 여부 (기본값: true)",
            required = false
        ) Boolean includeLoanInfo
    ) {
        log.info("┌─────────────────────────────────────────────────────┐");
        log.info("│ [@Tool: GetBookDetail] 📖 도서 상세 조회 시작         │");
        log.info("├─────────────────────────────────────────────────────┤");
        log.info("│ 요청 파라미터:                                        │");
        log.info("│   - isbn13 (ISBN): {}                                │", isbn13);
        log.info("│   - includeLoanInfo (대출정보포함): {}                  │", includeLoanInfo);
        log.info("└─────────────────────────────────────────────────────┘");

        try {
            boolean actualIncludeLoanInfo = includeLoanInfo != null ? includeLoanInfo : true;
            BookDetailInfo detail = apiClient.getBookDetail(isbn13, actualIncludeLoanInfo);

            if (detail == null) {
                log.warn("┌─────────────────────────────────────────────────────┐");
                log.warn("│ [@Tool: GetBookDetail] ⚠️ 도서 상세 정보 없음         │");
                log.warn("│   - isbn: {}                                        │", isbn13);
                log.warn("└─────────────────────────────────────────────────────┘");
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

            log.info("┌─────────────────────────────────────────────────────┐");
            log.info("│ [@Tool: GetBookDetail] ✅ 실행 완료                    │");
            log.info("│   - 도서명: {}                                        │", detail.bookname());
            log.info("└─────────────────────────────────────────────────────┘");

            return new BookDetailResult(detail.isbn13(), detail.bookname(), detail.authors(),
                detail.publisher(), detail.publicationDate(), detail.imageUrl(), detail.description(),
                detail.category(), detail.loanCount(), loanInfo);

        } catch (Exception e) {
            log.error("┌─────────────────────────────────────────────────────┐");
            log.error("│ [@Tool: GetBookDetail] ❌ 실행 실패                    │");
            log.error("│   - 에러: {}                                        │", e.getMessage());
            log.error("└─────────────────────────────────────────────────────┘");
            throw e;
        }
    }

    /**
     * 5. 인기대출도서 조회 Tool
     * <p>
     * 특정 기간 동안의 인기 대출 도서를 조회합니다.
     * </p>
     *
     * @param startDt 시작일 (yyyyMMdd 형식)
     * @param endDt 종료일 (yyyyMMdd 형식)
     * @param region 지역 코드 (선택 사항)
     * @return 인기 대출 도서 목록 (PopularBooksResult)
     */
    @org.springframework.ai.tool.annotation.Tool(
        description = "특정 기간 동안의 인기 대출 도서를 조회합니다. " +
                     "날짜는 yyyyMMdd 형식으로 입력하세요. (예: 20230101)"
    )
    public List<PopularBooksResult.BookItem> getPopularBooks(
        @org.springframework.ai.tool.annotation.ToolParam(
            description = "시작일 (yyyyMMdd 형식, 예: 20230101)"
        ) String startDt,
        @org.springframework.ai.tool.annotation.ToolParam(
            description = "종료일 (yyyyMMdd 형식, 예: 20231231)"
        ) String endDt,
        @org.springframework.ai.tool.annotation.ToolParam(
            description = "지역 코드 (선택 사항, 숫자 코드)",
            required = false
        ) String region
    ) {
        log.info("┌─────────────────────────────────────────────────────┐");
        log.info("│ [@Tool: GetPopularBooks] 📈 인기대출도서 조회 시작      │");
        log.info("├─────────────────────────────────────────────────────┤");
        log.info("│ 요청 파라미터:                                        │");
        log.info("│   - startDt (시작일): {}                              │", startDt);
        log.info("│   - endDt (종료일): {}                                │", endDt);
        log.info("│   - region (지역): {}                                 │", region);
        log.info("└─────────────────────────────────────────────────────┘");

        try {
            List<PopularBookInfo> popularBooks = apiClient.getPopularBooks(startDt, endDt, region);
            List<PopularBooksResult.BookItem> items = popularBooks.stream()
                .map(book -> new PopularBooksResult.BookItem(
                    book.isbn13(), book.bookName(), book.authors(), book.publisher(),
                    book.loanCount(), book.ranking()))
                .toList();

            log.info("┌─────────────────────────────────────────────────────┐");
            log.info("│ [@Tool: GetPopularBooks] ✅ 실행 완료                 │");
            log.info("│   - 검색된 도서 수: {}권                              │", items.size());
            log.info("└─────────────────────────────────────────────────────┘");

            return items;

        } catch (Exception e) {
            log.error("┌─────────────────────────────────────────────────────┐");
            log.error("│ [@Tool: GetPopularBooks] ❌ 실행 실패                 │");
            log.error("│   - 에러: {}                                        │", e.getMessage());
            log.error("└─────────────────────────────────────────────────────┘");
            throw e;
        }
    }

    /**
     * 6. 대출 급상승 도서 조회 Tool
     * <p>
     * 특정 날짜의 대출 급상승 도서를 조회합니다.
     * </p>
     *
     * @param searchDate 검색일 (yyyyMMdd 형식)
     * @return 대출 급상승 도서 목록 (HotTrendResult)
     */
    @org.springframework.ai.tool.annotation.Tool(
        description = "특정 날짜의 대출 급상승 도서를 조회합니다. " +
                     "날짜는 yyyyMMdd 형식으로 입력하세요. (예: 20230101)"
    )
    public List<HotTrendResult.BookItem> getHotTrendBooks(
        @org.springframework.ai.tool.annotation.ToolParam(
            description = "검색일 (yyyyMMdd 형식, 예: 20230101)"
        ) String searchDate
    ) {
        log.info("┌─────────────────────────────────────────────────────┐");
        log.info("│ [@Tool: GetHotTrendBooks] 🔥 대출 급상승 도서 조회 시작   │");
        log.info("├─────────────────────────────────────────────────────┤");
        log.info("│ 요청 파라미터:                                        │");
        log.info("│   - searchDate (검색일): {}                           │", searchDate);
        log.info("└─────────────────────────────────────────────────────┘");

        try {
            List<HotTrendBookInfo> hotBooks = apiClient.getHotTrendBooks(searchDate);
            List<HotTrendResult.BookItem> items = hotBooks.stream()
                .map(book -> new HotTrendResult.BookItem(
                    book.isbn13(), book.bookName(), book.authors(), book.publisher(),
                    book.currentRank(), book.rankChange()))
                .toList();

            log.info("┌─────────────────────────────────────────────────────┐");
            log.info("│ [@Tool: GetHotTrendBooks] ✅ 실행 완료                │");
            log.info("│   - 검색된 도서 수: {}권                              │", items.size());
            log.info("└─────────────────────────────────────────────────────┘");

            return items;

        } catch (Exception e) {
            log.error("┌─────────────────────────────────────────────────────┐");
            log.error("│ [@Tool: GetHotTrendBooks] ❌ 실행 실패                │");
            log.error("│   - 에러: {}                                        │", e.getMessage());
            log.error("└─────────────────────────────────────────────────────┘");
            throw e;
        }
    }

    /**
     * 7. 추천도서 조회 Tool
     * <p>
     * 특정 도서와 연관된 추천 도서를 조회합니다.
     * </p>
     *
     * @param isbn13 기준 도서 ISBN13
     * @param type 추천 유형 (기본값: "relation", 선택 사항)
     * @return 추천 도서 목록 (RecommendResult)
     */
    @org.springframework.ai.tool.annotation.Tool(
        description = "특정 도서와 연관된 추천 도서를 조회합니다. " +
                     "추천 유형: relation(연관도서), author(동일저자), category(동일분야)"
    )
    public List<RecommendResult.BookItem> getRecommendedBooks(
        @org.springframework.ai.tool.annotation.ToolParam(
            description = "기준 도서 ISBN13 (13자리 ISBN)"
        ) String isbn13,
        @org.springframework.ai.tool.annotation.ToolParam(
            description = "추천 유형 (기본값: relation). " +
                         "relation=연관도서, author=동일저자, category=동일분야, keyword=키워드",
            required = false
        ) String type
    ) {
        log.info("┌─────────────────────────────────────────────────────┐");
        log.info("│ [@Tool: GetRecommendedBooks] 💡 추천도서 조회 시작      │");
        log.info("├─────────────────────────────────────────────────────┤");
        log.info("│ 요청 파라미터:                                        │");
        log.info("│   - isbn13 (기준 도서 ISBN): {}                       │", isbn13);
        log.info("│   - type (추천 유형): {}                              │", type);
        log.info("└─────────────────────────────────────────────────────┘");

        try {
            String actualType = (type != null && !type.isBlank()) ? type : "relation";
            List<RecommendedBookInfo> recommendedBooks = apiClient.getRecommendedBooks(isbn13, actualType);
            List<RecommendResult.BookItem> items = recommendedBooks.stream()
                .map(book -> new RecommendResult.BookItem(
                    book.isbn13(), book.bookName(), book.authors(), book.publisher(), book.rank()))
                .toList();

            log.info("┌─────────────────────────────────────────────────────┐");
            log.info("│ [@Tool: GetRecommendedBooks] ✅ 실행 완료             │");
            log.info("│   - 검색된 도서 수: {}권                              │", items.size());
            log.info("└─────────────────────────────────────────────────────┘");

            return items;

        } catch (Exception e) {
            log.error("┌─────────────────────────────────────────────────────┐");
            log.error("│ [@Tool: GetRecommendedBooks] ❌ 실행 실패             │");
            log.error("│   - 에러: {}                                        │", e.getMessage());
            log.error("└─────────────────────────────────────────────────────┘");
            throw e;
        }
    }

    /**
     * 8. 도서 소장 여부 조회 Tool (bookExist API)
     * <p>
     * 특정 도서관에 도서가 소장되어 있는지 확인합니다.
     * </p>
     *
     * @param libCode 도서관 코드
     * @param isbn13 도서 ISBN13
     * @return 도서 소장 여부 (BookExistResult)
     */
    @org.springframework.ai.tool.annotation.Tool(
        description = "특정 도서관에 도서가 소장되어 있는지 확인합니다. " +
                     "도서관 코드(libCode)와 ISBN을 받아 소장 여부와 대출 가능 여부를 반환합니다."
    )
    public BookExistResult checkBookExists(
        @org.springframework.ai.tool.annotation.ToolParam(
            description = "도서관 코드 (예: 강남도서관='111701')"
        ) String libCode,
        @org.springframework.ai.tool.annotation.ToolParam(
            description = "도서 ISBN13 (13자리 ISBN)"
        ) String isbn13
    ) {
        long startTime = System.currentTimeMillis();
        log.info("┌─────────────────────────────────────────────────────┐");
        log.info("│ [@Tool: CheckBookExists] ✅ 도서 소장 여부 확인         │");
        log.info("├─────────────────────────────────────────────────────┤");
        log.info("│ 요청 파라미터:                                        │");
        log.info("│   - libCode (도서관 코드): {}                         │", libCode);
        log.info("│   - isbn13 (ISBN): {}                                 │", isbn13);
        log.info("└─────────────────────────────────────────────────────┘");

        try {
            com.nhnacademy.library.external.opennaru.dto.BookExistInfo info =
                apiClient.checkBookExists(libCode, isbn13);

            long apiElapsed = System.currentTimeMillis() - startTime;
            log.info("┌─────────────────────────────────────────────────────┐");
            log.info("│ [@Tool: CheckBookExists] bookExist API 호출 완료      │");
            log.info("│   - 소요시간: {}ms                                   │", apiElapsed);
            log.info("│   - 소장 여부: {}                                    │", info != null ? info.exists() : false);
            if (info != null && info.exists()) {
                log.info("│   - 대출 가능: {}                                  │", "Y".equals(info.loanAvailable()) ? "✅" : "❌");
            }
            log.info("└─────────────────────────────────────────────────────┘");

            BookExistResult result = BookExistResult.from(libCode, null, info);

            long totalElapsed = System.currentTimeMillis() - startTime;
            log.info("┌─────────────────────────────────────────────────────┐");
            log.info("│ [@Tool: CheckBookExists] ✅ 실행 완료                  │");
            log.info("│   - 총 소요시간: {}ms                                 │", totalElapsed);
            log.info("│   - exists: {}                                       │", result.exists());
            log.info("│   - loanAvailable: {}                                │", result.loanAvailable());
            log.info("└─────────────────────────────────────────────────────┘");

            return result;

        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("┌─────────────────────────────────────────────────────┐");
            log.error("│ [@Tool: CheckBookExists] ❌ 실행 실패 ({}ms)          │", elapsed);
            log.error("│   - 에러: {}                                        │", e.getMessage());
            log.error("└─────────────────────────────────────────────────────┘");
            throw e;
        }
    }
}
