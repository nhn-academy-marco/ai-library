package com.nhnacademy.library.ai.agent;

import com.nhnacademy.library.ai.function.dto.BookSearchRequest;
import com.nhnacademy.library.ai.function.dto.BookSearchResult;
import com.nhnacademy.library.core.book.dto.BookSearchResponse;
import com.nhnacademy.library.core.book.service.search.BookSearchService;
import com.nhnacademy.library.external.opennaru.client.LibraryInfoNaruApiClient;
import com.nhnacademy.library.external.opennaru.dto.LibraryBookInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

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

    private final BookSearchService bookSearchService;
    private final LibraryInfoNaruApiClient libraryInfoNaruApiClient;

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
            String searchQuery = request.query();

            // 1. 도서관정보나루 API 검색 (실제 도서관 소장 도서)
            List<BookInfo> libraryBooks = searchLibraryNaru(searchQuery);
            log.info("[{}] 도서관정보나루 검색 결과: {}권", getName(), libraryBooks.size());

            // 2. 내부 DB 검색
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

            List<BookInfo> dbBooks = result.getBooks().getContent().stream()
                .map(book -> new BookInfo(
                    book.getTitle(),
                    book.getAuthorName(),
                    book.getPublisherName(),
                    book.getIsbn(),
                    "내부 DB"
                ))
                .collect(Collectors.toList());

            log.info("[{}] 내부 DB 검색 결과: {}권", getName(), dbBooks.size());

            // 3. 결과 합치기 (도서관정보나루 우선, ISBN 중복 제거)
            List<BookInfo> mergedBooks = mergeResults(libraryBooks, dbBooks);
            log.info("[{}] 합친 결과: {}권", getName(), mergedBooks.size());

            // 4. 상위 5개만 반환
            List<BookInfo> finalBooks = mergedBooks.stream()
                .limit(DEFAULT_PAGE_SIZE)
                .collect(Collectors.toList());

            if (finalBooks.isEmpty()) {
                log.info("[{}] 검색 결과 없음: query={}", getName(), searchQuery);
                return "검색 결과가 없습니다.";
            }

            // 5. 결과 포맷팅
            StringBuilder response = new StringBuilder();
            response.append(String.format("'%s' 검색 결과 %d권을 찾았습니다.\n\n", searchQuery, finalBooks.size()));

            for (int i = 0; i < finalBooks.size(); i++) {
                BookInfo book = finalBooks.get(i);
                response.append(String.format("%d. %s", i + 1, book.title));
                if (book.source != null && !book.source.equals("내부 DB")) {
                    response.append(String.format(" [%s]", book.source));
                }
                response.append("\n");
                response.append(String.format("   저자: %s\n", book.author));
                response.append(String.format("   출판사: %s\n", book.publisher));
                if (book.isbn != null) {
                    response.append(String.format("   ISBN: %s\n", book.isbn));
                }
                response.append("\n");
            }

            log.info("[{}] 도서 검색 완료: {}권 찾음", getName(), finalBooks.size());
            return response.toString();

        } catch (Exception e) {
            log.error("[{}] 도서 검색 실패: query={}", getName(), request.query(), e);
            return "도서 검색 중 오류가 발생했습니다.";
        }
    }

    /**
     * 도서관정보나루 API 검색
     */
    private List<BookInfo> searchLibraryNaru(String searchQuery) {
        try {
            List<LibraryBookInfo> libraryBooks = libraryInfoNaruApiClient.searchBooksByTitle(searchQuery);
            return libraryBooks.stream()
                .map(libBook -> new BookInfo(
                    libBook.title(),
                    libBook.authors(),
                    libBook.publisher(),
                    libBook.isbn13(),
                    "도서관 소장"
                ))
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("[{}] 도서관정보나루 검색 실패: query={}", getName(), searchQuery, e);
            return Collections.emptyList();
        }
    }

    /**
     * 결과 합치기 (ISBN 중복 제거, 도서관정보나루 우선)
     */
    private List<BookInfo> mergeResults(List<BookInfo> libraryBooks, List<BookInfo> dbBooks) {
        Set<String> seenIsbns = new HashSet<>();
        List<BookInfo> merged = new ArrayList<>();

        // 1. 도서관정보나루 결과 추가 (우선)
        for (BookInfo book : libraryBooks) {
            if (book.isbn != null && !book.isbn.isBlank()) {
                if (!seenIsbns.contains(book.isbn)) {
                    merged.add(book);
                    seenIsbns.add(book.isbn);
                }
            } else {
                merged.add(book);
            }
        }

        // 2. 내부 DB 결과 추가 (중복 ISBN 제외)
        for (BookInfo book : dbBooks) {
            if (book.isbn != null && !book.isbn.isBlank()) {
                if (!seenIsbns.contains(book.isbn)) {
                    merged.add(book);
                    seenIsbns.add(book.isbn);
                }
            } else if (merged.size() < DEFAULT_PAGE_SIZE) {
                // ISBN이 없고 결과가 부족하면 추가
                merged.add(book);
            }
        }

        return merged;
    }

    /**
     * 도서 정보 내부 클래스
     */
    private record BookInfo(
        String title,
        String author,
        String publisher,
        String isbn,
        String source
    ) {}

    /**
     * ChatClient를 사용한 지능형 검색어 추출
     * (향후 확장용)
     */
    protected String extractSearchQuery(String userMessage) {
        // TODO: LLM을 사용하여 검색어 추출
        return userMessage;
    }
}
