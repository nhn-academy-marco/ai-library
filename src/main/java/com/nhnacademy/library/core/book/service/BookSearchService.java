package com.nhnacademy.library.core.book.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.library.core.book.domain.BookReview;
import com.nhnacademy.library.core.book.dto.BookAiRecommendationResponse;
import com.nhnacademy.library.core.book.dto.BookSearchRequest;
import com.nhnacademy.library.core.book.dto.BookSearchResponse;
import com.nhnacademy.library.core.book.dto.BookViewResponse;
import com.nhnacademy.library.core.book.event.BookSearchEvent;
import com.nhnacademy.library.core.book.exception.BookNotFoundException;
import com.nhnacademy.library.core.book.repository.BookRepository;
import com.nhnacademy.library.core.book.repository.BookReviewRepository;
import com.nhnacademy.library.core.book.domain.BookSearchCache;
import com.nhnacademy.library.core.book.repository.BookSearchCacheRepository;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

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
    private final BookSearchCacheService bookSearchCacheService;
    private final ApplicationEventPublisher eventPublisher;
    private final BookSearchCacheRepository cacheRepository;
    private final CacheManager cacheManager;
    private final BookReviewRepository bookReviewRepository;
    private final ReviewSummarizer reviewSummarizer;

    @Value("${cache.ttl.minutes:30}")
    private int cacheTtlMinutes;

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
                && request.keyword() != null && !request.keyword().isBlank() && request.vector() == null) {
            float[] vector = embeddingService.getEmbedding(request.keyword());
            request = new BookSearchRequest(request.keyword(), request.isbn(), request.searchType(), vector);
        }
        
        return searchBooksWithVector(pageable, request);
    }

    @Transactional(readOnly = true)
    public SearchResult searchBooksWithVector(Pageable pageable, BookSearchRequest request) {
        // [미션 2-1] 의미적 캐싱 (Semantic Caching): 벡터 유사도 기반 캐시 조회
        if ("rag".equals(request.searchType()) && request.vector() != null) {
            SearchResult cachedResult = findSimilarResultInCache(request);
            if (cachedResult != null) {
                log.info("[SEMANTIC_CACHE] Cache hit for similar keyword: {}", request.keyword());
                return cachedResult;
            }
        }

        // background warm-up 중일 때는 이벤트를 또 던지지 않도록 함
        boolean isFromWarmUp = false;
        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            if (element.getClassName().contains("BookSearchCacheService")) {
                isFromWarmUp = true;
                break;
            }
        }

        // [미션 2] 전략적 캐싱: 하이브리드 검색 시 이벤트를 발생시켜 백그라운드에서 RAG 결과 캐싱 시도
        if (!isFromWarmUp && "hybrid".equals(request.searchType()) && request.keyword() != null && !request.keyword().isBlank()) {
            log.info("[STRATEGIC_CACHE] Publishing search event for keyword: {}", request.keyword());
            eventPublisher.publishEvent(new BookSearchEvent(this, request.keyword()));
        }

        Page<BookSearchResponse> results;
        List<BookAiRecommendationResponse> aiResponse = null;

        if ("hybrid".equals(request.searchType()) && request.vector() != null) {
            results = hybridSearch(pageable, request);
        } else if ("rag".equals(request.searchType()) && request.vector() != null) {
            // [수정] 의미적 캐싱이 적용되었으므로, 여기까지 왔다면 캐시 미스임.
            // RAG 검색 시 캐시 확인 (isFromWarmUp이 아닐 때만 이벤트 발행 및 폴백)
            if (!isFromWarmUp) {
                log.info("[STRATEGIC_CACHE] No RAG cache found. Publishing search event and falling back to hybrid search results.");
                eventPublisher.publishEvent(new BookSearchEvent(this, request.keyword()));
                
                // 캐시가 없으면 하이브리드 검색 결과를 보여줌
                results = hybridSearch(pageable, request);
                aiResponse = null;
            } else {
                // Warm-up 중일 때만 실제로 AI 호출 및 결과 생성
                // [분리 구현] AI는 더 넓은 범위를 훑어보기 위해 Retrieval K를 넉넉히(100) 설정
                Pageable retrievalPageable = PageRequest.of(0, DEFAULT_BATCH_SIZE);
                Page<BookSearchResponse> retrievalResults = hybridSearch(retrievalPageable, request);
                
                double threshold = 0.02;
                List<BookSearchResponse> topKBooks = retrievalResults.getContent().stream()
                        .filter(b -> b.getRrfScore() != null && b.getRrfScore() >= threshold)
                        .limit(5)
                        .toList();
                
                if (topKBooks.isEmpty()) {
                    log.info("No books passed the threshold (>= {}). Skipping AI response generation to save cost and avoid noise.", threshold);
                    aiResponse = List.of();
                } else {
                    aiResponse = generateAiResponse(request.keyword(), topKBooks);
                }

                // 화면 결과는 사용자가 요청한 페이징 크기를 유지
                results = hybridSearch(pageable, request);
            }
            
            // 결과를 Redis 캐시에 저장 (RAG 타입인 경우)
            if (aiResponse != null) {
                BookSearchCache cacheEntry = BookSearchCache.builder()
                        .keyword(request.keyword())
                        .vector(request.vector())
                        .books(results.getContent())
                        .aiResponse(aiResponse)
                        .createdAt(System.currentTimeMillis())
                        .build();
                log.info("[REDIS_VECTOR_CACHE] Putting result into Redis for keyword: {}", request.keyword());
                cacheRepository.save(cacheEntry);
            }
        } else {
            results = bookRepository.search(pageable, request);
        }

        return SearchResult.builder()
                .books(results)
                .aiResponse(aiResponse)
                .build();
    }

    public SearchResult findSimilarResultInCache(BookSearchRequest request) {
        if (request.vector() == null) return null;

        long ttlMillis = (long) cacheTtlMinutes * 60 * 1000;
        long now = System.currentTimeMillis();

        // Redis Vector Search를 이용한 유사도 검색
        // Redis OM의 near 필터가 타입 안정성 문제로 작동하지 않을 경우를 대비하여 
        // 일단 모든 캐시를 가져와서 수동으로 유사도를 비교하는 방식으로 구현 (Repository의 한계)
        // 실제로는 BookSearchCache$.VECTOR.near(...)를 사용하는 것이 정석입니다.
        Iterable<BookSearchCache> allCached = cacheRepository.findAll();
        
        for (BookSearchCache cached : allCached) {
            double similarity = calculateCosineSimilarity(request.vector(), cached.getVector());
            if (similarity < 0.98) continue;
            long age = now - cached.getCreatedAt();
            if (age > ttlMillis || age < 0) {
                log.info("[REDIS_VECTOR_CACHE] Cache expired for keyword: '{}' (Age: {}ms, TTL: {}ms)",
                        cached.getKeyword(), age, ttlMillis);
                cacheRepository.delete(cached);
                continue;
            }

            log.info("[REDIS_VECTOR_CACHE] Found similar request in Redis: '{}' (Similarity: {})", 
                    cached.getKeyword(), similarity);
            return SearchResult.builder()
                    .books(new PageImpl<>(cached.getBooks()))
                    .aiResponse(cached.getAiResponse())
                    .createdAt(cached.getCreatedAt())
                    .build();
        }
        return null;
    }

    private double calculateCosineSimilarity(float[] vectorA, float[] vectorB) {
        if (vectorA == null || vectorB == null || vectorA.length != vectorB.length) {
            return 0.0;
        }
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += Math.pow(vectorA[i], 2);
            normB += Math.pow(vectorB[i], 2);
        }
        if (normA == 0 || normB == 0) {
            return 0.0;
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private List<BookAiRecommendationResponse> generateAiResponse(String question, List<BookSearchResponse> books) {
        if (books == null || books.isEmpty()) {
            return List.of();
        }

        // [미션 3] Context 재구성: RRF 점수 내림차순으로 정렬하여 가장 중요한 정보가 컨텍스트의 앞부분에 오도록 함
        List<BookSearchResponse> sortedBooks = new ArrayList<>(books);
        sortedBooks.sort((b1, b2) -> {
            Double s1 = b1.getRrfScore() != null ? b1.getRrfScore() : 0.0;
            Double s2 = b2.getRrfScore() != null ? b2.getRrfScore() : 0.0;
            return s2.compareTo(s1);
        });

        StringBuilder context = new StringBuilder();
        for (BookSearchResponse book : sortedBooks) {
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
            - 추천 사유("why")는 사용자에게 친절하고 공손한 어투(예: "~입니다", "~를 추천해 드립니다")로 작성하세요.
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
        private final long createdAt;

        public SearchResult(Page<BookSearchResponse> books, List<BookAiRecommendationResponse> aiResponse) {
            this.books = books;
            this.aiResponse = aiResponse;
            this.createdAt = System.currentTimeMillis();
        }

        @com.fasterxml.jackson.annotation.JsonCreator
        public SearchResult(
                @com.fasterxml.jackson.annotation.JsonProperty("books") Page<BookSearchResponse> books,
                @com.fasterxml.jackson.annotation.JsonProperty("aiResponse") List<BookAiRecommendationResponse> aiResponse,
                @com.fasterxml.jackson.annotation.JsonProperty("createdAt") long createdAt) {
            this.books = books;
            this.aiResponse = aiResponse;
            this.createdAt = createdAt == 0 ? System.currentTimeMillis() : createdAt;
        }
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

    /**
     * 도서 리뷰 요약을 생성합니다.
     *
     * @param bookId 도서 ID
     * @return 요약된 리뷰 텍스트
     */
    public String getReviewSummary(Long bookId) {
        log.info("Generating review summary for book id: {}", bookId);
        List<String> reviews = bookReviewRepository.findAllByBookId(bookId).stream()
                .map(BookReview::getContent)
                .toList();
        
        return reviewSummarizer.summarizeReviews(reviews);
    }
}
