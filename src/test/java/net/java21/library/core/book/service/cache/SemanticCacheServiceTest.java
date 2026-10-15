package net.java21.library.core.book.service.cache;

import net.java21.library.core.book.domain.BookSearchCache;
import net.java21.library.core.book.domain.SearchType;
import net.java21.library.core.book.dto.BookAiRecommendationResponse;
import net.java21.library.core.book.dto.BookSearchRequest;
import net.java21.library.core.book.dto.BookSearchResponse;
import net.java21.library.core.book.repository.InMemoryBookSearchCacheRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SemanticCacheService: AI 추천만 저장·조회")
class SemanticCacheServiceTest {

    private static final float[] VECTOR = {0.1f, 0.2f, 0.3f};

    private InMemoryBookSearchCacheRepository repository;
    private SemanticCacheService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryBookSearchCacheRepository(10);
        service = new SemanticCacheService(repository);
        ReflectionTestUtils.setField(service, "cacheTtlMinutes", 30);
    }

    private static BookAiRecommendationResponse rec(long id) {
        BookAiRecommendationResponse rec = new BookAiRecommendationResponse();
        rec.setId(id);
        rec.setRelevance(80);
        rec.setWhy("사유");
        return rec;
    }

    private static BookSearchRequest request() {
        return new BookSearchRequest("자바", null, SearchType.RAG, VECTOR);
    }

    @Test
    @DisplayName("저장 시 도서 목록은 저장하지 않고 AI 추천만 저장하며, 같은 벡터로 조회된다")
    void save_storesRecommendationsOnly() {
        service.save(request(), List.of(rec(1L)));

        BookSearchCache saved = repository.findAll().iterator().next();
        assertThat(saved.getBooks()).isNull();
        assertThat(saved.getAiResponse()).hasSize(1);
        assertThat(service.findRecommendations(request()))
                .hasValueSatisfying(recs -> assertThat(recs).extracting(BookAiRecommendationResponse::getId).containsExactly(1L));
    }

    @Test
    @DisplayName("이전 버전 항목(books 목록 + 도서 정보 없는 추천)도 오류 없이 읽고 도서 정보를 채운다")
    void legacyEntry_filledFromBooks() {
        BookSearchResponse book = new BookSearchResponse(1L, "9791156759270", "자바의 정석", null, "남궁성", "도우출판",
                null, null, null, "소개", 0.9, 0.03);
        repository.save(BookSearchCache.builder()
                .keyword("자바").vector(VECTOR)
                .books(List.of(book))
                .aiResponse(List.of(rec(1L), rec(99L)))
                .createdAt(System.currentTimeMillis())
                .build());

        List<BookAiRecommendationResponse> recs = service.findRecommendations(request()).orElseThrow();

        assertThat(recs.get(0).getTitle()).isEqualTo("자바의 정석");
        assertThat(recs.get(0).getIsbn()).isEqualTo("9791156759270");
        assertThat(recs.get(1).getTitle()).isNull();
    }

    @Test
    @DisplayName("TTL이 지난 항목은 조회되지 않고 삭제된다")
    void expiredEntry_evicted() {
        repository.save(BookSearchCache.builder()
                .keyword("자바").vector(VECTOR).aiResponse(List.of(rec(1L)))
                .createdAt(System.currentTimeMillis() - 31 * 60 * 1000L)
                .build());

        assertThat(service.findRecommendations(request())).isEmpty();
        assertThat(repository.findAll()).isEmpty();
    }
}
