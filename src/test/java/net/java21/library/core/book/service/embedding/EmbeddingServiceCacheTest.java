package net.java21.library.core.book.service.embedding;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("EmbeddingService 검색어 임베딩 캐시 테스트")
class EmbeddingServiceCacheTest {

    private static final long TTL_MINUTES = 60;
    private static final long MAX_SIZE = 10_000;

    private EmbeddingModel embeddingModel;
    private EmbeddingService embeddingService;

    @BeforeEach
    void setUp() {
        embeddingModel = mock(EmbeddingModel.class);
        embeddingService = new EmbeddingService(embeddingModel, TTL_MINUTES, MAX_SIZE);
    }

    @Test
    @DisplayName("같은 검색어를 두 번 임베딩하면 모델은 한 번만 호출된다")
    void sameKeywordCallsModelOnce() {
        float[] vector = {0.1f, 0.2f};
        when(embeddingModel.embed(anyString())).thenReturn(vector);

        float[] first = embeddingService.getEmbedding("spring");
        float[] second = embeddingService.getEmbedding("spring");

        assertThat(second).isSameAs(first);
        verify(embeddingModel, times(1)).embed(anyString());
    }

    @Test
    @DisplayName("앞뒤 공백·대소문자·연속 공백만 다른 검색어는 같은 캐시 키를 쓴다")
    void normalizedKeywordsShareCacheEntry() {
        when(embeddingModel.embed(anyString())).thenReturn(new float[]{0.3f});

        embeddingService.getEmbedding("  Spring   Boot ");
        embeddingService.getEmbedding("spring boot");

        verify(embeddingModel, times(1)).embed("Spring Boot");
        assertThat(EmbeddingService.normalizeKey("  Spring \t  Boot ")).isEqualTo("spring boot");
    }

    @Test
    @DisplayName("빈 임베딩 결과는 캐시하지 않는다")
    void emptyResultIsNotCached() {
        when(embeddingModel.embed(anyString())).thenReturn(null);

        embeddingService.getEmbedding("spring");
        embeddingService.getEmbedding("spring");

        verify(embeddingModel, times(2)).embed(anyString());
    }

    @Test
    @DisplayName("도서 임베딩 배치(getEmbeddings)는 캐시하지 않는다")
    void batchEmbeddingsAreNotCached() {
        when(embeddingModel.embed(anyList())).thenReturn(List.of(new float[]{0.1f}));

        embeddingService.getEmbeddings(List.of("책 소개"));
        embeddingService.getEmbeddings(List.of("책 소개"));

        verify(embeddingModel, times(2)).embed(anyList());
    }
}
