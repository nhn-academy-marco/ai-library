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
    @Test
    @DisplayName("RRF 점수가 같으면 벡터 유사도가 높은 도서가 앞에 온다")
    void fuse_tieOnScore_higherSimilarityFirst() {
        // 30번: 키워드 1위·벡터 2위, 10번: 키워드 2위·벡터 1위 → RRF 점수 동일
        List<BookSearchResponse> fused = rrfService.fuse(
                List.of(book(30L, null, false), book(10L, null, false)),
                List.of(book(10L, 0.9, false), book(30L, 0.8, false)));

        assertThat(fused).extracting(BookSearchResponse::getId).containsExactly(10L, 30L);
    }

    @Test
    @DisplayName("RRF 점수와 유사도가 모두 같으면 도서 ID 오름차순으로 정렬한다")
    void fuse_tieOnScoreAndSimilarity_lowerIdFirst() {
        List<BookSearchResponse> fused = rrfService.fuse(
                List.of(book(20L, 0.5, false)),
                List.of(book(3L, 0.5, false)));

        assertThat(fused).extracting(BookSearchResponse::getId).containsExactly(3L, 20L);
    }

    @Test
    @DisplayName("동점이 많은 입력의 순서를 바꿔도(키워드·벡터 목록 교환) 같은 결과를 반환한다")
    void fuse_swappedInputs_sameOrder() {
        List<BookSearchResponse> listA = new java.util.ArrayList<>();
        List<BookSearchResponse> listB = new java.util.ArrayList<>();
        // 서로 겹치지 않는 도서들을 같은 순위에 배치해 동점을 다수 만든다 (ID는 HashMap 순서와 무관하게 흩어 놓음)
        long[] idsA = {9001L, 17L, 4242L, 256L, 1L, 77777L};
        long[] idsB = {65536L, 3L, 1024L, 88L, 123456L, 5L};
        for (int i = 0; i < idsA.length; i++) {
            listA.add(book(idsA[i], 0.5, false));
            listB.add(book(idsB[i], 0.5, false));
        }

        List<Long> forward = rrfService.fuse(listA, listB).stream().map(BookSearchResponse::getId).toList();
        List<Long> swapped = rrfService.fuse(listB, listA).stream().map(BookSearchResponse::getId).toList();

        assertThat(swapped).isEqualTo(forward);
        // 같은 순위끼리는 ID 오름차순
        assertThat(forward.subList(0, 2)).containsExactly(9001L, 65536L);
    }
}
