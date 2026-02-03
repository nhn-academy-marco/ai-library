package com.nhnacademy.library.core.book.repository.impl;

import com.nhnacademy.library.core.book.domain.QBook;
import com.nhnacademy.library.core.book.dto.BookSearchRequest;
import com.nhnacademy.library.core.book.dto.BookSearchResponse;
import com.nhnacademy.library.core.book.dto.QBookSearchResponse;
import com.nhnacademy.library.core.book.repository.BookRepositoryCustom;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;


import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookRepositoryImpl implements BookRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    QBook book = QBook.book;

    @Override
    public Page<BookSearchResponse> search(Pageable pageable, BookSearchRequest request) {

        List<BookSearchResponse> bookSearchResponseList = queryFactory
                .from(book)
                .select(
                        new QBookSearchResponse(
                                book.id,
                                book.isbn,
                                book.title,
                                book.volumeTitle,
                                book.authorName,
                                book.publisherName,
                                book.price,
                                book.editionPublishDate,
                                book.imageUrl
                        )
                )
                .where(commonWhere(request))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long totalCount = queryFactory
                .selectFrom(book)
                .where(commonWhere(request))
                .fetchCount();

        return new PageImpl<>(bookSearchResponseList, pageable, totalCount);
    }

    private BooleanBuilder commonWhere(BookSearchRequest request) {
        BooleanBuilder builder = new BooleanBuilder();

        if (StringUtils.isNotEmpty(request.keyword())) {
            String keyword = request.keyword();

            // 1. LIKE 검색 (기존)
            builder.or(book.title.contains(keyword))
                    .or(book.authorName.contains(keyword))
                    .or(book.publisherName.contains(keyword))
                    .or(book.subtitle.contains(keyword))
                    .or(book.volumeTitle.contains(keyword));

            // 2. Full Text Search (PostgreSQL 전용)
            // book_content 필드에 대해 전문 검색 적용
            // plainto_tsquery를 사용하여 검색어를 tsquery로 변환하고 @@ 연산자로 매칭 확인
            // 하이버네이트 6의 SyntaxException을 방지하기 위해 전체를 하나의 SQL 템플릿으로 처리
            BooleanExpression fts = Expressions.booleanTemplate(
                    "function('ts_match_korean', {0}, {1}) = true",
                    book.bookContent,
                    keyword
            );
            builder.or(fts);
        }

        if (StringUtils.isNotEmpty(request.isbn())) {
            builder.and(book.isbn.eq(request.isbn()));
        }

        return builder;
    }

}
