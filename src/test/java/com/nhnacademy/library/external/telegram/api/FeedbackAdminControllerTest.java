package com.nhnacademy.library.external.telegram.api;

import com.nhnacademy.library.external.telegram.dto.FeedbackStats;
import com.nhnacademy.library.external.telegram.dto.FeedbackType;
import com.nhnacademy.library.external.telegram.entity.SearchFeedback;
import com.nhnacademy.library.external.telegram.service.FeedbackService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * FeedbackAdminController 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("피드백 관리자 API 컨트롤러 테스트")
class FeedbackAdminControllerTest {

    @Mock
    private FeedbackService feedbackService;

    @InjectMocks
    private FeedbackAdminController controller;

    @Test
    @DisplayName("도서별 피드백 통계 조회")
    void testGetBookFeedbackStats() {
        // Given
        Long bookId = 1L;
        FeedbackStats stats = new FeedbackStats(10, 2, 12, 0.833, 0.666);
        when(feedbackService.getBookFeedbackStats(bookId)).thenReturn(stats);

        // When
        ResponseEntity<FeedbackStats> response = controller.getBookFeedbackStats(bookId);

        // Then
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().goodCount()).isEqualTo(10);
        assertThat(response.getBody().badCount()).isEqualTo(2);
        assertThat(response.getBody().totalCount()).isEqualTo(12);
    }

    @Test
    @DisplayName("검색어별 피드백 통계 조회")
    void testGetQueryFeedbackStats() {
        // Given
        String query = "해리포터";
        FeedbackStats stats = new FeedbackStats(15, 3, 18, 0.833, 0.666);
        when(feedbackService.getQueryFeedbackStats(query)).thenReturn(stats);

        // When
        ResponseEntity<FeedbackStats> response = controller.getQueryFeedbackStats(query);

        // Then
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().goodCount()).isEqualTo(15);
        assertThat(response.getBody().badCount()).isEqualTo(3);
        assertThat(response.getBody().totalCount()).isEqualTo(18);
    }

    @Test
    @DisplayName("최근 피드백 목록 조회")
    void testGetRecentFeedback() {
        // Given
        SearchFeedback feedback1 = SearchFeedback.of(123L, "해리포터", 1L, FeedbackType.GOOD);
        SearchFeedback feedback2 = SearchFeedback.of(456L, "마법사의 돌", 2L, FeedbackType.BAD);
        List<SearchFeedback> feedbacks = List.of(feedback1, feedback2);
        when(feedbackService.getRecentFeedback(eq(7))).thenReturn(feedbacks);

        // When
        ResponseEntity<List<SearchFeedback>> response = controller.getRecentFeedback(7);

        // Then
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody().get(0).getType()).isEqualTo(FeedbackType.GOOD);
        assertThat(response.getBody().get(1).getType()).isEqualTo(FeedbackType.BAD);
    }

    @Test
    @DisplayName("최대 30일로 제한")
    void testGetRecentFeedback_MaxDays() {
        // Given
        when(feedbackService.getRecentFeedback(eq(30))).thenReturn(List.of());

        // When
        ResponseEntity<List<SearchFeedback>> response = controller.getRecentFeedback(100);

        // Then
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
    }

    @Test
    @DisplayName("최소 1일로 제한")
    void testGetRecentFeedback_MinDays() {
        // Given
        when(feedbackService.getRecentFeedback(eq(1))).thenReturn(List.of());

        // When
        ResponseEntity<List<SearchFeedback>> response = controller.getRecentFeedback(0);

        // Then
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
    }

    @Test
    @DisplayName("사용자별 피드백 목록 조회")
    void testGetUserFeedback() {
        // Given
        Long chatId = 123L;
        SearchFeedback feedback1 = SearchFeedback.of(chatId, "해리포터", 1L, FeedbackType.GOOD);
        SearchFeedback feedback2 = SearchFeedback.of(chatId, "마법사의 돌", 2L, FeedbackType.GOOD);
        List<SearchFeedback> feedbacks = List.of(feedback1, feedback2);
        when(feedbackService.getUserFeedback(chatId)).thenReturn(feedbacks);

        // When
        ResponseEntity<List<SearchFeedback>> response = controller.getUserFeedback(chatId);

        // Then
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody()).allMatch(f -> f.getChatId().equals(chatId));
    }
}
