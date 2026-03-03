package com.nhnacademy.library.ai.agent;

import com.nhnacademy.library.ai.function.dto.BookSearchRequest;
import com.nhnacademy.library.ai.function.dto.BookSearchResult;
import com.nhnacademy.library.core.book.dto.BookSearchResponse;
import com.nhnacademy.library.core.book.service.search.BookSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * 도서 검색 Agent
 * <p>
 * 도서 검색 전담 Agent입니다.
 * 사용자의 요청을 분석하여 적절한 검색어를 추출하고 검색을 수행합니다.
 * </p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BookAgent implements Agent {

    private final ChatModel chatModel;
    private final BookSearchService bookSearchService;

    private static final int DEFAULT_PAGE_SIZE = 5;  // 검색 결과를 5개로 제한

    @Override
    public AgentType getType() {
        return AgentType.BOOK;
    }

    @Override
    public String getName() {
        return "BookAgent";
    }

    @Override
    public String getDescription() {
        return "도서 검색 전담 Agent. 책 제목, 저자명, 키워드로 도서를 검색합니다.";
    }

    @Override
    public String execute(AgentRequest request) {
        log.info("[{}] 도서 검색 실행: query={}", getName(), request.query());

        try {
            // 1. 검색어 추출 (이미 MainAgent에서 추출했다고 가정)
            String searchQuery = request.query();

            // 2. 하이브리드 검색 실행
            var coreRequest = new com.nhnacademy.library.core.book.dto.BookSearchRequest(
                searchQuery,
                null,
                com.nhnacademy.library.core.book.domain.SearchType.HYBRID,
                null,
                false
            );

            var result = bookSearchService.searchBooks(
                PageRequest.of(0, DEFAULT_PAGE_SIZE),
                coreRequest
            );

            var books = result.getBooks().getContent();

            if (books.isEmpty()) {
                log.info("[{}] 검색 결과 없음: query={}", getName(), searchQuery);
                return "검색 결과가 없습니다.";
            }

            // 3. 결과 포맷팅
            StringBuilder response = new StringBuilder();
            response.append(String.format("'%s' 검색 결과 %d권을 찾았습니다.\n\n", searchQuery, books.size()));

            for (int i = 0; i < books.size(); i++) {
                BookSearchResponse book = books.get(i);
                response.append(String.format("%d. %s\n", i + 1, book.getTitle()));
                response.append(String.format("   저자: %s\n", book.getAuthorName()));
                response.append(String.format("   출판사: %s\n", book.getPublisherName()));
                if (book.getIsbn() != null) {
                    response.append(String.format("   ISBN: %s\n", book.getIsbn()));
                }
                response.append("\n");
            }

            log.info("[{}] 도서 검색 완료: {}권 찾음", getName(), books.size());
            return response.toString();

        } catch (Exception e) {
            log.error("[{}] 도서 검색 실패: query={}", getName(), request.query(), e);
            return "도서 검색 중 오류가 발생했습니다.";
        }
    }

    /**
     * ChatClient를 사용한 지능형 검색어 추출
     * (향후 확장용)
     */
    protected String extractSearchQuery(String userMessage) {
        // TODO: LLM을 사용하여 검색어 추출
        return userMessage;
    }
}
