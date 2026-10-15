package net.java21.library.core.book.service.search;

import net.java21.library.core.book.dto.BookSearchResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * RrfService 순수 단위 테스트 (Spring Context 없음)
 */
class RrfServiceTest {

    private static final int RRF_K = 60;
    private static final BigDecimal RATING = new BigDecimal("4.50");
    private static final long REVIEW_COUNT = 12L;
    private static final String SUMMARY = "[장점] 문장이 따뜻합니다.";

    private final RrfService rrfService = new RrfService();

    private static BookSearchResponse book(long id, Double similarity, boolean withReview) {
        BookSearchResponse response = new BookSearchResponse(id, "isbn" + id, "제목" + id, null, "저자", "출판사",
                BigDecimal.valueOf(15000), LocalDate.of(2024, 1, 1), null, "소개", similarity);
        if (withReview) {
            response.setAverageRating(RATING);
            response.setReviewCount(REVIEW_COUNT);
            response.setReviewSummary(SUMMARY);
        }
        return response;
    }

    @Test
    @DisplayName("두 결과에 모두 있는 도서는 리뷰 정보를 유지하고 벡터 유사도와 RRF 점수가 채워진다")
    void fuse_keepsReviewFieldsForOverlappingBook() {
        List<BookSearchResponse> fused = rrfService.fuse(
                List.of(book(1L, null, true)),
                List.of(book(1L, 0.87, false)));

        BookSearchResponse result = fused.get(0);
        assertThat(result.getAverageRating()).isEqualByComparingTo(RATING);
        assertThat(result.getReviewCount()).isEqualTo(REVIEW_COUNT);
        assertThat(result.getReviewSummary()).isEqualTo(SUMMARY);
        assertThat(result.getSimilarity()).isEqualTo(0.87);
        assertThat(result.getRrfScore()).isCloseTo(2.0 / (RRF_K + 1), within(1e-9));
    }

    @Test
    @DisplayName("한쪽 결과에만 있는 도서도 리뷰 정보를 유지한다")
    void fuse_keepsReviewFieldsForSingleSourceBook() {
        List<BookSearchResponse> fused = rrfService.fuse(
                List.of(),
                List.of(book(2L, 0.5, true)));

        BookSearchResponse result = fused.get(0);
        assertThat(result.getReviewCount()).isEqualTo(REVIEW_COUNT);
        assertThat(result.getAverageRating()).isEqualByComparingTo(RATING);
        assertThat(result.getReviewSummary()).isEqualTo(SUMMARY);
    }

    @Test
    @DisplayName("두 검색 결과에 모두 등장한 도서가 RRF 점수 내림차순으로 앞에 온다")
    void fuse_sortsByRrfScoreDescending() {
        List<BookSearchResponse> fused = rrfService.fuse(
                List.of(book(1L, null, false), book(2L, null, false)),
                List.of(book(2L, 0.9, false), book(3L, 0.8, false)));

        assertThat(fused).extracting(BookSearchResponse::getId).containsExactly(2L, 1L, 3L);
    }
}
