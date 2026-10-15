package net.java21.library.core.review.repository;

import net.java21.library.core.review.domain.BookReview;
import net.java21.library.core.review.domain.QBookReview;
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

        // 리뷰 ID는 전체 도서에 걸쳐 증가하므로 도서별 리뷰 개수와 비교할 수 없음.
        // 도서별 리뷰를 ID 오름차순으로 정렬한 뒤, 이미 요약된 개수만큼 건너뛴다(offset).
        long offset = lastSummarizedCount != null ? Math.max(lastSummarizedCount, 0L) : 0L;

        return jpaQueryFactory
                .selectFrom(bookReview)
                .where(bookReview.book.id.eq(bookId))
                .orderBy(bookReview.id.asc())
                .offset(offset)
                .fetch();
    }
}
