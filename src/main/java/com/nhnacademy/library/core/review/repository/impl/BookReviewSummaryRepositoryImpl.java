package com.nhnacademy.library.core.review.repository.impl;

import com.nhnacademy.library.core.review.domain.QBookReview;
import com.nhnacademy.library.core.review.dto.BookReviewSummaryStatDto;
import com.nhnacademy.library.core.review.dto.QBookReviewSummaryStatDto;
import com.nhnacademy.library.core.review.repository.BookReviewSummaryRepositoryCustom;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class BookReviewSummaryRepositoryImpl implements BookReviewSummaryRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    QBookReview qBookReview = QBookReview.bookReview;

    @Override
    public Optional<BookReviewSummaryStatDto> selectStat(Long bookId) {

        return Optional.ofNullable(queryFactory.from(qBookReview)
                .select(new QBookReviewSummaryStatDto(
                        qBookReview.count(),
                        qBookReview.rating.avg(),
                        qBookReview.rating
                                .when(1).then(1)
                                .otherwise(0)
                                .sum(),
                        qBookReview.rating
                                .when(2).then(1)
                                .otherwise(0)
                                .sum(),
                        qBookReview.rating
                                .when(3).then(1)
                                .otherwise(0)
                                .sum(),
                        qBookReview.rating
                                .when(4).then(1)
                                .otherwise(0)
                                .sum(),
                        qBookReview.rating
                                .when(5).then(1)
                                .otherwise(0)
                                .sum(),
                        qBookReview.createdAt.max()

                ))
                .where(qBookReview.book.id.eq(bookId))
                .fetchOne());
    }
}
