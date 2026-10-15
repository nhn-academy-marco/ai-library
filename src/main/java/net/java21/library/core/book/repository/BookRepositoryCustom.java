package net.java21.library.core.book.repository;

import net.java21.library.core.book.domain.BrowseSort;
import net.java21.library.core.book.dto.BookSearchRequest;
import net.java21.library.core.book.dto.BookSearchResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;

@NoRepositoryBean
public interface BookRepositoryCustom {
    Page<BookSearchResponse> search(Pageable pageable, BookSearchRequest request);
    Page<BookSearchResponse> vectorSearch(Pageable pageable, BookSearchRequest request);

    /**
     * 하이브리드·RAG의 RRF 후보로 쓸 키워드 검색 상위 {@code limit}건을 조회합니다.
     * 전체 건수가 필요 없으므로 count 쿼리를 실행하지 않습니다.
     */
    List<BookSearchResponse> searchCandidates(BookSearchRequest request, int limit);

    /**
     * 하이브리드·RAG의 RRF 후보로 쓸 벡터 검색 상위 {@code limit}건을 조회합니다.
     * count 쿼리를 실행하지 않으며, hnsw.ef_search를 트랜잭션 범위로 올리므로 읽기 전용 트랜잭션 안에서 호출해야 합니다.
     */
    List<BookSearchResponse> vectorSearchCandidates(BookSearchRequest request, int limit);

    /**
     * 검색어 없이 정해진 기준으로 도서를 둘러봅니다. 순서가 고정되어 페이지를 오가도 결과가 바뀌지 않습니다.
     */
    Page<BookSearchResponse> browse(Pageable pageable, BrowseSort sort);
}
