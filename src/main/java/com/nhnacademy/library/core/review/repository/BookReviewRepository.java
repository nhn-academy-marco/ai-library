package com.nhnacademy.library.core.review.repository;

import com.nhnacademy.library.core.review.domain.BookReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookReviewRepository extends JpaRepository<BookReview, Long> {
    List<BookReview> findAllByBookId(Long bookId);
    Page<BookReview> findAllByBookIdOrderByCreatedAtDesc(Long bookId, Pageable pageable);

    @Query("SELECT r FROM BookReview r " +
           "WHERE r.book.id = :bookId " +
           "AND r.id > :lastSummarizedCount " +
           "ORDER BY r.id ASC")
    List<BookReview> findByBookIdAndIdGreaterThanOrderByIdAsc(
            @Param("bookId") Long bookId,
            @Param("lastSummarizedCount") Long lastSummarizedCount
    );
}
