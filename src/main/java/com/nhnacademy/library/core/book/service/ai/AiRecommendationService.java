package com.nhnacademy.library.core.book.service.ai;

import org.springframework.ai.chat.model.ChatModel;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.library.core.book.dto.BookAiRecommendationResponse;
import com.nhnacademy.library.core.book.dto.BookSearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 도서 추천 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiRecommendationService {

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;

    private static final int MAX_REVIEW_SUMMARY_LENGTH = 100;

    /**
     * 질문과 관련 도서 목록을 바탕으로 AI 추천 응답을 생성합니다.
     *
     * @param question 사용자의 질문
     * @param books    관련 도서 목록 (RRF 점수 기반 정렬됨)
     * @return AI 추천 응답 리스트
     */
    public List<BookAiRecommendationResponse> recommend(String question, List<BookSearchResponse> books) {
        if (books == null || books.isEmpty()) {
            return List.of();
        }

        log.info("[AI 추천] 시작, 질문: {}, 도서 수: {}", question, books.size());

        // Context 구성: RRF 점수 내림차순으로 정렬하여 가장 중요한 정보가 컨텍스트의 앞부분에 오도록 함
        List<BookSearchResponse> sortedBooks = new ArrayList<>(books);
        sortedBooks.sort((b1, b2) -> {
            Double s1 = b1.getRrfScore() != null ? b1.getRrfScore() : 0.0;
            Double s2 = b2.getRrfScore() != null ? b2.getRrfScore() : 0.0;
            return s2.compareTo(s1);
        });

        StringBuilder context = new StringBuilder();
        for (BookSearchResponse book : sortedBooks) {
            // 평점 정보 구성
            String ratingInfo = formatRatingInfo(book);
            log.debug("[AI 추천] ID={}, 평점={}, 리뷰수={}",
                book.getId(), book.getAverageRating(), book.getReviewCount());

            // 리뷰 요약 구성 (100자 제한, null 처리)
            String summaryInfo = formatReviewSummary(book.getReviewSummary());
            log.debug("[AI 추천] ID={}, 요약길이={}",
                book.getId(), summaryInfo.length());

            // 컨텍스트 구성
            StringBuilder bookContext = new StringBuilder();
            bookContext.append(String.format("ID: %d, 제목: %s, 저자: %s",
                book.getId(), book.getTitle(), book.getAuthorName()));

            // 평점 정보가 있으면 추가
            if (!ratingInfo.isEmpty()) {
                bookContext.append(", ").append(ratingInfo);
            }

            // 리뷰 요약이 있으면 추가
            if (!summaryInfo.isEmpty()) {
                bookContext.append(", ").append(summaryInfo);
            }

            // 출판일과 내용 추가
            bookContext.append(String.format(", 출판일: %s, 내용: %s\n",
                book.getEditionPublishDate() != null ? book.getEditionPublishDate().toString() : "알 수 없음",
                book.getBookContent() != null ? book.getBookContent() : "내용 없음"));

            context.append(bookContext);
        }

        log.info("[AI 추천] 컨텍스트 생성 완료, 길이: {}자", context.length());

        String template = """
            [규칙]
            - 사용자가 제공하는 query와 가장 관련 있는 도서를 선별하세요.
            - 각 도서에 대해 relevance 점수(0~100)를 부여하세요:
              - 90–100: query와 직접적으로 강하게 연관, 주제 적합성이 매우 높음
              - 70–89: query와 밀접하게 관련 있지만 일부 범위가 제한적임
              - 50–69: query와 간접적으로 관련, 배경 지식에 도움이 됨
              - 50 미만: 관련성이 낮으므로 출력에서 제외
            - 추천 사유("why")에는 점수를 포함하지 말고, 순수하게 이유만 설명하세요.
            - 추천 사유("why")는 사용자에게 친절하고 공손한 어투(예: "~입니다", "~를 추천해 드립니다")로 작성하세요.
            - 추천 사유를 명확히 알 수 없는 경우에는 "추천 사유를 모름" 또는 "추천 사유를 명확히 알 수 없습니다"와 같이 명확하게 모른다는 표현을 사용하세요.
            - 최신 출간일과 query와의 직접적인 관련성을 함께 고려하세요.

            [규칙 - 리뷰 정보 반영]
            - 평점과 리뷰 수를 relevance 점수에 반영하세요:
              - 평점 4.5 이상 + 리뷰 20개 이상: relevance +5점 (검증된 인기 도서)
              - 평점 4.0 이상 + 리뷰 20개 이상: relevance +3점 (검증된 도서)
              - 평점 3.5 미만: relevance -10점 (부정적 평가)
              - 리뷰 없음: relevance ±0점 (변동 없음)

            - 평점이 4.0 이상인 도서를 우선적으로 고려하세요.
            - 리뷰가 20개 이상인 도서는 검증된 도서로 판단하고 신뢰도를 높게 평가하세요.
            - 리뷰 요약이 있는 경우, 요약의 내용을 참고하여 구체적인 추천 사유를 작성하세요.
              - 예: "리뷰어들이 '설명이 쉽고 실전 예제가 많다'고 평가했습니다."
            - 평점이 3.5 미만인 도서는 주의를 권장하세요.
              - 예: "평점이 3.2/5.0로 평가가 엇갈립니다. 리뷰 내용을 확인해보세요."
            - 리뷰가 없는 신간 도서는 "신간 도서로 아직 리뷰가 없습니다"라고 언급하고, 최신성과 내용의 관련성을 더 중요하게 평가하세요.

            [출력 형식]
            - 출력은 반드시 순수 JSON만 포함하세요.
            - 마크다운 코드 블록(```json ... ```)이나 추가 설명 텍스트는 절대 포함하지 마세요.
            - 언어는 반드시 한국어를 사용하세요.

            [JSON STRUCTURE]
             [
                {
                  "id": 123,
                  "relevance": 95,
                  "why": "추천 사유"
                }
             ]

            - 결과는 relevance 기준 내림차순으로 정렬하세요.
            - 입력 데이터에 없는 필드는 추측하지 마세요.

            query: {question}

            도서 데이터:
            {context}
            """;

        String renderedPrompt = template
                .replace("{question}", question)
                .replace("{context}", context.toString());

        String rawResponse = chatModel.call(renderedPrompt);
        log.debug("AI Raw Response: {}", rawResponse);

        try {
            // JSON 응답에서 마크다운 코드 블록 제거
            String jsonPart = rawResponse.replaceAll("```json", "").replaceAll("```", "").trim();
            List<BookAiRecommendationResponse> recommendations = objectMapper.readValue(jsonPart, new TypeReference<List<BookAiRecommendationResponse>>() {});
            
            // 검색된 도서 리스트에서 유사도 및 RRF 점수 매핑
            for (BookAiRecommendationResponse rec : recommendations) {
                books.stream()
                        .filter(b -> b.getId().equals(rec.getId()))
                        .findFirst()
                        .ifPresent(b -> {
                            rec.setSimilarity(b.getSimilarity());
                            rec.setRrfScore(b.getRrfScore());
                        });
            }
            return recommendations;
        } catch (Exception e) {
            log.error("Failed to parse AI response: {}", rawResponse, e);
            return List.of();
        }
    }

    /**
     * 평점 정보 포맷팅
     *
     * @return "평점: 4.8/5.0(127개 리뷰)" 또는 빈 문자열
     */
    private String formatRatingInfo(BookSearchResponse book) {
        if (book.getAverageRating() != null
                && book.getReviewCount() != null
                && book.getReviewCount() > 0) {
            return String.format("평점: %.1f/5.0(%d개 리뷰)",
                book.getAverageRating(),
                book.getReviewCount());
        }
        return "";  // 리뷰 없으면 빈 문자열
    }

    /**
     * 리뷰 요약 포맷팅
     *
     * @return "요약: '리뷰 내용...'" 또는 빈 문자열
     */
    private String formatReviewSummary(String summary) {
        if (summary == null || summary.isBlank()) {
            return "";  // 요약 없으면 빈 문자열
        }

        // 100자 제한
        String truncated = summary.length() > MAX_REVIEW_SUMMARY_LENGTH
            ? summary.substring(0, MAX_REVIEW_SUMMARY_LENGTH) + "..."
            : summary;

        return String.format("요약: \"%s\"", truncated);
    }
}
