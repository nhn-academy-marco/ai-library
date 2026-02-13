package com.nhnacademy.library.core.review.repository;

import com.nhnacademy.library.core.review.domain.BookReview;
import com.nhnacademy.library.core.review.domain.QBookReview;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * QueryDSL을 사용하는 커스텀 리포지토리 구현체
 */
@Component
@RequiredArgsConstructor
public class BookReviewRepositoryCustomImpl implements BookReviewRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public List<BookReview> findNewReviewsAfterId(Long bookId, Long lastSummarizedCount) {
        QBookReview bookReview = QBookReview.bookReview;

        return jpaQueryFactory
                .selectFrom(bookReview)
                .where(
                        bookReview.book.id.eq(bookId)
                                .and(bookReview.id.gt(lastSummarizedCount))
                )
                .orderBy(bookReview.id.asc())
                .fetch();
    }
}
