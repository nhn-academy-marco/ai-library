package com.nhnacademy.library.core.book.service.embedding;
import com.nhnacademy.library.core.book.service.search.BookSearchService;
import com.nhnacademy.library.core.book.service.embedding.EmbeddingService;
import com.nhnacademy.library.core.book.service.embedding.BookEmbeddingService;
import com.nhnacademy.library.core.review.service.ReviewSummarizer;

import com.nhnacademy.library.core.book.domain.Book;
import com.nhnacademy.library.core.book.repository.BookRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.ai.selected-model=ollama",
    "rabbitmq.queue.review-summary=nhnacademy-library-review"
})
@Transactional
class BookEmbeddingServiceTest {

    @Autowired
    private BookEmbeddingService bookEmbeddingService;

    @MockitoBean
    private BookRepository bookRepository;

    @MockitoBean
    private EmbeddingService embeddingService;

    @Test
    @DisplayName("제목과 내용이 모두 있는 도서에 대해서만 임베딩이 생성되어야 한다")
    void testProcessEmptyEmbeddingsWithTitleAndContent() {
        // Given
        Book bookWithContent = new Book(
                "1234567890", null, "자바의 정석", "남궁성", "도우출판",
                null, null, null, "자바 기초 강의", null, null
        );
        
        when(bookRepository.findAllByTitleIsNotNullAndBookContentIsNotNullAndEmbeddingIsNull(any(Pageable.class)))
                .thenReturn(List.of(bookWithContent));
        
        when(embeddingService.getEmbeddings(any()))
                .thenReturn(List.of(new float[1024]));

        // When
        int processedCount = bookEmbeddingService.processEmptyEmbeddings(10);

        // Then
        assertThat(processedCount).isEqualTo(1);
    }

    @Test
    @DisplayName("한자 제목이 포함된 도서도 정상적으로 전처리되어 임베딩이 생성되어야 한다")
    void testProcessEmptyEmbeddingsWithChineseCharacters() {
        // Given
        Book chineseBook = new Book(
                "9787521724752", null, "世界文明立体翻翻书：回到古罗马", "波·琼·埃尔남데스 著", "Publisher",
                null, null, null, "중국어 내용 포함", null, null
        );

        when(bookRepository.findAllByTitleIsNotNullAndBookContentIsNotNullAndEmbeddingIsNull(any(Pageable.class)))
                .thenReturn(List.of(chineseBook));

        when(embeddingService.getEmbeddings(any()))
                .thenReturn(List.of(new float[1024]));

        // When
        int processedCount = bookEmbeddingService.processEmptyEmbeddings(10);

        // Then
        assertThat(processedCount).isEqualTo(1);
    }
}
