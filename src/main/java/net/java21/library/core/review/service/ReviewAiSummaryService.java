package net.java21.library.core.review.service;

import net.java21.library.core.review.domain.BookReview;
import net.java21.library.core.review.domain.BookReviewSummary;
import net.java21.library.core.review.repository.BookReviewRepository;
import net.java21.library.core.review.repository.BookReviewSummaryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;

/**
 * 리뷰 AI 요약을 생성하는 서비스
 * 큐 기반 워커에 의해 순차적으로 호출됨
 *
 * <p>LLM 호출은 수 초~수십 초가 걸리므로 DB 트랜잭션 밖에서 실행합니다.
 * 트랜잭션 안에서 LLM을 호출하면 그동안 book_review_summary 행이 잠겨, 같은 도서에 리뷰를 등록하는
 * 요청이 통계를 갱신하지 못하고 기다리게 됩니다. 그래서 다음 세 단계로 나눕니다.</p>
 * <ol>
 *   <li>짧은 트랜잭션: 조건 확인, 생성 중 표시, 요약할 리뷰 조회</li>
 *   <li>트랜잭션 없음: LLM 호출 (Map-Reduce 요약)</li>
 *   <li>짧은 트랜잭션: 엔티티를 다시 읽어 요약만 반영</li>
 * </ol>
 */
@Slf4j
@Service
public class ReviewAiSummaryService {
    private static final int MIN_REVIEW_COUNT_FOR_SUMMARY = 5;

    private final BookReviewSummaryRepository bookReviewSummaryRepository;
    private final BookReviewRepository bookReviewRepository;
    private final ReviewSummarizer reviewSummarizer;
    private final TransactionTemplate transactionTemplate;

    public ReviewAiSummaryService(BookReviewSummaryRepository bookReviewSummaryRepository,
                                  BookReviewRepository bookReviewRepository,
                                  ReviewSummarizer reviewSummarizer,
                                  PlatformTransactionManager transactionManager) {
        this.bookReviewSummaryRepository = bookReviewSummaryRepository;
        this.bookReviewRepository = bookReviewRepository;
        this.reviewSummarizer = reviewSummarizer;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * 요약 작업에 필요한 값 (1단계에서 읽어 3단계까지 전달)
     *
     * @param fullRebuild     전체 리뷰를 다시 요약할지 여부
     * @param reviewContents  요약할 리뷰 본문
     * @param existingSummary 기존 요약 (누적 요약일 때 사용)
     * @param reviewCount     요약 대상 시점의 전체 리뷰 수
     */
    record SummaryJob(boolean fullRebuild, List<String> reviewContents, String existingSummary, long reviewCount) {
    }

    public void generateSummary(Long bookId) {
        // 1단계: 생성 중 표시 후 바로 커밋 (행 잠금은 이 짧은 트랜잭션 동안만 유지)
        Optional<SummaryJob> job = transactionTemplate.execute(status -> prepare(bookId));
        if (job == null || job.isEmpty()) {
            return;
        }

        String newSummary;
        try {
            // 2단계: 트랜잭션 밖에서 LLM 호출
            SummaryJob summaryJob = job.get();
            newSummary = summaryJob.fullRebuild()
                    ? reviewSummarizer.summarizeReviews(summaryJob.reviewContents())
                    : reviewSummarizer.summarizeIncremental(summaryJob.reviewContents(), summaryJob.existingSummary());
        } catch (Exception e) {
            log.error("Failed to generate summary for book {}: {}", bookId, e.getMessage(), e);
            transactionTemplate.executeWithoutResult(status -> releaseGenerating(bookId));
            return;
        }

        // 3단계: 최신 엔티티를 다시 읽어 요약만 반영 (그사이 갱신된 통계는 덮어쓰지 않음)
        long summarizedCount = job.get().reviewCount();
        transactionTemplate.executeWithoutResult(status ->
                bookReviewSummaryRepository.findById(bookId).ifPresent(summary -> {
                    summary.completeSummary(newSummary, summarizedCount);
                    log.info("Successfully updated review summary for book {}.", bookId);
                }));
    }

    private Optional<SummaryJob> prepare(Long bookId) {
        Optional<BookReviewSummary> bookReviewSummaryOptional = bookReviewSummaryRepository.findById(bookId);

        if (bookReviewSummaryOptional.isEmpty()) {
            log.info("No summary found for book {}. Skipping AI summary generation.", bookId);
            return Optional.empty();
        }

        BookReviewSummary summary = bookReviewSummaryOptional.get();

        // 조건 확인
        if (!shouldGenerateSummary(summary)) {
            log.info("Conditions not met for book {}. Review count: {}, is_dirty: {}, is_generating: {}",
                    bookId, summary.getReviewCount(), summary.getIsSummaryDirty(), summary.getIsGenerating());
            return Optional.empty();
        }

        // 중복 실행 방지 체크
        if (Boolean.TRUE.equals(summary.getIsGenerating())) {
            log.info("Already generating summary for book {}. Skipping.", bookId);
            return Optional.empty();
        }

        long reviewCount = summary.getReviewCount();
        long lastSummarizedCount = summary.getLastSummarizedCount() != null
                ? summary.getLastSummarizedCount()
                : 0;

        // Full Rebuild 필요성 확인
        boolean needsRebuild = (reviewCount - lastSummarizedCount) >= reviewSummarizer.getReduceThreshold();

        List<BookReview> reviews;
        if (needsRebuild) {
            // 전체 리뷰를 다시 요약 (Map-Reduce Full Rebuild)
            log.info("Full rebuild needed for book {} ({} new reviews). Rebuilding all reviews.",
                    bookId, reviewCount - lastSummarizedCount);
            reviews = bookReviewRepository.findAllByBookId(bookId);
        } else {
            // 누적 요약 (새 리뷰만 요약해서 기존 요약과 병합)
            log.info("Incremental summary for book {} ({} new reviews since last summary).",
                    bookId, reviewCount - lastSummarizedCount);
            reviews = bookReviewRepository.findNewReviewsAfterId(bookId, lastSummarizedCount);
        }

        if (reviews.isEmpty()) {
            log.info("No new reviews to summarize for book {}.", bookId);
            summary.setSummaryDirty(false);
            return Optional.empty();
        }

        // 요약 시작 플래그 설정 (중복 실행 방지). 이 트랜잭션이 끝나면 바로 커밋되어 잠금이 풀립니다.
        summary.setGenerating(true);

        List<String> reviewContents = reviews.stream()
                .map(BookReview::getContent)
                .toList();
        return Optional.of(new SummaryJob(needsRebuild, reviewContents, summary.getReviewSummary(), reviewCount));
    }

    private void releaseGenerating(Long bookId) {
        bookReviewSummaryRepository.findById(bookId).ifPresent(summary -> summary.setGenerating(false));
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
