package com.nhnacademy.library.core.book.repository.impl;

import com.nhnacademy.library.core.book.domain.QBook;
import com.nhnacademy.library.core.book.dto.BookSearchRequest;
import com.nhnacademy.library.core.book.dto.BookSearchResponse;
import com.nhnacademy.library.core.book.dto.QBookSearchResponse;
import com.nhnacademy.library.core.book.repository.BookRepositoryCustom;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
    public List<BookSearchResponse> search(Pageable pageable, BookSearchRequest request) {

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

        return new PageImpl<>(bookSearchResponseList, pageable, totalCount).getContent();
    }

    private BooleanBuilder commonWhere(BookSearchRequest request) {
        BooleanBuilder builder = new BooleanBuilder();

        if (StringUtils.isEmpty(request.keyword())) {
            builder
                    .or(book.bookContent.contains(request.keyword()))
                    .or(book.authorName.contains(request.keyword()))
                    .or(book.publisherName.contains(request.keyword()))
                    .or(book.subtitle.contains(request.keyword()))
                    .or(book.title.contains(request.keyword()))
                    .or(book.volumeTitle.contains(request.keyword()));
        }

        if (StringUtils.isNotEmpty(request.isbn())) {
            builder
                    .or(book.isbn.eq(request.isbn()));
        }

        return builder;
    }

}
