package net.java21.library.core.review.repository;

import net.java21.library.core.review.domain.BookReview;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("BookReviewRepositoryCustomImpl 단위 테스트")
class BookReviewRepositoryCustomImplTest {

    @Test
    @DisplayName("리뷰 ID가 아니라 이미 요약된 리뷰 개수만큼 건너뛰어(offset) 새 리뷰를 조회해야 한다")
    @SuppressWarnings("unchecked")
    void findNewReviewsAfterId_SkipsSummarizedCount() {
        JPAQueryFactory factory = mock(JPAQueryFactory.class);
        JPAQuery<BookReview> query = mock(JPAQuery.class, RETURNS_SELF);
        when(factory.selectFrom(any(com.querydsl.core.types.EntityPath.class))).thenReturn(query);
        when(query.fetch()).thenReturn(List.of());

        new BookReviewRepositoryCustomImpl(factory).findNewReviewsAfterId(10L, 5L);

        ArgumentCaptor<Predicate> predicate = ArgumentCaptor.forClass(Predicate.class);
        verify(query).where(predicate.capture());
        // 리뷰 ID 비교 조건(id > count)이 없어야 함
        assertThat(predicate.getValue().toString()).isEqualTo("bookReview.book.id = 10");
        verify(query).offset(5L);
    }

    @Test
    @DisplayName("lastSummarizedCount가 null이면 처음부터 조회해야 한다")
    @SuppressWarnings("unchecked")
    void findNewReviewsAfterId_NullCount_OffsetZero() {
        JPAQueryFactory factory = mock(JPAQueryFactory.class);
        JPAQuery<BookReview> query = mock(JPAQuery.class, RETURNS_SELF);
        when(factory.selectFrom(any(com.querydsl.core.types.EntityPath.class))).thenReturn(query);
        when(query.fetch()).thenReturn(List.of());

        new BookReviewRepositoryCustomImpl(factory).findNewReviewsAfterId(10L, null);

        verify(query).offset(0L);
    }
}
