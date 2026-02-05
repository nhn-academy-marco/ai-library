package com.nhnacademy.library.core.review.repository;

import com.nhnacademy.library.core.review.dto.BookReviewSummaryStatDto;

import java.util.Optional;

public interface BookReviewSummaryRepositoryCustom {
    Optional<BookReviewSummaryStatDto> selectStat(Long bookId);
}
