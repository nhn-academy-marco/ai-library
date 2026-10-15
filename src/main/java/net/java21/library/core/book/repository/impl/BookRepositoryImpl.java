package net.java21.library.core.book.repository.impl;

import net.java21.library.core.book.domain.BrowseSort;
import net.java21.library.core.book.domain.SearchType;
import net.java21.library.core.book.domain.QBook;
import net.java21.library.core.book.dto.BookSearchRequest;
import net.java21.library.core.book.dto.BookSearchResponse;
import net.java21.library.core.book.repository.BookRepositoryCustom;
import net.java21.library.core.review.domain.QBookReviewSummary;
import net.java21.library.core.review.domain.BookReviewSummary;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.NumberTemplate;
import com.querydsl.core.types.OrderSpecifier;
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

import java.util.Arrays;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookRepositoryImpl implements BookRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    QBook book = QBook.book;
    QBookReviewSummary bookReviewSummary = QBookReviewSummary.bookReviewSummary;

    @Override
    public Page<BookSearchResponse> search(Pageable pageable, BookSearchRequest request) {

        if (request.searchType() == SearchType.VECTOR && request.vector() != null) {
            return vectorSearch(pageable, request);
        }

        // 1. Book 조회 (BookSearchResponse.from() 사용)
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
                                book.bookContent
                        )
                )
                .where(commonWhere(request))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 2. 리뷰 정보 조회 (N+1 문제 방지 - IN 절 사용)
        if (!bookSearchResponseList.isEmpty()) {
            List<Long> bookIds = bookSearchResponseList.stream()
                    .map(BookSearchResponse::getId)
                    .collect(Collectors.toList());

            List<BookReviewSummary> summaries = queryFactory
                    .selectFrom(bookReviewSummary)
                    .where(bookReviewSummary.bookId.in(bookIds))
                    .fetch();

            Map<Long, BookReviewSummary> reviewMap = summaries.stream()
                    .collect(Collectors.toMap(BookReviewSummary::getBookId, s -> s));

            // 3. 리뷰 정보 조합
            bookSearchResponseList.forEach(response -> {
                BookReviewSummary summary = reviewMap.get(response.getId());
                if (summary != null) {
                    response.setAverageRating(summary.getAverageRating());
                    response.setReviewCount(summary.getReviewCount());
                    response.setReviewSummary(summary.getReviewSummary());
                }
            });
        }

        long totalCount = queryFactory
                .select(book.count())
                .from(book)
                .where(commonWhere(request))
                .fetchOne();

        return new PageImpl<>(bookSearchResponseList, pageable, totalCount);
    }

    @Override
    public Page<BookSearchResponse> browse(Pageable pageable, BrowseSort sort) {
        List<BookSearchResponse> books = sort == BrowseSort.POPULAR
                ? fetchPopular(pageable)
                : fetchRecent(pageable);
        attachReviewInfo(books);

        Long totalCount = sort == BrowseSort.POPULAR
                ? queryFactory.select(bookReviewSummary.count())
                        .from(bookReviewSummary)
                        .where(bookReviewSummary.reviewCount.gt(0L))
                        .fetchOne()
                : queryFactory.select(book.count()).from(book).fetchOne();

        return new PageImpl<>(books, pageable, totalCount == null ? 0 : totalCount);
    }

    private List<BookSearchResponse> fetchRecent(Pageable pageable) {
        return queryFactory
                .select(bookResponseProjection())
                .from(book)
                .orderBy(book.editionPublishDate.desc().nullsLast(), book.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }

    private List<BookSearchResponse> fetchPopular(Pageable pageable) {
        // BookReviewSummary는 Book과 연관관계 없이 bookId만 가지므로 연관관계 없는 조인(theta join)을 사용
        OrderSpecifier<?>[] order = {
                bookReviewSummary.reviewCount.desc(),
                bookReviewSummary.averageRating.desc(),
                book.id.desc()
        };
        return queryFactory
                .select(bookResponseProjection())
                .from(bookReviewSummary)
                .join(book).on(book.id.eq(bookReviewSummary.bookId))
                .where(bookReviewSummary.reviewCount.gt(0L))
                .orderBy(order)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }

    private com.querydsl.core.types.ConstructorExpression<BookSearchResponse> bookResponseProjection() {
        return Projections.constructor(
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
                book.bookContent
        );
    }

    /**
     * 조회한 도서에 평점·리뷰 수·리뷰 요약을 채웁니다. (N+1 방지를 위해 IN 절로 한 번에 조회)
     */
    private void attachReviewInfo(List<BookSearchResponse> books) {
        if (books.isEmpty()) {
            return;
        }
        List<Long> bookIds = books.stream().map(BookSearchResponse::getId).toList();
        Map<Long, BookReviewSummary> reviewMap = queryFactory
                .selectFrom(bookReviewSummary)
                .where(bookReviewSummary.bookId.in(bookIds))
                .fetch()
                .stream()
                .collect(Collectors.toMap(BookReviewSummary::getBookId, s -> s));

        books.forEach(response -> {
            BookReviewSummary summary = reviewMap.get(response.getId());
            if (summary != null) {
                response.setAverageRating(summary.getAverageRating());
                response.setReviewCount(summary.getReviewCount());
                response.setReviewSummary(summary.getReviewSummary());
            }
        });
    }

    @Override
    public Page<BookSearchResponse> vectorSearch(Pageable pageable, BookSearchRequest request) {
        if (request.vector() == null) {
            log.warn("[VECTOR_SEARCH] Vector is null, returning empty result");
            return new PageImpl<>(List.of(), pageable, 0);
        }

        String vectorString = arrayToVectorString(request.vector());

        NumberTemplate<Double> similarityTemplate = Expressions.numberTemplate(Double.class, "function('vector_cosine_similarity', {0})", vectorString);

        // 1. Book 벡터 검색
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

        // 2. 리뷰 정보 조회 (N+1 문제 방지 - IN 절 사용)
        if (!bookSearchResponseList.isEmpty()) {
            List<Long> bookIds = bookSearchResponseList.stream()
                    .map(BookSearchResponse::getId)
                    .collect(Collectors.toList());

            List<BookReviewSummary> summaries = queryFactory
                    .selectFrom(bookReviewSummary)
                    .where(bookReviewSummary.bookId.in(bookIds))
                    .fetch();

            Map<Long, BookReviewSummary> reviewMap = summaries.stream()
                    .collect(Collectors.toMap(BookReviewSummary::getBookId, s -> s));

            // 3. 리뷰 정보 조합
            bookSearchResponseList.forEach(response -> {
                BookReviewSummary summary = reviewMap.get(response.getId());
                if (summary != null) {
                    response.setAverageRating(summary.getAverageRating());
                    response.setReviewCount(summary.getReviewCount());
                    response.setReviewSummary(summary.getReviewSummary());
                }
            });
        }

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

        // Arrays.toString()을 사용하여 불필요한 루프 제거
        // Arrays.toString()은 [1.0, 2.0, 3.0] 형식으로 반환
        return Arrays.toString(vector);
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
