package com.nhnacademy.library.core.review.service;

import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
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
    private static final int MAP_CHUNK_SIZE = 10;
    private static final int REDUCE_THRESHOLD = 10; // Full Rebuild 기준

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
        List<List<String>> chunks = Lists.partition(reviews, MAP_CHUNK_SIZE);

        for (int i = 0; i < chunks.size(); i++) {
            log.info("Map step: processing chunk {}/{}", i + 1, chunks.size());
            String mapPrompt = createMapPrompt(chunks.get(i));
            String summary = chatModel.call(mapPrompt);
            partialSummaries.add(summary);
        }

        // 2. Reduce 단계: 부분 요약들을 합쳐 최종 요약 생성
        log.info("Reduce step: combining {} partial summaries.", partialSummaries.size());
        String reducePrompt = createReducePrompt(partialSummaries);
        String finalSummary = chatModel.call(reducePrompt);

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
        List<List<String>> chunks = Lists.partition(newReviews, MAP_CHUNK_SIZE);

        for (int i = 0; i < chunks.size(); i++) {
            log.info("Map step: processing new reviews chunk {}/{}", i + 1, chunks.size());
            String mapPrompt = createMapPrompt(chunks.get(i));
            String summary = chatModel.call(mapPrompt);
            partialSummaries.add(summary);
        }

        // 2. 기존 요약과 새 리뷰 요약본들을 병합
        log.info("Reduce step: merging existing summary with {} new partial summaries.", partialSummaries.size());
        String mergePrompt = createMergePrompt(existingSummary, partialSummaries);
        String finalSummary = chatModel.call(mergePrompt);

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

            위 두 내용을 통합해서 전체 리뷰를 종합한 '최종 요약'을 작성해주세요.
            기존 요약의 핵심 내용은 유지하면서, 새로운 정보를 추가해주세요.
            항상 [장점], [단점], [총평]의 형식을 유지하여 답변하세요.
            답변은 반드시 공손하고 친절한 어조(~입니다, ~하세요)를 사용하세요.
            """, existingSummary != null ? existingSummary : "없음", newSummaries.toString());
    }

    private String createMapPrompt(List<String> reviews) {
        StringBuilder sb = new StringBuilder();
        for (String review : reviews) {
            sb.append("- ").append(review).append("\n");
        }

        return String.format("""
            다음은 특정 도서에 대한 독자 리뷰들입니다.
            이 리뷰들에서 공통적으로 언급되는 [장점], [단점], [추천대상]을 각각 한 문장씩 요약하세요.

            리뷰 리스트:
            %s
            """, sb.toString());
    }

    private String createReducePrompt(List<String> partialSummaries) {
        StringBuilder sb = new StringBuilder();
        for (String summary : partialSummaries) {
            sb.append(summary).append("\n\n");
        }

        return String.format("""
            다음은 도서 리뷰들을 부분적으로 요약한 내용들입니다.
            이 내용들을 종합하여 사용자가 이 책을 살지 말지 결정하는 데 도움이 되는 '최종 평판 요약'을 작성하세요.
            항상 [장점], [단점], [총평]의 형식을 유지하여 답변하세요.
            답변은 반드시 공손하고 친절한 어조(~입니다, ~하세요)를 사용하세요.

            요약 데이터:
            %s
            """, sb.toString());
    }

    public int getReduceThreshold() {
        return REDUCE_THRESHOLD;
    }
}
