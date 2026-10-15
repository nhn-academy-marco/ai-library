package net.java21.library.core.book.service.ai;

import org.springframework.ai.chat.model.ChatModel;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.java21.library.core.book.dto.BookAiRecommendationResponse;
import net.java21.library.core.book.dto.BookSearchResponse;
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
    /** 프롬프트에 넣는 도서 소개 최대 길이 (LLM 입력 토큰 절감) */
    private static final int MAX_BOOK_CONTENT_LENGTH = 150;

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
                truncate(book.getBookContent(), MAX_BOOK_CONTENT_LENGTH, "내용 없음")));

            context.append(bookContext);
        }

        log.info("[AI 추천] 컨텍스트 생성 완료, 길이: {}자", context.length());

        // CPU에서 도는 로컬 LLM은 입력·출력 토큰 수가 곧 응답 시간이므로 규칙을 짧게 유지합니다.
        String template = """
            query에 맞는 도서를 아래 도서 데이터에서 골라 JSON 배열로만 답하세요.

            [규칙]
            - relevance(0~100): 90 이상 직접 관련, 70~89 밀접, 50~69 간접 관련. 50 미만은 제외합니다.
            - 평점 4.5 이상이고 리뷰 20개 이상이면 +5, 평점 3.5 미만이면 -10을 반영합니다.
            - why: 한 문장, 60자 이내, 공손한 합니다체. 점수는 쓰지 않습니다. 리뷰 요약이 있으면 근거로 쓰고, 이유를 모르면 "추천 사유를 명확히 알 수 없습니다"라고 씁니다.
            - relevance 내림차순, 입력에 없는 정보는 추측하지 않습니다.
            - 마크다운이나 설명 없이 JSON만 출력합니다.

            형식: [{"id": 123, "relevance": 95, "why": "추천 사유"}]

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
            
            // 후보 도서에서 점수와 표시용 도서 정보를 채웁니다.
            // 후보에 없는 ID(LLM이 지어낸 ID)는 다른 도서로 연결되지 않도록 버립니다.
            List<BookAiRecommendationResponse> matched = new ArrayList<>();
            for (BookAiRecommendationResponse rec : recommendations) {
                books.stream()
                        .filter(b -> b.getId() != null && b.getId().equals(rec.getId()))
                        .findFirst()
                        .ifPresentOrElse(b -> {
                            rec.applyBook(b);
                            matched.add(rec);
                        }, () -> log.warn("[AI 추천] 후보에 없는 도서 ID를 제외합니다: {}", rec.getId()));
            }
            return matched;
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

    private static String truncate(String text, int maxLength, String fallback) {
        if (text == null || text.isBlank()) {
            return fallback;
        }
        String trimmed = text.strip();
        return trimmed.length() > maxLength ? trimmed.substring(0, maxLength) + "..." : trimmed;
    }
}
