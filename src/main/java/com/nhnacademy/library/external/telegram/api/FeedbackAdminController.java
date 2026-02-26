package com.nhnacademy.library.external.telegram.api;

import com.nhnacademy.library.external.telegram.dto.FeedbackStats;
import com.nhnacademy.library.external.telegram.entity.SearchFeedback;
import com.nhnacademy.library.external.telegram.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 피드백 관리자 API 컨트롤러
 *
 * <p>피드백 통계 조회, 분석을 위한 관리자용 REST API를 제공합니다.
 *
 * <p>API 엔드포인트:
 * <ul>
 *   <li>GET /api/admin/feedback/book/{bookId}/stats - 도서별 피드백 통계</li>
 *   <li>GET /api/admin/feedback/stats - 검색어별 피드백 통계</li>
 *   <li>GET /api/admin/feedback/recent - 최근 피드백 목록</li>
 *   <li>GET /api/admin/feedback/user/{chatId} - 사용자별 피드백 목록</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/feedback")
@RequiredArgsConstructor
public class FeedbackAdminController {

    private final FeedbackService feedbackService;

    /**
     * 도서별 피드백 통계를 조회합니다.
     *
     * @param bookId 도서 ID
     * @return 피드백 통계
     */
    @GetMapping("/book/{bookId}/stats")
    public ResponseEntity<FeedbackStats> getBookFeedbackStats(@PathVariable("bookId") Long bookId) {
        log.info("[Admin API] Getting feedback stats for bookId: {}", bookId);
        FeedbackStats stats = feedbackService.getBookFeedbackStats(bookId);
        return ResponseEntity.ok(stats);
    }

    /**
     * 검색어별 피드백 통계를 조회합니다.
     *
     * @param query 검색어
     * @return 피드백 통계
     */
    @GetMapping("/stats")
    public ResponseEntity<FeedbackStats> getQueryFeedbackStats(@RequestParam("query") String query) {
        log.info("[Admin API] Getting feedback stats for query: {}", query);
        FeedbackStats stats = feedbackService.getQueryFeedbackStats(query);
        return ResponseEntity.ok(stats);
    }

    /**
     * 최근 N일간의 피드백 목록을 조회합니다.
     *
     * @param days 일수 (기본값 7일, 최대 30일)
     * @return 피드백 목록
     */
    @GetMapping("/recent")
    public ResponseEntity<List<SearchFeedback>> getRecentFeedback(
            @RequestParam(value = "days", defaultValue = "7") int days) {

        // 최대 30일로 제한
        int validDays = Math.min(Math.max(days, 1), 30);
        log.info("[Admin API] Getting recent feedback for last {} days", validDays);

        List<SearchFeedback> feedbacks = feedbackService.getRecentFeedback(validDays);
        return ResponseEntity.ok(feedbacks);
    }

    /**
     * 특정 사용자의 피드백 목록을 조회합니다.
     *
     * @param chatId Telegram 사용자 ID
     * @return 피드백 목록
     */
    @GetMapping("/user/{chatId}")
    public ResponseEntity<List<SearchFeedback>> getUserFeedback(@PathVariable("chatId") Long chatId) {
        log.info("[Admin API] Getting feedback for user chatId: {}", chatId);
        List<SearchFeedback> feedbacks = feedbackService.getUserFeedback(chatId);
        return ResponseEntity.ok(feedbacks);
    }
}
