package net.java21.library.core.book.service.search;

import net.java21.library.core.book.domain.Book;
import net.java21.library.core.book.domain.SearchType;
import net.java21.library.core.book.dto.BookSearchRequest;
import net.java21.library.core.book.dto.BookSearchResponse;
import net.java21.library.core.book.repository.BookRepository;
import net.java21.library.core.book.service.embedding.EmbeddingService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 검색 결과 순서의 결정성 검증 (Testcontainers pgvector)
 *
 * <p>검색 결과 캐시는 테스트 설정에서 즉시 만료(cache.search-result.ttl.minutes=0)되므로
 * 매 호출이 실제로 DB 조회·RRF를 수행합니다. 즉 캐시 없이도 같은 결과가 나오는지 검증합니다.</p>
 */
@SpringBootTest
@Transactional
class SearchDeterminismTest {

    private static final int DIMENSION = 1024;
    private static final String KEYWORD = "결정성키워드";

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookSearchService bookSearchService;

    @Autowired
    private RrfService rrfService;

    @PersistenceContext
    private EntityManager entityManager;

    @MockitoBean
    private EmbeddingService embeddingService;

    private Book save(String isbn, String title, String author, LocalDate editionDate, float[] embedding) {
        Book book = new Book(isbn, null, title, author, "출판사", editionDate, BigDecimal.TEN, null, "소개", null, editionDate);
        if (embedding != null) {
            book.updateEmbedding(embedding);
        }
        return bookRepository.save(book);
    }

    private static float[] axis(int index, float value) {
        float[] v = new float[DIMENSION];
        v[index] = value;
        return v;
    }

    @Test
    @DisplayName("키워드 검색은 제목 일치 → 출간일 최신 → ID 순으로 정렬한다")
    void keywordSearch_ordersByTitleMatchThenDateThenId() {
        Book authorOnly = save("9990000000001", "다른 제목", KEYWORD + " 저자", LocalDate.of(2025, 1, 1), null);
        Book titleOld = save("9990000000002", KEYWORD + " 입문", "저자", LocalDate.of(2020, 1, 1), null);
        Book titleNewA = save("9990000000003", KEYWORD + " 실전", "저자", LocalDate.of(2024, 1, 1), null);
        Book titleNewB = save("9990000000004", KEYWORD + " 심화", "저자", LocalDate.of(2024, 1, 1), null);
        bookRepository.flush();

        List<Long> ids = bookRepository.search(PageRequest.of(0, 10),
                        new BookSearchRequest(KEYWORD, null, SearchType.KEYWORD, null))
                .map(BookSearchResponse::getId).getContent();

        assertThat(ids).containsExactly(titleNewA.getId(), titleNewB.getId(), titleOld.getId(), authorOnly.getId());
    }

    @Test
    @DisplayName("벡터 검색은 거리 오름차순(유사도 내림차순)이고, 거리가 같으면 ID 오름차순이며 유사도 값은 1 - 거리이다")
    void vectorSearch_ordersByDistanceThenId() {
        int dim = 1000; // 다른 테스트 데이터와 겹치지 않는 축
        Book far = save("9990000000011", "벡터 먼 책", "저자", LocalDate.of(2024, 1, 1),
                mixed(dim));
        Book tieA = save("9990000000012", "벡터 같은 책 A", "저자", LocalDate.of(2024, 1, 1), axis(dim, 1f));
        Book tieB = save("9990000000013", "벡터 같은 책 B", "저자", LocalDate.of(2024, 1, 1), axis(dim, 1f));
        bookRepository.flush();
        Set<Long> mine = Set.of(far.getId(), tieA.getId(), tieB.getId());

        List<BookSearchResponse> results = bookRepository.vectorSearch(PageRequest.of(0, 100),
                        new BookSearchRequest(null, null, SearchType.VECTOR, axis(dim, 1f)))
                .getContent().stream().filter(r -> mine.contains(r.getId())).toList();

        assertThat(results).extracting(BookSearchResponse::getId).containsExactly(tieA.getId(), tieB.getId(), far.getId());
        assertThat(results.get(0).getSimilarity()).isCloseTo(1.0, within(1e-6));
        assertThat(results.get(2).getSimilarity()).isCloseTo(Math.sqrt(0.5), within(1e-6));
    }

