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
 * 하이브리드 검색 전략 구현체입니다.
 * 키워드 검색(Keyword Search)과 벡터 검색(Vector Search) 결과를 결합하여 보다 정확한 검색 결과를 제공합니다.
 * 두 검색 결과의 순위를 통합하기 위해 RRF(Reciprocal Rank Fusion) 알고리즘을 사용합니다.
 */
@Component
@RequiredArgsConstructor
public class HybridSearchStrategy implements SearchStrategy {

    private final BookRepository bookRepository;
    private final RrfService rrfService;

    /** RRF 점수 계산을 위해 각 검색 엔진에서 가져올 상위 결과 수 */
    private static final int DEFAULT_BATCH_SIZE = 100;

    /**
     * 하이브리드 검색을 수행합니다.
     * 1. 키워드 검색: 형태소 분석 기반의 전통적인 텍스트 검색을 수행합니다.
     * 2. 벡터 검색: 임베딩 벡터 유사도 기반의 의미론적 검색을 수행합니다.
     * 3. 결과 병합: RRF 알고리즘을 통해 두 결과의 순위를 재조정하여 통합합니다.
     * 4. 페이징: 통합된 전체 결과 리스트에서 요청된 페이지 구간을 추출합니다.
     *
     * @param pageable 페이징 정보
     * @param request  검색 요청 DTO (키워드 등 포함)
     * @return 통합 순위가 적용된 도서 검색 결과
     */
    @Override
    public BookSearchResult search(Pageable pageable, BookSearchRequest request) {
        // 1. 키워드 검색 결과 (Top 100) 추출
        var keywordPage = bookRepository.search(PageRequest.of(0, DEFAULT_BATCH_SIZE), request);
        List<BookSearchResponse> keywordResults = (keywordPage != null && keywordPage.getContent() != null) 
                ? keywordPage.getContent() : List.of();

        // 2. 벡터 검색 결과 (Top 100) 추출
        var vectorPage = bookRepository.vectorSearch(PageRequest.of(0, DEFAULT_BATCH_SIZE), request);
        List<BookSearchResponse> vectorResults = (vectorPage != null && vectorPage.getContent() != null) 
                ? vectorPage.getContent() : List.of();

        // 3. RRF (Reciprocal Rank Fusion) 알고리즘을 적용하여 결과 병합 및 순위 재조정
        List<BookSearchResponse> fusedResults = rrfService.fuse(keywordResults, vectorResults);

        // 4. 병합된 결과 리스트에서 메모리 내 페이징 처리
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
