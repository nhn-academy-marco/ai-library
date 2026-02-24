package com.nhnacademy.library.core.book.repository.impl;

import com.nhnacademy.library.core.book.domain.SearchType;
import com.nhnacademy.library.core.book.domain.QBook;
import com.nhnacademy.library.core.book.dto.BookSearchRequest;
import com.nhnacademy.library.core.book.dto.BookSearchResponse;
import com.nhnacademy.library.core.book.dto.QBookSearchResponse;
import com.nhnacademy.library.core.book.repository.BookRepositoryCustom;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.NumberTemplate;
import com.querydsl.core.types.Projections;
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

        if (request.searchType() == SearchType.VECTOR && request.vector() != null) {
            return vectorSearch(pageable, request);
        }

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
                                book.imageUrl,
                                book.bookContent
                        )
                )
                .where(commonWhere(request))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long totalCount = queryFactory
                .select(book.count())
                .from(book)
                .where(commonWhere(request))
                .fetchOne();

        return new PageImpl<>(bookSearchResponseList, pageable, totalCount);
    }

    @Override
    public Page<BookSearchResponse> vectorSearch(Pageable pageable, BookSearchRequest request) {
        if (request.vector() == null) {
            log.warn("[VECTOR_SEARCH] Vector is null, returning empty result");
            return new PageImpl<>(List.of(), pageable, 0);
        }

        String vectorString = arrayToVectorString(request.vector());

        NumberTemplate<Double> similarityTemplate = Expressions.numberTemplate(Double.class, "function('vector_cosine_similarity', {0})", vectorString);

        List<BookSearchResponse> bookSearchResponseList = queryFactory
                .from(book)
                .select(
                        Projections.constructor(
                                BookSearchResponse.class,
                                book.id,
                                book.isbn,
                                book.title,
                                book.volumeTitle,
                                book.authorName,
                                book.publisherName,
                                book.price,
                                book.editionPublishDate,
                                book.imageUrl,
                                book.bookContent,
                                similarityTemplate
                        )
                )
                .where(Expressions.booleanTemplate("embedding is not null"))
                .orderBy(similarityTemplate.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long totalCount = queryFactory
                .select(book.count())
                .from(book)
                .where(Expressions.booleanTemplate("embedding is not null"))
                .fetchOne();

        return new PageImpl<>(bookSearchResponseList, pageable, totalCount);
    }

    private String arrayToVectorString(float[] vector) {
        if (vector == null) {
            throw new IllegalArgumentException("Vector cannot be null");
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            sb.append(vector[i]);
            if (i < vector.length - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    private BooleanBuilder commonWhere(BookSearchRequest request) {
        BooleanBuilder builder = new BooleanBuilder();

        if (StringUtils.isNotEmpty(request.keyword())) {
            String keyword = request.keyword();
            log.info("[BOOK_REPOSITORY] Applying keyword filter: {}", keyword);

            BooleanBuilder keywordBuilder = new BooleanBuilder();
            // 1. LIKE 검색
            keywordBuilder.or(book.title.containsIgnoreCase(keyword))
                    .or(book.authorName.containsIgnoreCase(keyword))
                    .or(book.publisherName.containsIgnoreCase(keyword))
                    .or(book.subtitle.containsIgnoreCase(keyword))
                    .or(book.volumeTitle.containsIgnoreCase(keyword));

            // 2. Full Text Search (PostgreSQL 전용) - 환경에 따라 실패할 수 있으므로 주의
            try {
                BooleanExpression fts = Expressions.booleanTemplate(
                        "function('ts_match_korean', {0}, {1}) = true",
                        book.bookContent,
                        keyword
                );
                keywordBuilder.or(fts);
            } catch (Exception e) {
                log.warn("[BOOK_REPOSITORY] FTS search failed or not supported: {}", e.getMessage());
            }
            
            builder.and(keywordBuilder);
        }

        if (StringUtils.isNotEmpty(request.isbn())) {
            log.info("[BOOK_REPOSITORY] Applying isbn filter: {}", request.isbn());
            builder.and(book.isbn.eq(request.isbn()));
        }

        return builder;
    }

}