    @Test
    @DisplayName("후보 조회(count 없음)는 페이지 조회와 같은 순서의 결과를 돌려준다")
    void candidates_sameOrderAsPagedQueries() {
        int dim = 1002;
        for (int i = 0; i < 5; i++) {
            save(String.format("99900000002%02d", i), KEYWORD + " 후보 " + i, "저자", LocalDate.of(2020 + i, 1, 1),
                    i % 2 == 0 ? axis(dim, 1f) : mixed(dim));
        }
        bookRepository.flush();
        BookSearchRequest request = new BookSearchRequest(KEYWORD, null, SearchType.HYBRID, axis(dim, 1f));

        List<Long> keywordPaged = bookRepository.search(PageRequest.of(0, 100), request).map(BookSearchResponse::getId).getContent();
        List<Long> vectorPaged = bookRepository.vectorSearch(PageRequest.of(0, 100), request).map(BookSearchResponse::getId).getContent();

        assertThat(bookRepository.searchCandidates(request, 100)).extracting(BookSearchResponse::getId)
                .containsExactlyElementsOf(keywordPaged);
        assertThat(bookRepository.vectorSearchCandidates(request, 100)).extracting(BookSearchResponse::getId)
                .containsExactlyElementsOf(vectorPaged);
    }

    @Test
    @DisplayName("벡터 후보 조회는 같은 트랜잭션 안에서 hnsw.ef_search를 설정값으로 올리고 오류 없이 실행된다")
    void vectorCandidates_setsLocalEfSearch() {
        bookRepository.vectorSearchCandidates(new BookSearchRequest(null, null, SearchType.VECTOR, axis(1003, 1f)), 100);

        Object efSearch = entityManager.createNativeQuery("SELECT current_setting('hnsw.ef_search')").getSingleResult();

        assertThat(efSearch).isEqualTo("120");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED) // 하이브리드는 별도 스레드에서 조회하므로 데이터를 커밋해야 함
    @DisplayName("같은 하이브리드 검색을 두 번 실행하면 순서와 점수가 같다 (동점 다수)")
    void hybridSearch_sameRequestTwice_sameResult() {
        List<Book> saved = new java.util.ArrayList<>();
        try {
            runHybridTwice(saved);
        } finally {
            bookRepository.deleteAll(saved);
        }
    }

    private void runHybridTwice(List<Book> saved) {
        int dim = 1001;
        when(embeddingService.getEmbedding(anyString())).thenReturn(axis(dim, 1f));
        LocalDate sameDate = LocalDate.of(2024, 5, 5);
        for (int i = 0; i < 30; i++) {
            // 같은 출간일·같은 임베딩 → 키워드·벡터 양쪽에서 동점이 다수 발생
            saved.add(save(String.format("99900000001%02d", i), KEYWORD + " 하이브리드 " + i, "저자", sameDate, axis(dim, 1f)));
        }
        bookRepository.flush();

        BookSearchRequest request = new BookSearchRequest(KEYWORD, null, SearchType.HYBRID, null);
        List<BookSearchResponse> first = bookSearchService.searchBooks(PageRequest.of(0, 24), request).getBooks().getContent();
        List<BookSearchResponse> second = bookSearchService.searchBooks(PageRequest.of(0, 24), request).getBooks().getContent();

        assertThat(first).hasSize(24);
        // 병렬·후보 조회로 바꾼 뒤에도 페이지 조회 결과를 RRF로 합친 것과 순서가 같아야 합니다.
        BookSearchRequest withVector = new BookSearchRequest(KEYWORD, null, SearchType.HYBRID, axis(dim, 1f));
        List<Long> expected = rrfService.fuse(
                        bookRepository.search(PageRequest.of(0, 100), withVector).getContent(),
                        bookRepository.vectorSearch(PageRequest.of(0, 100), withVector).getContent())
                .stream().limit(24).map(BookSearchResponse::getId).toList();
        assertThat(first).extracting(BookSearchResponse::getId).containsExactlyElementsOf(expected);
        assertThat(second).extracting(BookSearchResponse::getId)
                .containsExactlyElementsOf(first.stream().map(BookSearchResponse::getId).toList());
        assertThat(second).extracting(BookSearchResponse::getRrfScore)
                .containsExactlyElementsOf(first.stream().map(BookSearchResponse::getRrfScore).toList());
    }

    private static float[] mixed(int dim) {
        float[] v = new float[DIMENSION];
        v[dim] = 1f;
        v[dim + 10] = 1f; // 코사인 유사도 = 1/√2
        return v;
    }
}
