package com.nhnacademy.library.core.book.service.embedding;
import com.nhnacademy.library.core.book.service.search.BookSearchService;
import com.nhnacademy.library.core.book.service.embedding.EmbeddingService;
import com.nhnacademy.library.core.book.service.embedding.BookEmbeddingService;
import com.nhnacademy.library.core.review.service.ReviewSummarizer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
class EmbeddingServiceTest {

    @Autowired
    private EmbeddingService embeddingService;

    @Test
    @DisplayName("단일 텍스트 임베딩 생성 테스트")
    void testGetEmbedding() {
        String text = "안녕하세요";
        float[] embedding = embeddingService.getEmbedding(text);
        
        assertThat(embedding).isNotNull();
        assertThat(embedding.length).isEqualTo(1024);

        log.debug("Embedding length: {}", embedding.length);
    }

    @Test
    @DisplayName("여러 텍스트 일괄 임베딩 생성 테스트")
    void testGetEmbeddings() {
        List<String> texts = List.of("안녕하세요", "반갑습니다");
        List<float[]> embeddings = embeddingService.getEmbeddings(texts);

        assertThat(embeddings).hasSize(2);
        assertThat(embeddings.get(0)).hasSize(1024);
        assertThat(embeddings.get(1)).hasSize(1024);

        log.debug("Batch embeddings size: {}", embeddings.size());
    }
}
