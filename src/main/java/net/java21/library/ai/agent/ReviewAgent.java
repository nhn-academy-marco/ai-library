package net.java21.library.ai.agent;

import net.java21.library.ai.function.dto.ReviewResult;
import net.java21.library.core.review.dto.ReviewResponse;
import net.java21.library.core.review.service.ReviewService;
import net.java21.library.core.book.service.search.BookSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 리뷰 조회 Agent
 * <p>
 * 리뷰 조회 전담 Agent입니다.
 * 도서 ID를 받아 리뷰 요약, 평점, 주요 리뷰를 조회합니다.
 * </p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReviewAgent implements Agent {

    private final ReviewService reviewService;
    private final BookSearchService bookSearchService;

    private static final int REVIEW_LIMIT = 5;

    @Override
    public AgentType getType() {
        return AgentType.REVIEW;
    }

    @Override
    public String getName() {
        return "ReviewAgent";
    }

    @Override
    public String getDescription() {
        return "리뷰 조회 전담 Agent. 도서의 리뷰, 평점, 요약 정보를 제공합니다.";
    }

    @Override
    public String execute(AgentRequest request) {
        log.info("[{}] 리뷰 조회 실행: query={}", getName(), request.query());

        try {
            // 1. Book ID 추출 (query에서 파싱)
            Long bookId = extractBookId(request.query());

            if (bookId == null) {
                log.warn("[{}] Book ID 추출 실패: query={}", getName(), request.query());
                return "도서 ID를 찾을 수 없습니다.";
            }

            // 2. 리뷰 조회
            var reviews = reviewService.getReviews(
                bookId,
                PageRequest.of(0, REVIEW_LIMIT)
            );

            // 3. 리뷰 요약 조회
            String summary = bookSearchService.getReviewSummary(bookId);

            // 4. 평균 평점 계산
            double avgRating = reviews.getContent().stream()
                .mapToInt(ReviewResponse::rating)
                .average()
                .orElse(0.0);

            // 5. 결과 포맷팅
            StringBuilder response = new StringBuilder();
            response.append(String.format("리뷰 요약: %s\n\n", summary != null ? summary : "없음"));
            response.append(String.format("평균 평점: %.1f/5.0 (%d개 리뷰)\n\n", avgRating, reviews.getTotalElements()));

            if (!reviews.getContent().isEmpty()) {
                response.append("주요 리뷰:\n");
                for (int i = 0; i < reviews.getContent().size(); i++) {
                    ReviewResponse review = reviews.getContent().get(i);
                    String content = review.content();
                    // 리뷰 내용이 너무 길면 100자로 제한
                    String displayContent = content.length() > 100 ? content.substring(0, 100) + "..." : content;
                    response.append(String.format("%d. %s (평점: %d)\n", i + 1, displayContent, review.rating()));
                }
            }

            log.info("[{}] 리뷰 조회 완료: bookId={}, reviewCount={}", getName(), bookId, reviews.getTotalElements());
            return response.toString();

        } catch (Exception e) {
            log.error("[{}] 리뷰 조회 실패: query={}", getName(), request.query(), e);
            return "리뷰 조회 중 오류가 발생했습니다.";
        }
    }

    /**
     * 쿼리에서 Book ID 추출
     */
    private Long extractBookId(String query) {
        try {
            // 숫자만 추출
            String numbers = query.replaceAll("[^0-9]", "");
            return numbers.isEmpty() ? null : Long.parseLong(numbers);
        } catch (Exception e) {
            return null;
        }
    }
}
