package com.nhnacademy.library.core.book.service.ai;
import com.nhnacademy.library.core.book.service.search.BookSearchService;
import org.springframework.ai.chat.model.ChatModel;
import com.nhnacademy.library.core.book.dto.BookSearchResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
class BookContextReorderingTest {

    @Autowired
    private BookSearchService bookSearchService;

    @Autowired
    private AiRecommendationService aiRecommendationService;

    @MockitoBean
    private ChatModel chatModel;

    @Test
    @DisplayName("RRF 점수가 높은 도서가 컨텍스트의 앞부분에 위치해야 한다 (Mission 3)")
    @SuppressWarnings("unchecked")
    void testContextReorderingByRrfScore() {
        // Given
        BookSearchResponse book1 = new BookSearchResponse(1L, "isbn1", "낮은점수도서", null, "저자1", null, null, null, null, "내용1", 0.1, 0.01);
        BookSearchResponse book2 = new BookSearchResponse(2L, "isbn2", "높은점수도서", null, "저자2", null, null, null, null, "내용2", 0.9, 0.05);
        
        // Reflection을 사용하여 private 메서드인 generateAiResponse를 직접 테스트하거나,
        // searchBooks의 흐름을 이용하되 hybridSearch 결과를 모킹하는 것이 정석이지만
        // 여기서는 generateAiResponse 내부의 정렬 로직이 실제 프롬프트에 반영되는지 확인합니다.
        
        when(chatModel.call(anyString())).thenReturn("[]");

        // When: AiRecommendationService를 통해 간접적으로 검증
        ReflectionTestUtils.invokeMethod(aiRecommendationService, "recommend", "질문", Arrays.asList(book1, book2));

        // Then
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(chatModel).call(promptCaptor.capture());
        
        String renderedPrompt = promptCaptor.getValue();
        int indexBook1 = renderedPrompt.indexOf("낮은점수도서");
        int indexBook2 = renderedPrompt.indexOf("높은점수도서");
        
        // 높은 점수 도서(0.05)가 낮은 점수 도서(0.01)보다 먼저 나와야 함
        assertThat(indexBook2).isLessThan(indexBook1);
    }
}
