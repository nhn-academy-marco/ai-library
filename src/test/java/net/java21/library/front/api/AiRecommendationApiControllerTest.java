package net.java21.library.front.api;

import net.java21.library.core.book.dto.BookAiRecommendationResponse;
import net.java21.library.core.book.service.search.BookSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("GET /api/recommendations: AI 추천 폴링")
class AiRecommendationApiControllerTest {

    private BookSearchService bookSearchService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        bookSearchService = mock(BookSearchService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AiRecommendationApiController(bookSearchService)).build();
    }

    @Test
    @DisplayName("캐시에 추천이 없으면 pending을 돌려준다")
    void pending() throws Exception {
        when(bookSearchService.findCachedRecommendations("자바")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/recommendations").param("keyword", "자바"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("pending"))
                .andExpect(jsonPath("$.recommendations").isEmpty());
    }

    @Test
    @DisplayName("캐시에 추천이 있으면 ready와 도서 정보가 포함된 추천을 돌려준다")
    void ready() throws Exception {
        BookAiRecommendationResponse rec = new BookAiRecommendationResponse();
        rec.setId(1L);
        rec.setRelevance(90);
        rec.setWhy("입문서로 적합합니다.");
        rec.setTitle("자바의 정석");
        when(bookSearchService.findCachedRecommendations("자바")).thenReturn(Optional.of(List.of(rec)));

        mockMvc.perform(get("/api/recommendations").param("keyword", "자바"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ready"))
                .andExpect(jsonPath("$.recommendations[0].id").value(1))
                .andExpect(jsonPath("$.recommendations[0].title").value("자바의 정석"))
                .andExpect(jsonPath("$.recommendations[0].why").value("입문서로 적합합니다."));
    }

    @Test
    @DisplayName("임베딩 서버 장애로 조회할 수 없으면 unavailable을 돌려준다")
    void unavailable() throws Exception {
        when(bookSearchService.findCachedRecommendations("자바")).thenThrow(new IllegalStateException("down"));

        mockMvc.perform(get("/api/recommendations").param("keyword", "자바"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("unavailable"));
    }

    @Test
    @DisplayName("검색어가 비어 있으면 400을 돌려주고 조회하지 않는다")
    void blankKeyword_badRequest() throws Exception {
        mockMvc.perform(get("/api/recommendations").param("keyword", " "))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(bookSearchService);
    }
}
