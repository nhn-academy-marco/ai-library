package com.nhnacademy.library.core.book.service;

import com.nhnacademy.library.core.book.dto.BookSearchRequest;
import com.nhnacademy.library.core.book.dto.BookSearchResponse;
import com.nhnacademy.library.core.book.dto.BookViewResponse;
import com.nhnacademy.library.core.book.exception.BookNotFoundException;
import com.nhnacademy.library.core.book.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 도서 검색 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookSearchService {

    private final BookRepository bookRepository;
    private final EmbeddingService embeddingService;

    private static final int DEFAULT_BATCH_SIZE = 100;
    private static final int RRF_K = 60;

    /**
     * 조건에 맞는 도서를 검색하여 페이징된 결과를 반환합니다.
     *
     * @param pageable 페이징 정보
     * @param request  검색 조건
     * @return 페이징된 도서 검색 결과
     */
    @Transactional(readOnly = true)
    public Page<BookSearchResponse> searchBooks(Pageable pageable, BookSearchRequest request) {
        log.info("Searching books with request: {}, pageable: {}", request, pageable);

        if (("vector".equals(request.searchType()) || "hybrid".equals(request.searchType()))
                && request.keyword() != null && !request.keyword().isBlank()) {
            float[] vector = embeddingService.getEmbedding(request.keyword());
            request = new BookSearchRequest(request.keyword(), request.isbn(), request.searchType(), vector);
        }

        if ("hybrid".equals(request.searchType()) && request.vector() != null) {
            return hybridSearch(pageable, request);
        }

        return bookRepository.search(pageable, request);
    }

    private Page<BookSearchResponse> hybridSearch(Pageable pageable, BookSearchRequest request) {
        // 1. 키워드 검색 결과 (Top 100)
        Page<BookSearchResponse> keywordPage = bookRepository.search(PageRequest.of(0, DEFAULT_BATCH_SIZE), request);
        List<BookSearchResponse> keywordResults = keywordPage.getContent();

        // 2. 벡터 검색 결과 (Top 100)
        Page<BookSearchResponse> vectorPage = bookRepository.vectorSearch(PageRequest.of(0, DEFAULT_BATCH_SIZE), request);
        List<BookSearchResponse> vectorResults = vectorPage.getContent();

        // 3. RRF (Reciprocal Rank Fusion) 알고리즘 적용
        Map<Long, Double> rrfScores = new HashMap<>();
        Map<Long, BookSearchResponse> bookMap = new HashMap<>();

        for (int i = 0; i < keywordResults.size(); i++) {
            BookSearchResponse b = keywordResults.get(i);
            rrfScores.put(b.getId(), rrfScores.getOrDefault(b.getId(), 0.0) + 1.0 / (RRF_K + i + 1));
            bookMap.put(b.getId(), b);
        }

        for (int i = 0; i < vectorResults.size(); i++) {
            BookSearchResponse b = vectorResults.get(i);
            rrfScores.put(b.getId(), rrfScores.getOrDefault(b.getId(), 0.0) + 1.0 / (RRF_K + i + 1));
            if (!bookMap.containsKey(b.getId())) {
                bookMap.put(b.getId(), b);
            } else {
                // 키워드 결과에 이미 있는 경우 유사도 정보 업데이트
                BookSearchResponse existing = bookMap.get(b.getId());
                bookMap.put(b.getId(), new BookSearchResponse(
                        existing.getId(), existing.getIsbn(), existing.getTitle(), existing.getVolumeTitle(),
                        existing.getAuthorName(), existing.getPublisherName(), existing.getPrice(),
                        existing.getEditionPublishDate(), existing.getImageUrl(), b.getSimilarity()
                ));
            }
        }

        // 4. 점수 기준 정렬 및 페이징 (RRF 점수 우선)
        List<Long> sortedIds = rrfScores.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .map(Map.Entry::getKey)
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), sortedIds.size());

        List<BookSearchResponse> content = new ArrayList<>();
        if (start < sortedIds.size()) {
            for (int i = start; i < end; i++) {
                Long id = sortedIds.get(i);
                BookSearchResponse original = bookMap.get(id);
                Double rrfScore = rrfScores.get(id);
                
                content.add(new BookSearchResponse(
                        original.getId(), original.getIsbn(), original.getTitle(), original.getVolumeTitle(),
                        original.getAuthorName(), original.getPublisherName(), original.getPrice(),
                        original.getEditionPublishDate(), original.getImageUrl(), original.getSimilarity(),
                        rrfScore
                ));
            }
        }

        return new PageImpl<>(content, pageable, sortedIds.size());
    }

    /**
     * 도서 상세 정보를 조회합니다.
     *
     * @param id 도서 ID
     * @return 도서 상세 정보
     * @throws BookNotFoundException 도서를 찾을 수 없는 경우
     */
    public BookViewResponse getBook(Long id) {
        log.info("Fetching book details for id: {}", id);
        return bookRepository.findById(id)
                .map(BookViewResponse::from)
                .orElseThrow(() -> new BookNotFoundException(id));
    }
}
