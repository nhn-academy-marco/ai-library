package com.nhnacademy.library.core.book.service;

import com.nhnacademy.library.core.book.dto.BookSearchRequest;
import com.nhnacademy.library.core.book.dto.BookSearchResponse;
import com.nhnacademy.library.core.book.dto.BookViewResponse;
import com.nhnacademy.library.core.book.exception.BookNotFoundException;
import com.nhnacademy.library.core.book.repository.BookRepository;
import lombok.Builder;
import lombok.Getter;
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
    private final BookAiService bookAiService;

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
    public SearchResult searchBooks(Pageable pageable, BookSearchRequest request) {
        log.info("Searching books with request: {}, pageable: {}", request, pageable);

        if (("vector".equals(request.searchType()) || "hybrid".equals(request.searchType()) || "rag".equals(request.searchType()))
                && request.keyword() != null && !request.keyword().isBlank()) {
            float[] vector = embeddingService.getEmbedding(request.keyword());
            request = new BookSearchRequest(request.keyword(), request.isbn(), request.searchType(), vector);
        }

        Page<BookSearchResponse> results;
        String aiResponse = null;

        if ("hybrid".equals(request.searchType()) && request.vector() != null) {
            results = hybridSearch(pageable, request);
        } else if ("rag".equals(request.searchType()) && request.vector() != null) {
            results = bookRepository.vectorSearch(pageable, request);
            aiResponse = generateAiResponse(request.keyword(), results.getContent());
        } else {
            results = bookRepository.search(pageable, request);
        }

        return SearchResult.builder()
                .books(results)
                .aiResponse(aiResponse)
                .build();
    }

    private String generateAiResponse(String question, List<BookSearchResponse> books) {
        if (books.isEmpty()) {
            return "검색 결과가 없어 답변을 생성할 수 없습니다.";
        }

        StringBuilder context = new StringBuilder();
        int index = 1;
        for (BookSearchResponse book : books) {
            context.append(String.format("%d. 제목: %s / 저자: %s\n", index++, book.getTitle(), book.getAuthorName()));
            if (book.getBookContent() != null && !book.getBookContent().isBlank()) {
                context.append(String.format("   내용: %s\n", book.getBookContent()));
            }
        }

        String prompt = String.format("""
            당신은 도서관의 전문 사서입니다. 
            아래 제공된 [참고 도서 리스트]를 바탕으로 사용자의 질문에 친절하게 답변해 주세요.
            리스트에 없는 책은 절대로 추천하지 마세요.
            정보가 부족하다면 정직하게 모른다고 답해 주세요.

            [참고 도서 리스트]
            %s
            
            사용자 질문: %s
            답변:
            """, context.toString(), question);

        return bookAiService.askAboutBooks(prompt);
    }

    @Getter
    @Builder
    public static class SearchResult {
        private final Page<BookSearchResponse> books;
        private final String aiResponse;
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
                        existing.getEditionPublishDate(), existing.getImageUrl(), existing.getBookContent(), b.getSimilarity()
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
                        original.getEditionPublishDate(), original.getImageUrl(), original.getBookContent(), original.getSimilarity(),
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
