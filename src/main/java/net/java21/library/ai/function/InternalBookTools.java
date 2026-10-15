package net.java21.library.ai.function;

import net.java21.library.ai.function.dto.BookSearchResult;
import net.java21.library.ai.function.dto.LoanCheckResult;
import net.java21.library.ai.function.dto.ReviewResult;
import net.java21.library.core.book.domain.SearchType;
import net.java21.library.core.book.dto.BookSearchResponse;
import net.java21.library.core.book.service.search.BookSearchService;
import net.java21.library.core.review.dto.ReviewResponse;
import net.java21.library.core.review.service.ReviewService;
import net.java21.library.external.opennaru.client.LibraryInfoNaruApiClient;
import net.java21.library.external.opennaru.dto.LoanItemInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 내부 DB 검색용 Spring AI @Tool 애노테이션 기반 구현
 * <p>
 * Spring AI 1.1.2의 @Tool 애노테이션을 사용하여 내부 DB 검색 Function Calling을 구현합니다.
 * AI Agent가 내부 DB의 도서, 리뷰, 대출 가능 정보를 조회할 때 호출하는 함수들입니다.
 * </p>
 *
 * @see org.springframework.ai.model.function.FunctionCallback
 * @see org.springframework.ai.model.function.ToolContext
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InternalBookTools {

    private final BookSearchService bookSearchService;
    private final ReviewService reviewService;
    private final LibraryInfoNaruApiClient libraryInfoNaruApiClient;

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int REVIEW_LIMIT = 5;

    /**
     * 1. 내부 DB 도서 검색 Tool
     * <p>
     * 내부 DB에서 도서를 검색합니다.
     * 하이브리드 검색 (전체 텍스트 + 임베딩)을 사용합니다.
     * </p>
     *
     * @param query 검색어 (도서 제목, 저자, 출판사 등)
     * @param limit 최대 결과 수 (기본값: 10)
     * @return 도서 검색 결과 목록 (BookSearchResult.BookItem)
     */
    @org.springframework.ai.tool.annotation.Tool(
        description = "내부 DB에서 도서를 검색합니다. " +
                     "하이브리드 검색(전체 텍스트 + 임베딩)을 사용하여 도서 제목, 저자, 출판사 등으로 검색합니다. " +
                     "정확한 ISBN을 알면 ISBN 검색도 가능합니다."
    )
    public List<BookSearchResult.BookItem> searchInternalBooks(
        @org.springframework.ai.tool.annotation.ToolParam(
            description = "검색어 (도서 제목, 저자, 출판사 등)"
        ) String query,
        @org.springframework.ai.tool.annotation.ToolParam(
            description = "최대 결과 수 (기본값: 10, 최대: 100)",
            required = false
        ) Integer limit
    ) {
        long startTime = System.currentTimeMillis();
        int actualLimit = (limit != null && limit > 0) ? Math.min(limit, 100) : DEFAULT_PAGE_SIZE;

        log.info("┌─────────────────────────────────────────────────────┐");
        log.info("│ [@Tool: SearchInternalBooks] 📚 내부 DB 도서 검색 시작  │");
        log.info("├─────────────────────────────────────────────────────┤");
        log.info("│ 요청 파라미터:                                        │");
        log.info("│   - query (검색어): {}                               │", query);
        log.info("│   - limit (최대 결과): {}                            │", actualLimit);
        log.info("└─────────────────────────────────────────────────────┘");

        try {
            // 1. 내부 DB 검색 실행 (하이브리드 검색)
            var searchRequest = new net.java21.library.core.book.dto.BookSearchRequest(
                query,
                null,
                SearchType.HYBRID,
                null,
                false
            );

            var result = bookSearchService.searchBooks(
                PageRequest.of(0, actualLimit),
                searchRequest
            );

            // 2. 결과 변환
            List<BookSearchResult.BookItem> bookItems = result.getBooks().getContent().stream()
                .map(this::toBookItem)
                .toList();

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("┌─────────────────────────────────────────────────────┐");
            log.info("│ [@Tool: SearchInternalBooks] ✅ 실행 완료             │");
            log.info("│   - 소요시간: {}ms                                   │", elapsed);
            log.info("│   - 검색된 도서 수: {}권                              │", bookItems.size());
            log.info("└─────────────────────────────────────────────────────┘");

            return bookItems;

        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("┌─────────────────────────────────────────────────────┐");
            log.error("│ [@Tool: SearchInternalBooks] ❌ 실행 실패 ({}ms)      │", elapsed);
            log.error("│   - 에러: {}                                        │", e.getMessage());
            log.error("└─────────────────────────────────────────────────────┘");
            return List.of();
        }
    }

    /**
     * BookSearchResponse를 BookSearchResult.BookItem으로 변환합니다.
     */
    private BookSearchResult.BookItem toBookItem(BookSearchResponse book) {
        return BookSearchResult.BookItem.fromInternalDb(
            book.getId(),
            book.getTitle(),
            book.getAuthorName(),
            book.getPublisherName(),
            book.getIsbn()
        );
    }

    /**
     * 2. 리뷰 조회 Tool
     * <p>
     * 도서 ID를 받아 리뷰 요약, 평점, 주요 리뷰를 반환합니다.
     * </p>
     *
     * @param bookId 도서 ID (내부 DB ID)
     * @return 리뷰 결과 (ReviewResult)
     */
    @org.springframework.ai.tool.annotation.Tool(
        description = "도서의 리뷰를 조회합니다. " +
                     "도서 ID를 받아 리뷰 요약, 평균 평점, 주요 리뷰를 반환합니다."
    )
    public ReviewResult getReviews(
        @org.springframework.ai.tool.annotation.ToolParam(
            description = "도서 ID (내부 DB ID)"
        ) Long bookId
    ) {
        long startTime = System.currentTimeMillis();
        log.info("┌─────────────────────────────────────────────────────┐");
        log.info("│ [@Tool: GetReviews] ⭐ 리뷰 조회 시작                   │");
        log.info("├─────────────────────────────────────────────────────┤");
        log.info("│ 요청 파라미터:                                        │");
        log.info("│   - bookId (도서 ID): {}                             │", bookId);
        log.info("└─────────────────────────────────────────────────────┘");

        try {
            // 1. 리뷰 조회 (상위 N개)
            var reviews = reviewService.getReviews(
                bookId,
                PageRequest.of(0, REVIEW_LIMIT)
            );

            // 2. 리뷰 요약 조회
            String summary = bookSearchService.getReviewSummary(bookId);

            // 3. 평균 평점 계산
            double avgRating = reviews.getContent().stream()
                .mapToInt(ReviewResponse::rating)
                .average()
                .orElse(0.0);

            // 4. 결과 변환
            List<ReviewResult.ReviewItem> reviewItems = reviews.getContent().stream()
                .map(review -> ReviewResult.ReviewItem.of(
                    review.content(),
                    review.rating()
                ))
                .toList();

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("┌─────────────────────────────────────────────────────┐");
            log.info("│ [@Tool: GetReviews] ✅ 실행 완료                      │");
            log.info("│   - 소요시간: {}ms                                   │", elapsed);
            log.info("│   - 리뷰 수: {}개                                    │", reviewItems.size());
            log.info("│   - 평균 평점: {}                                    │", String.format("%.1f", avgRating));
            log.info("└─────────────────────────────────────────────────────┘");

            // 5. 결과 반환
            return ReviewResult.of(
                bookId,
                summary,
                avgRating,
                (int) reviews.getTotalElements(),
                reviewItems
            );

        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("┌─────────────────────────────────────────────────────┐");
            log.error("│ [@Tool: GetReviews] ❌ 실행 실패 ({}ms)              │", elapsed);
            log.error("│   - 에러: {}                                        │", e.getMessage());
            log.error("└─────────────────────────────────────────────────────┘");
            return ReviewResult.empty(bookId);
        }
    }

    /**
     * 3. 대출 가능 여부 조회 Tool
     * <p>
     * ISBN을 받아 전국 도서관의 대출 가능 현황을 반환합니다.
     * 지역 필터링, 거리 기반 정렬을 지원합니다.
     * </p>
     *
     * @param isbn ISBN (ISBN10 또는 ISBN13)
     * @param region 지역 (선택 사항, 예: "서울", "경기")
     * @param limit 최대 결과 수 (기본값: 10)
     * @return 대출 가능 여부 결과 (LoanCheckResult)
     */
    @org.springframework.ai.tool.annotation.Tool(
        description = "도서의 대출 가능 여부를 조회합니다. " +
                     "ISBN을 받아 전국 도서관의 대출 가능 현황을 반환합니다. " +
                     "지역 필터링, 위도/경도에 따른 거리 기반 정렬을 지원합니다."
    )
    public LoanCheckResult checkLoanAvailability(
        @org.springframework.ai.tool.annotation.ToolParam(
            description = "도서 ISBN (ISBN10 또는 ISBN13)"
        ) String isbn,
        @org.springframework.ai.tool.annotation.ToolParam(
            description = "지역 (선택 사항, 예: '서울', '경기', '광주')",
            required = false
        ) String region,
        @org.springframework.ai.tool.annotation.ToolParam(
            description = "최대 결과 수 (기본값: 10)",
            required = false
        ) Integer limit
    ) {
        long startTime = System.currentTimeMillis();
        int actualLimit = (limit != null && limit > 0) ? limit : 10;

        log.info("┌─────────────────────────────────────────────────────┐");
        log.info("│ [@Tool: CheckLoanAvailability] 📖 대출 가능 여부 조회    │");
        log.info("├─────────────────────────────────────────────────────┤");
        log.info("│ 요청 파라미터:                                        │");
        log.info("│   - isbn (ISBN): {}                                  │", isbn);
        log.info("│   - region (지역): {}                                 │", region);
        log.info("│   - limit (최대 결과): {}                             │", actualLimit);
        log.info("└─────────────────────────────────────────────────────┘");

        try {
            // 1. 도서관정보나루 API 호출
            List<LoanItemInfo> loanItems = libraryInfoNaruApiClient.searchLoanItemsByIsbn(isbn);

            if (loanItems.isEmpty()) {
                log.info("┌─────────────────────────────────────────────────────┐");
                log.info("│ [@Tool: CheckLoanAvailability] ⚠️ 대출 가능 도서관 없음│");
                log.info("│   - isbn: {}                                        │", isbn);
                log.info("└─────────────────────────────────────────────────────┘");
                return LoanCheckResult.empty(isbn);
            }

            // 2. 지역 필터링 (선택 사항)
            List<LoanItemInfo> filteredItems = filterByRegion(loanItems, region);

            // 3. 상위 N개 추출
            List<LoanItemInfo> topItems = filteredItems.stream()
                .limit(actualLimit)
                .toList();

            // 4. 결과 변환
            List<LoanCheckResult.LibraryInfo> libraries = topItems.stream()
                .map(this::toLibraryInfo)
                .toList();

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("┌─────────────────────────────────────────────────────┐");
            log.info("│ [@Tool: CheckLoanAvailability] ✅ 실행 완료            │");
            log.info("│   - 소요시간: {}ms                                   │", elapsed);
            log.info("│   - 대출 가능 도서관 수: {}개                           │", libraries.size());
            log.info("└─────────────────────────────────────────────────────┘");

            return LoanCheckResult.of(
                isbn,
                loanItems.size(),
                libraries
            );

        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("┌─────────────────────────────────────────────────────┐");
            log.error("│ [@Tool: CheckLoanAvailability] ❌ 실행 실패 ({}ms)     │", elapsed);
            log.error("│   - 에러: {}                                        │", e.getMessage());
            log.error("└─────────────────────────────────────────────────────┘");
            return LoanCheckResult.empty(isbn);
        }
    }

    /**
     * 지역 기반 필터링
     */
    private List<LoanItemInfo> filterByRegion(List<LoanItemInfo> loanItems, String region) {
        if (region == null || region.isBlank()) {
            return loanItems;
        }
        return loanItems.stream()
            .filter(item -> item.location() != null && item.location().contains(region))
            .collect(Collectors.toList());
    }

    /**
     * LoanItemInfo를 LibraryInfo로 변환합니다.
     */
    private LoanCheckResult.LibraryInfo toLibraryInfo(LoanItemInfo item) {
        boolean loanAvailable = "Y".equalsIgnoreCase(item.loanAvailable());

        return LoanCheckResult.LibraryInfo.of(
            item.libraryName(),
            item.location(),
            null, // 거리 정보 (현재 미지원)
            "",   // 전화번호 (현재 미지원)
            loanAvailable,
            ""    // 운영 시간 (현재 미지원)
        );
    }
}
