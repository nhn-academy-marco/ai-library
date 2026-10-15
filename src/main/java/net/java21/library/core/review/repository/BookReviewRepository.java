package net.java21.library.core.review.repository;

import net.java21.library.core.review.domain.BookReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookReviewRepository extends JpaRepository<BookReview, Long>, BookReviewRepositoryCustom {
    List<BookReview> findAllByBookId(Long bookId);
    Page<BookReview> findAllByBookIdOrderByCreatedAtDesc(Long bookId, Pageable pageable);
}
