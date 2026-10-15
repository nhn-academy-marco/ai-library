package net.java21.library.front.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.java21.library.core.book.service.search.BookSearchService;
import net.java21.library.front.api.dto.AiRecommendationRequest;
import net.java21.library.front.api.dto.AiRecommendationStatusResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * RAG 검색 화면이 AI 추천 준비 여부를 확인하는 API
 *
 * <p>RAG 검색에서 시맨틱 캐시 미스가 나면 화면은 하이브리드 목록을 먼저 보여 주고, 백그라운드 웜업이 끝날 때까지
 * 이 API를 짧은 간격으로 폴링합니다. 이 API는 캐시를 조회만 하며 LLM을 동기 호출하지 않습니다.</p>
 *
 * <ul>
 *   <li>{@code GET /api/recommendations?keyword=...}</li>
 *   <li>200 {"status":"pending"}: 아직 생성 중</li>
 *   <li>200 {"status":"ready","recommendations":[...]}: 준비됨 (빈 목록이면 추천할 도서 없음)</li>
 *   <li>200 {"status":"unavailable"}: 임베딩 서버 장애 등으로 조회할 수 없음 (화면은 폴링을 멈춤)</li>
 *   <li>400: 검색어가 비었거나 100자를 넘음</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class AiRecommendationApiController {

    private final BookSearchService bookSearchService;

    @GetMapping
    public ResponseEntity<AiRecommendationStatusResponse> status(@Valid @ModelAttribute AiRecommendationRequest request,
                                                                 BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            return ResponseEntity.ok(bookSearchService.findCachedRecommendations(request.keyword())
                    .map(AiRecommendationStatusResponse::ready)
                    .orElseGet(AiRecommendationStatusResponse::pending));
        } catch (RuntimeException e) {
            // 임베딩 서버 장애: 검색 화면은 키워드 검색으로 대체되므로 추천도 기다리지 않게 합니다.
            log.warn("[AI_RECOMMENDATION_API] Cannot look up recommendations for '{}': {}", request.keyword(), e.getMessage());
            return ResponseEntity.ok(AiRecommendationStatusResponse.unavailable());
        }
    }
}
