package net.java21.library.core.review.service;

import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 도서 리뷰 요약 서비스 (Map-Reduce 전략 및 누적 요약 적용)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewSummarizer {

    /**
     * 모든 요약 프롬프트가 공유하는 출력 형식.
     * 화면은 ReviewSummaryFormatter로 이 네 항목을 나눠 표시하므로 형식을 바꾸면 함께 수정해야 합니다.
     */
    private static final String OUTPUT_FORMAT = """
            [출력 형식]
            아래 네 줄만 출력하세요. 각 줄은 항목 이름으로 시작하고 1~2문장으로 작성합니다.
            [장점] ...
            [단점] ...
            [추천 대상] ...
            [총평] ...

            [규칙]
            - 인사말, 서두("물론입니다", "작성해 드리겠습니다" 등), 제목을 쓰지 마세요.
            - Markdown 기호(**, #, -, *)를 쓰지 마세요. 일반 텍스트만 출력하세요.
            - 리뷰에 근거가 없는 내용은 쓰지 마세요. 해당 항목에 근거가 없으면 "리뷰에서 언급이 없습니다."라고 쓰세요.
            - 공손한 합니다체(~입니다, ~합니다)를 사용하세요.
            """;

    @Value("${app.review.map-chunk-size:10}")
    private int mapChunkSize;

    @Value("${app.review.reduce-threshold:10}")
    private int reduceThreshold;

    private final ChatModel chatModel;

    /**
     * 전체 리뷰 요약 (Map-Reduce)
     * 초기 요약이나 Full Rebuild 시 사용
     */
    public String summarizeReviews(List<String> reviews) {
        if (reviews == null || reviews.isEmpty()) {
            return "리뷰가 없어 요약할 수 없습니다.";
        }

        log.info("Starting review summarization for {} reviews using Map-Reduce.", reviews.size());

        // 1. Map 단계: 리뷰를 청크 단위로 나누어 부분 요약 생성
        List<String> partialSummaries = new ArrayList<>();
        List<List<String>> chunks = Lists.partition(reviews, mapChunkSize);

        for (int i = 0; i < chunks.size(); i++) {
            log.info("Map step: processing chunk {}/{}", i + 1, chunks.size());
            String mapPrompt = createMapPrompt(chunks.get(i));
            String summary = ReviewSummaryFormatter.clean(chatModel.call(mapPrompt));
            partialSummaries.add(summary);
        }

        // 2. Reduce 단계: 부분 요약들을 합쳐 최종 요약 생성
        log.info("Reduce step: combining {} partial summaries.", partialSummaries.size());
        String reducePrompt = createReducePrompt(partialSummaries);
        String finalSummary = ReviewSummaryFormatter.clean(chatModel.call(reducePrompt));

        log.info("Successfully generated final review summary.");
        return finalSummary;
    }

    /**
     * 누적 요약 (Incremental Summary)
     * 새 리뷰만 요약해서 기존 요약과 병합
     *
     * @param newReviews 새로 추가된 리뷰 목록
     * @param existingSummary 기존 요약본
     * @return 병합된 최종 요약
     */
    public String summarizeIncremental(List<String> newReviews, String existingSummary) {
        if (newReviews == null || newReviews.isEmpty()) {
            return existingSummary != null ? existingSummary : "리뷰가 없어 요약할 수 없습니다.";
        }

        log.info("Starting incremental summarization for {} new reviews.", newReviews.size());

        // 1. 새 리뷰를 청크 단위로 나누어 부분 요약 생성
        List<String> partialSummaries = new ArrayList<>();
        List<List<String>> chunks = Lists.partition(newReviews, mapChunkSize);

        for (int i = 0; i < chunks.size(); i++) {
            log.info("Map step: processing new reviews chunk {}/{}", i + 1, chunks.size());
            String mapPrompt = createMapPrompt(chunks.get(i));
            String summary = ReviewSummaryFormatter.clean(chatModel.call(mapPrompt));
            partialSummaries.add(summary);
        }

        // 2. 기존 요약과 새 리뷰 요약본들을 병합
        log.info("Reduce step: merging existing summary with {} new partial summaries.", partialSummaries.size());
        String mergePrompt = createMergePrompt(existingSummary, partialSummaries);
        String finalSummary = ReviewSummaryFormatter.clean(chatModel.call(mergePrompt));

        log.info("Successfully generated incremental summary.");
        return finalSummary;
    }

    /**
     * 새 리뷰 요약본을 기존 요약과 병합하는 프롬프트 생성
     */
    private String createMergePrompt(String existingSummary, List<String> newPartialSummaries) {
        StringBuilder newSummaries = new StringBuilder();
        for (int i = 0; i < newPartialSummaries.size(); i++) {
            newSummaries.append("[새 요약 ").append(i + 1).append("]\n");
            newSummaries.append(newPartialSummaries.get(i)).append("\n\n");
        }

        return String.format("""
            당신은 도서 리뷰 분석 전문가입니다.

            [기존 요약]
            %s

            [새로운 리뷰 요약본]
            %s

            위 두 내용을 통합해서 전체 리뷰를 종합한 요약을 작성하세요.
            기존 요약의 핵심 내용은 유지하면서, 새로운 정보를 추가하세요.

            %s
            """, existingSummary != null ? existingSummary : "없음", newSummaries.toString(), OUTPUT_FORMAT);
    }

    private String createMapPrompt(List<String> reviews) {
        StringBuilder sb = new StringBuilder();
        for (String review : reviews) {
            sb.append("- ").append(review).append("\n");
        }

        return String.format("""
            다음은 특정 도서에 대한 독자 리뷰들입니다.
            이 리뷰들에서 공통적으로 언급되는 내용을 요약하세요.

            %s

            리뷰 리스트:
            %s
            """, OUTPUT_FORMAT, sb.toString());
    }

    private String createReducePrompt(List<String> partialSummaries) {
        StringBuilder sb = new StringBuilder();
        for (String summary : partialSummaries) {
            sb.append(summary).append("\n\n");
        }

        return String.format("""
            다음은 도서 리뷰들을 부분적으로 요약한 내용들입니다.
            이 내용들을 종합하여 사용자가 이 책을 살지 말지 결정하는 데 도움이 되는 평판 요약을 작성하세요.

            %s

            요약 데이터:
            %s
            """, OUTPUT_FORMAT, sb.toString());
    }

    public int getReduceThreshold() {
        return reduceThreshold;
    }
}
