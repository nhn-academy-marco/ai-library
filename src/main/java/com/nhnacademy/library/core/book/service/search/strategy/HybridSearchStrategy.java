package com.nhnacademy.library.core.book.service.search.strategy;

import com.nhnacademy.library.core.book.dto.BookSearchRequest;
import com.nhnacademy.library.core.book.dto.BookSearchResponse;
import com.nhnacademy.library.core.book.dto.BookSearchResult;
import com.nhnacademy.library.core.book.repository.BookRepository;
import com.nhnacademy.library.core.book.service.search.RrfService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 하이브리드(키워드 + 벡터) 검색 전략
 */
@Component
@RequiredArgsConstructor
public class HybridSearchStrategy implements SearchStrategy {

    private final BookRepository bookRepository;
    private final RrfService rrfService;

    private static final int DEFAULT_BATCH_SIZE = 100;

    @Override
    public BookSearchResult search(Pageable pageable, BookSearchRequest request) {
        // 1. 키워드 검색 결과 (Top 100)
        var keywordPage = bookRepository.search(PageRequest.of(0, DEFAULT_BATCH_SIZE), request);
        List<BookSearchResponse> keywordResults = (keywordPage != null && keywordPage.getContent() != null) 
                ? keywordPage.getContent() : List.of();

        // 2. 벡터 검색 결과 (Top 100)
        var vectorPage = bookRepository.vectorSearch(PageRequest.of(0, DEFAULT_BATCH_SIZE), request);
        List<BookSearchResponse> vectorResults = (vectorPage != null && vectorPage.getContent() != null) 
                ? vectorPage.getContent() : List.of();

        // 3. RRF (Reciprocal Rank Fusion) 알고리즘 적용
        List<BookSearchResponse> fusedResults = rrfService.fuse(keywordResults, vectorResults);

        // 4. 페이징 처리
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
