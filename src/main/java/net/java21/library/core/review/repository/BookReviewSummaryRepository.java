package net.java21.library.core.review.repository;

import net.java21.library.core.review.domain.BookReviewSummary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookReviewSummaryRepository extends JpaRepository<BookReviewSummary, Long>, BookReviewSummaryRepositoryCustom {
}
