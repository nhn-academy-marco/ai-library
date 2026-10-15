package net.java21.library.external.telegram.api;

import net.java21.library.external.telegram.dto.FeedbackStats;
import net.java21.library.external.telegram.entity.SearchFeedback;
import net.java21.library.external.telegram.service.FeedbackService;
import net.java21.library.external.telegram.util.CsvExportService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
@Validated
public class FeedbackAdminController {

    private final FeedbackService feedbackService;
    private final CsvExportService csvExportService;

    /**
     * 도서별 피드백 통계를 조회합니다.
     *
     * @param bookId 도서 ID (양수)
     * @return 피드백 통계
     */
    @GetMapping("/book/{bookId}/stats")
    public ResponseEntity<FeedbackStats> getBookFeedbackStats(
            @PathVariable("bookId") @Positive(message = "도서 ID는 양수여야 합니다") Long bookId) {
        log.info("[Admin API] Getting feedback stats for bookId: {}", bookId);
        FeedbackStats stats = feedbackService.getBookFeedbackStats(bookId);
        return ResponseEntity.ok(stats);
    }

    /**
     * 검색어별 피드백 통계를 조회합니다.
     *
     * @param query 검색어 (비어있지 않아야 함)
     * @return 피드백 통계
     */
    @GetMapping("/stats")
    public ResponseEntity<FeedbackStats> getQueryFeedbackStats(
            @RequestParam("query") @NotBlank(message = "검색어는 비어있을 수 없습니다")
            @Size(max = 100, message = "검색어는 100자 이하여야 합니다") String query) {
        log.info("[Admin API] Getting feedback stats for query: {}", query);
        FeedbackStats stats = feedbackService.getQueryFeedbackStats(query);
        return ResponseEntity.ok(stats);
    }

    /**
     * 최근 N일간의 피드백 목록을 조회합니다.
     *
     * @param days 일수 (기본값 7일, 최소 1일, 최대 30일)
     * @return 피드백 목록
     */
    @GetMapping("/recent")
    public ResponseEntity<List<SearchFeedback>> getRecentFeedback(
            @RequestParam(value = "days", defaultValue = "7")
            @Min(value = 1, message = "일수는 최소 1일 이상이어야 합니다")
            @Max(value = 30, message = "일수는 최대 30일 이하여야 합니다") int days) {

        log.info("[Admin API] Getting recent feedback for last {} days", days);

        List<SearchFeedback> feedbacks = feedbackService.getRecentFeedback(days);
        return ResponseEntity.ok(feedbacks);
    }

    /**
     * 특정 사용자의 피드백 목록을 조회합니다.
     *
     * @param chatId Telegram 사용자 ID (양수)
     * @return 피드백 목록
     */
    @GetMapping("/user/{chatId}")
    public ResponseEntity<List<SearchFeedback>> getUserFeedback(
            @PathVariable("chatId") @Positive(message = "사용자 ID는 양수여야 합니다") Long chatId) {
        log.info("[Admin API] Getting feedback for user chatId: {}", chatId);
        List<SearchFeedback> feedbacks = feedbackService.getUserFeedback(chatId);
        return ResponseEntity.ok(feedbacks);
    }

    /**
     * 전체 피드백 데이터를 CSV 파일로 내보냅니다.
     *
     * @return CSV 파일
     */
    @GetMapping(value = "/export/csv", produces = "text/csv; charset=UTF-8")
    public ResponseEntity<String> exportFeedbacksAsCsv() {
        log.info("[Admin API] Exporting all feedback as CSV");

        List<SearchFeedback> feedbacks = feedbackService.getAllFeedback();
        String csv = csvExportService.generateCsv(feedbacks);

        // 한국어 파일명을 위한 URL 인코딩
        String filename = "feedbacks_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";
        String encodedFilename = new String(filename.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                java.nio.charset.StandardCharsets.ISO_8859_1);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.setContentDispositionFormData("attachment", encodedFilename);
        headers.add("Access-Control-Expose-Headers", "Content-Disposition");

        return ResponseEntity.ok()
                .headers(headers)
                .body("\uFEFF" + csv); // UTF-8 BOM 추가 (엑셀에서 한글 깨짐 방지)
    }
}
