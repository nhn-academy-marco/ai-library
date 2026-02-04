package com.nhnacademy.library.core.book.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
class ReviewSummarizerTest {

    @Autowired
    private ReviewSummarizer reviewSummarizer;

    @MockBean
    private ChatModel chatModel;

    @Test
    @DisplayName("리뷰가 25개일 때 Map-Reduce 로직이 정상적으로 동작해야 한다 (3번의 Map, 1번의 Reduce)")
    void testSummarizeReviewsWithManyItems() {
        // Given
        List<String> reviews = IntStream.range(0, 25)
                .mapToObj(i -> "Review content " + i)
                .toList();

        // Map 단계 호출 3회 (10개, 10개, 5개), Reduce 단계 호출 1회
        when(chatModel.call(anyString()))
                .thenReturn("Partial Summary") // Map 결과들
                .thenReturn("Partial Summary")
                .thenReturn("Partial Summary")
                .thenReturn("Final Integrated Summary"); // Reduce 결과

        // When
        String finalResult = reviewSummarizer.summarizeReviews(reviews);

        // Then
        assertThat(finalResult).isEqualTo("Final Integrated Summary");
        
        // 총 4번의 LLM 호출이 있어야 함
        verify(chatModel, times(4)).call(anyString());
    }

    @Test
    @DisplayName("리뷰가 없을 경우 적절한 메시지를 반환해야 한다")
    void testSummarizeReviewsWithNoContent() {
        // When
        String result = reviewSummarizer.summarizeReviews(List.of());

        // Then
        assertThat(result).contains("리뷰가 없어");
        verify(chatModel, never()).call(anyString());
    }
}
