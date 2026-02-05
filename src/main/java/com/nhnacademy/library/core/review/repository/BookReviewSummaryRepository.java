package com.nhnacademy.library.core.review.repository;

import com.nhnacademy.library.core.review.domain.BookReviewSummary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookReviewSummaryRepository extends JpaRepository<BookReviewSummary, Long> {
}
