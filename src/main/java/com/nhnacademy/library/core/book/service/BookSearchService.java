package com.nhnacademy.library.core.book.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.library.core.book.dto.BookAiRecommendationResponse;
import com.nhnacademy.library.core.book.dto.BookSearchRequest;
import com.nhnacademy.library.core.book.dto.BookSearchResponse;
import com.nhnacademy.library.core.book.dto.BookViewResponse;
import com.nhnacademy.library.core.book.exception.BookNotFoundException;
import com.nhnacademy.library.core.book.repository.BookRepository;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.PromptTemplate;
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
    private final ObjectMapper objectMapper;

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
        List<BookAiRecommendationResponse> aiResponse = null;

        if ("hybrid".equals(request.searchType()) && request.vector() != null) {
            results = hybridSearch(pageable, request);
        } else if ("rag".equals(request.searchType()) && request.vector() != null) {
            results = hybridSearch(pageable, request);
            aiResponse = generateAiResponse(request.keyword(), results.getContent());
        } else {
            results = bookRepository.search(pageable, request);
        }

        return SearchResult.builder()
                .books(results)
                .aiResponse(aiResponse)
                .build();
    }

    private List<BookAiRecommendationResponse> generateAiResponse(String question, List<BookSearchResponse> books) {
        if (books.isEmpty()) {
            return List.of();
        }

        StringBuilder context = new StringBuilder();
        for (BookSearchResponse book : books) {
            context.append(String.format("ID: %d, 제목: %s, 저자: %s, 출판일: %s, 내용: %s\n",
                    book.getId(), book.getTitle(), book.getAuthorName(),
                    book.getEditionPublishDate() != null ? book.getEditionPublishDate().toString() : "알 수 없음",
                    book.getBookContent() != null ? book.getBookContent() : "내용 없음"));
        }

        String template = """
            [규칙]
            - 사용자가 제공하는 query와 가장 관련 있는 도서를 선별하세요.
            - 각 도서에 대해 relevance 점수(0~100)를 부여하세요:
              - 90–100: query와 직접적으로 강하게 연관, 주제 적합성이 매우 높음
              - 70–89: query와 밀접하게 관련 있지만 일부 범위가 제한적임
              - 50–69: query와 간접적으로 관련, 배경 지식에 도움이 됨
              - 50 미만: 관련성이 낮으므로 출력에서 제외
            - 추천 사유("why")에는 점수를 포함하지 말고, 순수하게 이유만 설명하세요.
            - 최신 출간일과 query와의 직접적인 관련성을 함께 고려하세요.
            
            [출력 형식]
            - 출력은 반드시 순수 JSON만 포함하세요.
            - 마크다운 코드 블록(```json ... ```)이나 추가 설명 텍스트는 절대 포함하지 마세요.
            - 언어는 반드시 한국어를 사용하세요.
            
            [JSON STRUCTURE]
             [
                {
                  "id": 123,
                  "relevance": 95,
                  "why": "추천 사유"
                }
             ]
             
            - 결과는 relevance 기준 내림차순으로 정렬하세요.
            - 입력 데이터에 없는 필드는 추측하지 마세요.
            
            query: {question}
            
            도서 데이터:
            {context}
            """;

        String renderedPrompt = template
                .replace("{question}", question)
                .replace("{context}", context.toString());

        String rawResponse = bookAiService.askAboutBooks(renderedPrompt);
        log.debug("AI Raw Response: {}", rawResponse);

        try {
            // JSON 응답에서 마크다운 코드 블록 제거 (혹시 포함될 경우를 대비)
            String jsonPart = rawResponse.replaceAll("```json", "").replaceAll("```", "").trim();
            List<BookAiRecommendationResponse> recommendations = objectMapper.readValue(jsonPart, new TypeReference<List<BookAiRecommendationResponse>>() {});
            
            // 검색된 도서 리스트에서 유사도 및 RRF 점수 매핑
            for (BookAiRecommendationResponse rec : recommendations) {
                books.stream()
                        .filter(b -> b.getId().equals(rec.getId()))
                        .findFirst()
                        .ifPresent(b -> {
                            rec.setSimilarity(b.getSimilarity());
                            rec.setRrfScore(b.getRrfScore());
                        });
            }
            return recommendations;
        } catch (Exception e) {
            log.error("Failed to parse AI response: {}", rawResponse, e);
            return List.of();
        }
    }

    @Getter
    @Builder
    public static class SearchResult {
        private final Page<BookSearchResponse> books;
        private final List<BookAiRecommendationResponse> aiResponse;
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
