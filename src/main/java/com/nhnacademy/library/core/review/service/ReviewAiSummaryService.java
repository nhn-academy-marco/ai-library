package com.nhnacademy.library.core.review.service;

import com.nhnacademy.library.core.review.domain.BookReview;
import com.nhnacademy.library.core.review.domain.BookReviewSummary;
import com.nhnacademy.library.core.review.repository.BookReviewRepository;
import com.nhnacademy.library.core.review.repository.BookReviewSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 리뷰 AI 요약을 생성하는 서비스
 * 큐 기반 워커에 의해 순차적으로 호출됨
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewAiSummaryService {
    private static final int MIN_REVIEW_COUNT_FOR_SUMMARY = 5;

    private final BookReviewSummaryRepository bookReviewSummaryRepository;
    private final BookReviewRepository bookReviewRepository;
    private final ReviewSummarizer reviewSummarizer;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void generateSummary(Long bookId) {
        Optional<BookReviewSummary> bookReviewSummaryOptional = bookReviewSummaryRepository.findById(bookId);

        if (bookReviewSummaryOptional.isEmpty()) {
            log.info("No summary found for book {}. Skipping AI summary generation.", bookId);
            return;
        }

        BookReviewSummary summary = bookReviewSummaryOptional.get();

        // 조건 확인
        if (!shouldGenerateSummary(summary)) {
            log.info("Conditions not met for book {}. Review count: {}, is_dirty: {}, is_generating: {}",
                    bookId, summary.getReviewCount(), summary.getIsSummaryDirty(), summary.getIsGenerating());
            return;
        }

        // 중복 실행 방지 체크
        if (Boolean.TRUE.equals(summary.getIsGenerating())) {
            log.info("Already generating summary for book {}. Skipping.", bookId);
            return;
        }

        try {
            // 요약 시작 플래그 설정 (중복 실행 방지)
            summary.setGenerating(true);
            bookReviewSummaryRepository.saveAndFlush(summary);

            long reviewCount = summary.getReviewCount();
            long lastSummarizedCount = summary.getLastSummarizedCount() != null
                ? summary.getLastSummarizedCount()
                : 0;

            // Full Rebuild 필요성 확인
            boolean needsRebuild = (reviewCount - lastSummarizedCount) >= reviewSummarizer.getReduceThreshold();

            String newSummary;

            if (needsRebuild) {
                // 전체 리뷰를 다시 요약 (Map-Reduce Full Rebuild)
                log.info("Full rebuild needed for book {} ({} new reviews). Rebuilding all reviews.",
                        bookId, reviewCount - lastSummarizedCount);

                List<BookReview> allReviews = bookReviewRepository.findAllByBookId(bookId);
                List<String> reviewContents = allReviews.stream()
                        .map(BookReview::getContent)
                        .toList();

                newSummary = reviewSummarizer.summarizeReviews(reviewContents);

                // last_summarized_count 초기화
                summary.updateSummaryWithCount(newSummary, reviewCount);

            } else {
                // 누적 요약 (새 리뷰만 요약해서 기존 요약과 병합)
                log.info("Incremental summary for book {} ({} new reviews since last summary).",
                        bookId, reviewCount - lastSummarizedCount);

                // last_summarized_count 이후의 새 리뷰만 조회
                List<BookReview> newReviews = bookReviewRepository
                        .findByBookIdAndIdGreaterThanOrderByIdAsc(
                                bookId,
                                lastSummarizedCount
                        );

                if (!newReviews.isEmpty()) {
                    List<String> newReviewContents = newReviews.stream()
                            .map(BookReview::getContent)
                            .toList();

                    String existingSummary = summary.getReviewSummary();

                    newSummary = reviewSummarizer.summarizeIncremental(newReviewContents, existingSummary);

                    // last_summarized_count 업데이트
                    summary.updateSummaryWithCount(newSummary, reviewCount);
                } else {
                    log.info("No new reviews to summarize for book {}.", bookId);
                    summary.setSummaryDirty(false);
                    bookReviewSummaryRepository.save(summary);
                    return;
                }
            }

            bookReviewSummaryRepository.save(summary);
            log.info("Successfully updated review summary for book {}.", bookId);

        } catch (Exception e) {
            log.error("Failed to generate summary for book {}: {}", bookId, e.getMessage(), e);
        } finally {
            // 항상 플래그 해제 (에러 발생해도)
            try {
                summary.setGenerating(false);
                bookReviewSummaryRepository.save(summary);
            } catch (Exception saveException) {
                log.error("Failed to reset is_generating flag for book {}: {}",
                        bookId, saveException.getMessage());
            }
        }
    }

    /**
     * 요약 생성 조건 확인
     *
     * @param summary 리뷰 요약 정보
     * @return 요약 생성 필요 여부
     */
    private boolean shouldGenerateSummary(BookReviewSummary summary) {
        return summary.getReviewCount() >= MIN_REVIEW_COUNT_FOR_SUMMARY
                && summary.getIsSummaryDirty();
    }
}
