package com.nhnacademy.library.core.book.service.search.strategy;

import com.nhnacademy.library.core.book.dto.BookSearchRequest;
import com.nhnacademy.library.core.book.dto.BookSearchResponse;
import com.nhnacademy.library.core.book.dto.BookSearchResult;
import com.nhnacademy.library.core.book.repository.BookRepository;
import com.nhnacademy.library.core.book.service.search.RrfService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 하이브리드 검색 전략 구현체입니다.
 * 키워드 검색(Keyword Search)과 벡터 검색(Vector Search) 결과를 병렬로 실행하여
 * 보다 빠른 검색 속도를 제공합니다.
 *
 * 두 검색 결과의 순위를 통합하기 위해 RRF(Reciprocal Rank Fusion) 알고리즘을 사용합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HybridSearchStrategy implements SearchStrategy {

    private final BookRepository bookRepository;
    private final RrfService rrfService;
    private final Executor taskExecutor;

    /** RRF 점수 계산을 위해 각 검색 엔진에서 가져올 상위 결과 수 */
    private static final int DEFAULT_BATCH_SIZE = 100;

    /**
     * 하이브리드 검색을 수행합니다.
     * 1. 키워드 검색과 벡터 검색을 병렬로 실행하여 응답 시간 단축
     * 2. 두 검색이 완료되면 RRF 알고리즘으로 결과 병합
     * 3. 병합된 결과에서 페이징 처리
     *
     * @param pageable 페이징 정보
     * @param request  검색 요청 DTO (키워드 등 포함)
     * @return 통합 순위가 적용된 도서 검색 결과
     */
    @Override
    public BookSearchResult search(Pageable pageable, BookSearchRequest request) {
        long startTime = System.currentTimeMillis();

        // 키워드 검색과 벡터 검색을 병렬로 실행
        CompletableFuture<List<BookSearchResponse>> keywordSearchFuture = CompletableFuture.supplyAsync(() -> {
            var keywordPage = bookRepository.search(PageRequest.of(0, DEFAULT_BATCH_SIZE), request);
            return (keywordPage != null && keywordPage.getContent() != null)
                    ? keywordPage.getContent()
                    : List.of();
        }, taskExecutor);

        CompletableFuture<List<BookSearchResponse>> vectorSearchFuture = CompletableFuture.supplyAsync(() -> {
            var vectorPage = bookRepository.vectorSearch(PageRequest.of(0, DEFAULT_BATCH_SIZE), request);
            return (vectorPage != null && vectorPage.getContent() != null)
                    ? vectorPage.getContent()
                    : List.of();
        }, taskExecutor);

        // 두 검색이 완료되면 RRF로 결과 병합
        CompletableFuture<List<BookSearchResponse>> fusedResultsFuture = keywordSearchFuture.thenCombine(
                vectorSearchFuture,
                (keywordResults, vectorResults) -> {
                    long fusionStartTime = System.currentTimeMillis();
                    List<BookSearchResponse> fused = rrfService.fuse(keywordResults, vectorResults);
                    log.debug("RRF fusion completed in {}ms", System.currentTimeMillis() - fusionStartTime);
                    return fused;
                },
                taskExecutor
        );

        // 병합된 결과를 가져와서 페이징 처리
        List<BookSearchResponse> fusedResults = fusedResultsFuture.join();
        long duration = System.currentTimeMillis() - startTime;
        log.info("Hybrid search completed in {}ms (keyword: {}, vector: {})",
                duration,
                keywordSearchFuture.isDone() ? "done" : "pending",
                vectorSearchFuture.isDone() ? "done" : "pending");

        // 메모리 내 페이징 처리
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), fusedResults.size());

        List<BookSearchResponse> content = new ArrayList<>();
        if (start < fusedResults.size()) {
            content = fusedResults.subList(start, end);
        }

        return BookSearchResult.builder()
                .books(new PageImpl<>(content, pageable, fusedResults.size()))
                .build();
    }
}
