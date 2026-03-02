package com.nhnacademy.library.ai.function;

import com.nhnacademy.library.ai.function.dto.ReviewResult;
import com.nhnacademy.library.core.review.dto.ReviewResponse;
import com.nhnacademy.library.core.review.service.ReviewService;
import com.nhnacademy.library.core.book.service.search.BookSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 리뷰 조회 Function (Spring AI 1.1.2 FunctionToolCallback 방식)
 * <p>
 * AI Agent가 도서의 리뷰를 조회할 때 호출하는 함수입니다.
 * 도서 ID를 받아 리뷰 요약, 평점, 주요 리뷰를 반환합니다.
 * </p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReviewFunction {

    private final ReviewService reviewService;
    private final BookSearchService bookSearchService;

    private static final int REVIEW_LIMIT = 5;

    /**
     * 리뷰 조회 실행 메서드 (외부에서 호출)
     */
    public ReviewResult getReviews(Long bookId) {
        log.info("[AI Function] 리뷰 조회 시작: bookId={}", bookId);

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

            log.info("[AI Function] 리뷰 조회 완료: bookId={}, reviewCount={}",
                bookId, reviewItems.size());

            // 5. 결과 반환
            return ReviewResult.of(
                bookId,
                summary,
                avgRating,
                (int) reviews.getTotalElements(),
                reviewItems
            );

        } catch (Exception e) {
            log.error("[AI Function] 리뷰 조회 실패: bookId={}", bookId, e);
            return ReviewResult.empty(bookId);
        }
    }
}
