package net.java21.library.core.book.service.cache;

import net.java21.library.core.book.domain.SearchType;
import net.java21.library.core.book.dto.BookSearchRequest;
import net.java21.library.core.book.service.embedding.EmbeddingService;
import net.java21.library.core.book.service.search.BookSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RAG 시맨틱 캐시 웜업 서비스
 *
 * <p>RAG 검색에서 캐시 미스가 나면 화면은 하이브리드 목록을 먼저 보여 주고, 이 서비스가 백그라운드에서
 * LLM 추천을 만들어 시맨틱 캐시에 넣습니다. 같은 검색어의 웜업이 동시에 여러 번 돌지 않게 막습니다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookSearchCacheService {

    private final ApplicationContext applicationContext;
    private final SemanticCacheService semanticCacheService;
    private final Set<String> inFlightWarmUps = ConcurrentHashMap.newKeySet();

    @Async
    public void warmUpRagCache(String keyword) {
        if (keyword == null || keyword.isBlank()) return;
        if (!inFlightWarmUps.add(keyword)) {
            log.info("[STRATEGIC_CACHE] Warm-up already in progress for keyword: {}", keyword);
            return;
        }

        log.info("[STRATEGIC_CACHE] Warming up RAG cache for keyword: {}", keyword);
        
        try {
            // BookSearchService를 ApplicationContext에서 지연 조회하여 순환 참조 해결
            BookSearchService bookSearchService = applicationContext.getBean(BookSearchService.class);
            EmbeddingService embeddingService = applicationContext.getBean(EmbeddingService.class);
            
            // 1. 임베딩 생성 (유사도 비교를 위해)
            float[] vector = embeddingService.getEmbedding(keyword);
            BookSearchRequest ragRequest = new BookSearchRequest(keyword, null, SearchType.RAG, vector, true);
            Pageable pageable = PageRequest.of(0, 24);


            // 2. 이미 캐시되어 있는지 확인 (의미적 캐싱 적용)
            if (semanticCacheService.findRecommendations(ragRequest).isPresent()) {
                log.info("[STRATEGIC_CACHE] Similar cache already exists for keyword: {}", keyword);
                return;
            }
            
            // 3. RAG 검색 수행 (LLM 호출 후 AI 추천이 시맨틱 캐시에 저장됨)
            bookSearchService.searchBooks(pageable, ragRequest);
            
            log.info("[STRATEGIC_CACHE] Successfully warmed up RAG cache for keyword: {}", keyword);
        } catch (Exception e) {
            // 캐시에 저장하지 않으므로 다음 검색 요청이 웜업을 다시 시도합니다(화면은 폴링 제한 시간 뒤 실패 안내).
            log.error("[STRATEGIC_CACHE] Failed to warm up RAG cache for keyword: {}", keyword, e);
        } finally {
            // 성공·실패와 관계없이 중복 방지 표시를 지워 다음 요청에서 다시 시도할 수 있게 합니다.
            inFlightWarmUps.remove(keyword);
        }
    }
}
