package net.java21.library.core.book.service.search.strategy;

import net.java21.library.core.book.domain.SearchType;
import net.java21.library.core.book.dto.BookSearchRequest;
import net.java21.library.core.book.dto.BookSearchResponse;
import net.java21.library.core.book.exception.SearchTimeoutException;
import net.java21.library.core.book.repository.BookRepository;
import net.java21.library.core.book.service.search.RrfService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("HybridSearchStrategy 병렬 후보 조회 테스트")
class HybridSearchStrategyTest {

    private static final long TIMEOUT_MILLIS = 2_000;
    private static final BookSearchRequest REQUEST =
            new BookSearchRequest("spring", null, SearchType.HYBRID, new float[]{0.1f});

    private final BookRepository bookRepository = mock(BookRepository.class);
    private final RrfService rrfService = new RrfService();
    private final AtomicInteger transactions = new AtomicInteger();
    /** 각 조회가 트랜잭션 안에서 실행되는지 세기 위한 대역 (실제 트랜잭션 없이 콜백만 실행) */
    private final TransactionOperations countingTransaction = new TransactionOperations() {
        @Override
        public <T> T execute(TransactionCallback<T> action) {
            transactions.incrementAndGet();
            return action.doInTransaction(null);
        }
    };

    private ExecutorService executor;
    private HybridSearchStrategy strategy;

    @BeforeEach
    void setUp() {
        executor = Executors.newFixedThreadPool(2);
        strategy = new HybridSearchStrategy(bookRepository, rrfService, executor, countingTransaction, TIMEOUT_MILLIS);
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    private static BookSearchResponse book(long id, Double similarity) {
        return new BookSearchResponse(id, "isbn" + id, "title" + id, null, "author", "publisher",
                null, null, null, "content", similarity);
    }

    @Test
    @DisplayName("키워드·벡터 후보를 동시에 조회하고 기존과 같은 RRF 순서로 병합한다")
    void runsBothQueriesConcurrentlyAndFusesInRrfOrder() {
        List<BookSearchResponse> keyword = List.of(book(1, null), book(2, null), book(3, null));
        List<BookSearchResponse> vector = List.of(book(3, 0.9), book(4, 0.8), book(1, 0.7));
        // 두 조회가 서로를 기다리므로 순차 실행이면 제한 시간 안에 끝나지 않습니다.
        CountDownLatch bothStarted = new CountDownLatch(2);
        when(bookRepository.searchCandidates(eq(REQUEST), anyInt())).thenAnswer(inv -> {
            bothStarted.countDown();
            assertThat(bothStarted.await(1, TimeUnit.SECONDS)).isTrue();
            return keyword;
        });
        when(bookRepository.vectorSearchCandidates(eq(REQUEST), anyInt())).thenAnswer(inv -> {
            bothStarted.countDown();
            assertThat(bothStarted.await(1, TimeUnit.SECONDS)).isTrue();
            return vector;
        });

        List<BookSearchResponse> result = strategy.search(PageRequest.of(0, 10), REQUEST).getBooks().getContent();

        List<Long> expected = rrfService.fuse(keyword, vector).stream().map(BookSearchResponse::getId).toList();
        assertThat(result).extracting(BookSearchResponse::getId).containsExactlyElementsOf(expected);
        assertThat(transactions).hasValue(2);
    }

    @Test
    @DisplayName("후보 조회에는 전체 건수(count)를 세는 페이지 조회를 쓰지 않는다")
    void doesNotUsePagedQueriesWithCount() {
        when(bookRepository.searchCandidates(any(), anyInt())).thenReturn(List.of(book(1, null)));
        when(bookRepository.vectorSearchCandidates(any(), anyInt())).thenReturn(List.of(book(1, 0.9)));

        strategy.search(PageRequest.of(0, 10), REQUEST);

        verify(bookRepository).searchCandidates(REQUEST, 100);
        verify(bookRepository).vectorSearchCandidates(REQUEST, 100);
        verify(bookRepository, never()).search(any(), any());
        verify(bookRepository, never()).vectorSearch(any(), any());
    }

    @Test
    @DisplayName("한쪽 조회가 실패하면 그 예외를 그대로 전파한다")
    void propagatesExceptionFromOneSide() {
        IllegalStateException failure = new IllegalStateException("vector query failed");
        when(bookRepository.searchCandidates(any(), anyInt())).thenReturn(List.of(book(1, null)));
        when(bookRepository.vectorSearchCandidates(any(), anyInt())).thenThrow(failure);

        assertThatThrownBy(() -> strategy.search(PageRequest.of(0, 10), REQUEST)).isSameAs(failure);
    }

    @Test
    @DisplayName("제한 시간 안에 끝나지 않으면 SearchTimeoutException을 던진다")
    void throwsTimeoutWhenQueryIsTooSlow() {
        HybridSearchStrategy shortTimeout =
                new HybridSearchStrategy(bookRepository, rrfService, executor, countingTransaction, 100);
        when(bookRepository.searchCandidates(any(), anyInt())).thenReturn(List.of());
        when(bookRepository.vectorSearchCandidates(any(), anyInt())).thenAnswer(inv -> {
            Thread.sleep(1_000);
            return List.of();
        });

        assertThatThrownBy(() -> shortTimeout.search(PageRequest.of(0, 10), REQUEST))
                .isInstanceOf(SearchTimeoutException.class);
    }
}
