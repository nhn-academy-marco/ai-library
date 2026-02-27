package com.nhnacademy.library.core.book.repository;

import com.nhnacademy.library.core.book.domain.Book;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookRepository extends JpaRepository<Book, Long>, BookRepositoryCustom {

    /**
     * 임베딩이 없는 도서 목록을 조회합니다.
     *
     * @param pageable 페이징 정보
     * @return 임베딩이 없는 도서 목록
     */
    List<Book> findAllByTitleIsNotNullAndBookContentIsNotNullAndEmbeddingIsNull(Pageable pageable);

    /**
     * 도서 ID 목록으로 임베딩 벡터들을 조회합니다.
     *
     * <p>사용자 선호도를 계산할 때, GOOD 피드백을 받은 도서들의
     * 임베딩을 가져오기 위해 사용됩니다.</p>
     *
     * @param bookIds 도서 ID 목록
     * @return 임베딩 벡터 배열 (float[1024][])
     */
    @Query("SELECT b.embedding FROM Book b WHERE b.id IN :bookIds AND b.embedding IS NOT NULL")
    List<float[]> findEmbeddingsByIds(@Param("bookIds") List<Long> bookIds);
}
