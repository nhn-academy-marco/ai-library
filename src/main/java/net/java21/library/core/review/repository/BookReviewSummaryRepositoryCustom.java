package net.java21.library.core.review.repository;

import net.java21.library.core.review.dto.BookReviewSummaryStatDto;

import java.util.Optional;

public interface BookReviewSummaryRepositoryCustom {
    Optional<BookReviewSummaryStatDto> selectStat(Long bookId);
}
