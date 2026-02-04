package com.nhnacademy.library.core.book.repository;

import com.nhnacademy.library.core.book.domain.BookReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookReviewRepository extends JpaRepository<BookReview, Long> {
    List<BookReview> findAllByBookId(Long bookId);
}
