package net.java21.library.core.book.service.search.strategy;

import net.java21.library.core.book.dto.BookSearchRequest;
import net.java21.library.core.book.dto.BookSearchResponse;
import net.java21.library.core.book.dto.BookSearchResult;
import net.java21.library.core.book.exception.SearchTimeoutException;
import net.java21.library.core.book.repository.BookRepository;
import net.java21.library.core.book.service.search.RrfService;
import net.java21.library.core.book.service.search.SearchTimings;
import net.java21.library.core.config.AsyncConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * 하이브리드 검색 전략 구현체입니다.
 *
 * <p>키워드 후보와 벡터 후보(각 상위 {@value #CANDIDATE_SIZE}건)를 전용 Executor에서 동시에 조회한 뒤
 * RRF(Reciprocal Rank Fusion)로 순위를 합칩니다. RAG 전략도 후보 추출에 이 전략을 사용합니다.</p>
 *
 * <p>흐름:</p>
 * <ol>
 *   <li>키워드·벡터 후보 조회를 각각 별도 스레드에서 실행합니다. 스레드마다 자체 읽기 전용 트랜잭션(EntityManager)을 열어
 *       벡터 조회의 {@code SET LOCAL hnsw.ef_search}가 같은 트랜잭션에서 적용되게 합니다.</li>
 *   <li>후보 조회는 전체 건수(count)를 세지 않습니다. RRF에는 상위 후보만 필요하기 때문입니다.</li>
 *   <li>한쪽이 실패하면 기다리지 않고 그 예외를 그대로 전파하고, 제한 시간을 넘기면 {@link SearchTimeoutException}을 던집니다.</li>
 *   <li>두 결과를 RRF로 병합하고 메모리에서 페이지를 자릅니다.</li>
 * </ol>
 */
@Slf4j
@Component
public class HybridSearchStrategy implements SearchStrategy {

    /** RRF 점수 계산을 위해 각 검색에서 가져올 상위 후보 수 */
    private static final int CANDIDATE_SIZE = 100;

    private final BookRepository bookRepository;
    private final RrfService rrfService;
    private final Executor executor;
    private final TransactionOperations readOnlyTransaction;
    private final long timeoutMillis;

    @Autowired
    public HybridSearchStrategy(BookRepository bookRepository,
                                RrfService rrfService,
                                @Qualifier(AsyncConfig.HYBRID_SEARCH_EXECUTOR) Executor executor,
                                PlatformTransactionManager transactionManager,
                                @Value("${search.hybrid.timeout-ms:10000}") long timeoutMillis) {
        this(bookRepository, rrfService, executor, readOnlyTemplate(transactionManager), timeoutMillis);
    }

    HybridSearchStrategy(BookRepository bookRepository,
                         RrfService rrfService,
                         Executor executor,
                         TransactionOperations readOnlyTransaction,
                         long timeoutMillis) {
        this.bookRepository = bookRepository;
        this.rrfService = rrfService;
        this.executor = executor;
        this.readOnlyTransaction = readOnlyTransaction;
        this.timeoutMillis = timeoutMillis;
    }

    private static TransactionTemplate readOnlyTemplate(PlatformTransactionManager transactionManager) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setReadOnly(true);
        return template;
    }

    /**
     * 하이브리드 검색을 수행합니다.
     *
     * @param pageable 페이징 정보
     * @param request  검색 요청 DTO (키워드와 질의 벡터 포함)
     * @return 통합 순위가 적용된 도서 검색 결과
     */
    @Override
    public BookSearchResult search(Pageable pageable, BookSearchRequest request) {
        long startNanos = System.nanoTime();

        CompletableFuture<Timed> keywordFuture = submit(() -> bookRepository.searchCandidates(request, CANDIDATE_SIZE));
        CompletableFuture<Timed> vectorFuture = submit(() -> bookRepository.vectorSearchCandidates(request, CANDIDATE_SIZE));
        awaitBoth(keywordFuture, vectorFuture, request.keyword());

        Timed keyword = keywordFuture.join();
        Timed vector = vectorFuture.join();

        long rrfStart = System.nanoTime();
        List<BookSearchResponse> fusedResults = rrfService.fuse(keyword.results(), vector.results());
        long rrfMillis = elapsedMillis(rrfStart);

        log.info("[HYBRID_SEARCH] keyword='{}' embedding={}ms keyword={}ms vector={}ms rrf={}ms total={}ms candidates={}/{} fused={}",
                request.keyword(), formatEmbedding(SearchTimings.embeddingMillis()),
                keyword.millis(), vector.millis(), rrfMillis, elapsedMillis(startNanos),
                keyword.results().size(), vector.results().size(), fusedResults.size());

        // 메모리 내 페이징 처리
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), fusedResults.size());
        List<BookSearchResponse> content = start < fusedResults.size()
                ? fusedResults.subList(start, end)
                : new ArrayList<>();

        return BookSearchResult.builder()
                .books(new PageImpl<>(content, pageable, fusedResults.size()))
                .build();
    }

    /**
     * 후보 조회를 전용 Executor에서 읽기 전용 트랜잭션으로 실행하고 소요 시간을 함께 돌려줍니다.
     */
    private CompletableFuture<Timed> submit(Supplier<List<BookSearchResponse>> query) {
        return CompletableFuture.supplyAsync(() -> {
            long begin = System.nanoTime();
            List<BookSearchResponse> results = readOnlyTransaction.execute(status -> query.get());
            return new Timed(results == null ? List.of() : results, elapsedMillis(begin));
        }, executor);
    }

    /**
     * 두 조회가 모두 끝날 때까지 기다립니다. 한쪽이 실패하면 다른 쪽을 기다리지 않고 그 예외를 던집니다.
     */
    private void awaitBoth(CompletableFuture<Timed> first, CompletableFuture<Timed> second, String keyword) {
        CompletableFuture<Void> both = CompletableFuture.allOf(first, second);
        first.whenComplete((r, ex) -> failFast(both, ex));
        second.whenComplete((r, ex) -> failFast(both, ex));
        try {
            both.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            first.cancel(true);
            second.cancel(true);
            throw new SearchTimeoutException(keyword, timeoutMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Hybrid search interrupted", e);
        } catch (ExecutionException e) {
            throw propagate(e.getCause());
        }
    }

    private static void failFast(CompletableFuture<Void> both, Throwable ex) {
        if (ex != null) {
            both.completeExceptionally(ex);
        }
    }

    private static RuntimeException propagate(Throwable cause) {
        Throwable actual = cause;
        while (actual instanceof CompletionException && actual.getCause() != null) {
            actual = actual.getCause();
        }
        if (actual instanceof RuntimeException runtime) {
            return runtime;
        }
        if (actual instanceof Error error) {
            throw error;
        }
        return new IllegalStateException("Hybrid search failed", actual);
    }

    private static long elapsedMillis(long startNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
    }

    private static String formatEmbedding(long millis) {
        return millis == SearchTimings.NOT_MEASURED ? "-" : String.valueOf(millis);
    }

    /** 후보 조회 결과와 소요 시간(ms) */
    private record Timed(List<BookSearchResponse> results, long millis) {
    }
}
