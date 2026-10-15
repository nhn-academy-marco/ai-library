package net.java21.library.core.book.service.search.strategy;

import net.java21.library.core.book.dto.BookSearchRequest;
import net.java21.library.core.book.dto.BookSearchResponse;
import net.java21.library.core.book.dto.BookSearchResult;
import net.java21.library.core.book.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 벡터 기반 검색 전략
 *
 * <p>저장소가 {@code hnsw.ef_search}를 트랜잭션 범위(SET LOCAL)로 올리므로 읽기 전용 트랜잭션 안에서 조회합니다.</p>
 */
@Component
@RequiredArgsConstructor
public class VectorSearchStrategy implements SearchStrategy {

    private final BookRepository bookRepository;

    @Override
    @Transactional(readOnly = true)
    public BookSearchResult search(Pageable pageable, BookSearchRequest request) {
        Page<BookSearchResponse> results = bookRepository.vectorSearch(pageable, request);
        return BookSearchResult.builder()
                .books(results)
                .build();
    }
}
