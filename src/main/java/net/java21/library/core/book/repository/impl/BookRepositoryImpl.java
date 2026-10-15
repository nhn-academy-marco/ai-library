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
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Value;
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
public class BookRepositoryImpl implements BookRepositoryCustom {

    /** 키워드 정렬: 제목 일치 도서의 순위 값 (작을수록 앞) */
    private static final int TITLE_MATCH_RANK = 0;
    /** 키워드 정렬: 제목 외 필드(저자·출판사·본문 등)로 일치한 도서의 순위 값 */
    private static final int OTHER_MATCH_RANK = 1;

    /** pgvector가 허용하는 hnsw.ef_search 최댓값 */
    private static final long MAX_HNSW_EF_SEARCH = 1000;
    /** 트랜잭션 범위(SET LOCAL)로 hnsw.ef_search를 바꾸는 SQL */
    private static final String SET_LOCAL_EF_SEARCH_SQL = "SELECT set_config('hnsw.ef_search', ?1, true)";

    private final JPAQueryFactory queryFactory;
    private final EntityManager entityManager;
    /** 벡터 조회 시 HNSW 탐색 폭. 조회 행 수(offset + limit)가 더 크면 그 값을 씁니다. */
    private final int hnswEfSearch;

    public BookRepositoryImpl(JPAQueryFactory queryFactory,
                              EntityManager entityManager,
                              @Value("${search.vector.ef-search:120}") int hnswEfSearch) {
        this.queryFactory = queryFactory;
        this.entityManager = entityManager;
        this.hnswEfSearch = hnswEfSearch;
    }

    QBook book = QBook.book;
    QBookReviewSummary bookReviewSummary = QBookReviewSummary.bookReviewSummary;

    @Override
    public Page<BookSearchResponse> search(Pageable pageable, BookSearchRequest request) {

        if (request.searchType() == SearchType.VECTOR && request.vector() != null) {
            return vectorSearch(pageable, request);
        }

        List<BookSearchResponse> bookSearchResponseList = fetchKeyword(request, pageable.getOffset(), pageable.getPageSize());

        // 키워드 단독 검색은 전체 페이지 수가 필요하므로 count를 유지합니다.
        Long totalCount = queryFactory
                .select(book.count())
                .from(book)
                .where(commonWhere(request))
                .fetchOne();

        return new PageImpl<>(bookSearchResponseList, pageable, totalCount == null ? 0 : totalCount);
    }

    @Override
    public List<BookSearchResponse> searchCandidates(BookSearchRequest request, int limit) {
        return fetchKeyword(request, 0, limit);
    }

    /**
     * 키워드 조건에 맞는 도서를 정렬 기준대로 조회하고 리뷰 정보를 채웁니다. (count 없음)
     */
    private List<BookSearchResponse> fetchKeyword(BookSearchRequest request, long offset, int limit) {
        List<BookSearchResponse> books = queryFactory
                .from(book)
                .select(bookResponseProjection())
                .where(commonWhere(request))
                .orderBy(keywordOrder(request))
                .offset(offset)
                .limit(limit)
                .fetch();
        attachReviewInfo(books);
        return books;
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

        List<BookSearchResponse> bookSearchResponseList = fetchVector(request, pageable.getOffset(), pageable.getPageSize());

        Long totalCount = queryFactory
                .select(book.count())
                .from(book)
                .where(Expressions.booleanTemplate("embedding is not null"))
                .fetchOne();

        return new PageImpl<>(bookSearchResponseList, pageable, totalCount == null ? 0 : totalCount);
    }

    @Override
    public List<BookSearchResponse> vectorSearchCandidates(BookSearchRequest request, int limit) {
        if (request.vector() == null) {
            log.warn("[VECTOR_SEARCH] Vector is null, returning empty candidates");
            return List.of();
        }
        return fetchVector(request, 0, limit);
    }

    /**
     * 질의 벡터와 가까운 도서를 코사인 거리 오름차순으로 조회하고 리뷰 정보를 채웁니다. (count 없음)
     *
     * <p>pgvector HNSW 인덱스는 한 번에 hnsw.ef_search(기본 40)건보다 많은 결과를 돌려주지 못하므로,
     * 조회 직전에 같은 트랜잭션 안에서 ef_search를 LIMIT 이상으로 올립니다.</p>
     */
    private List<BookSearchResponse> fetchVector(BookSearchRequest request, long offset, int limit) {
        applyHnswEfSearch(offset + limit);

        String vectorString = arrayToVectorString(request.vector());

        // 유사도(1 - 거리)는 화면·RRF용 값으로만 select하고, 정렬은 거리(embedding <=> q) 오름차순으로 합니다.
        // pgvector의 HNSW/IVFFlat 인덱스는 "ORDER BY embedding <=> q" 형태에서만 쓰이기 때문입니다. (순서는 동일)
        NumberTemplate<Double> similarityTemplate = Expressions.numberTemplate(Double.class, "function('vector_cosine_similarity', {0})", vectorString);
        NumberTemplate<Double> distanceTemplate = Expressions.numberTemplate(Double.class, "function('vector_cosine_distance', {0})", vectorString);

        List<BookSearchResponse> books = queryFactory
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
                .orderBy(distanceTemplate.asc(), book.id.asc()) // 거리가 같으면 ID로 순서 고정
                .offset(offset)
                .limit(limit)
                .fetch();
        attachReviewInfo(books);
        return books;
    }

    /**
     * 현재 트랜잭션에 한해 hnsw.ef_search를 올립니다. ({@code SET LOCAL}과 같은 {@code set_config(..., true)})
     *
     * <p>트랜잭션 밖에서는 해당 문장에만 적용되고 사라지므로, 호출하는 쪽이 읽기 전용 트랜잭션 안에서 조회해야 합니다.
     * 인덱스가 없는 환경에서도 설정만 바뀔 뿐 오류가 나지 않습니다.</p>
     *
     * @param requiredRows 이번 조회에서 받아야 하는 행 수(offset + limit)
     */
    private void applyHnswEfSearch(long requiredRows) {
        long efSearch = Math.min(MAX_HNSW_EF_SEARCH, Math.max(hnswEfSearch, requiredRows));
        entityManager.createNativeQuery(SET_LOCAL_EF_SEARCH_SQL)
                .setParameter(1, String.valueOf(efSearch))
                .getSingleResult();
    }

    /**
     * 키워드 검색 정렬 기준을 만듭니다.
     *
     * <p>정렬이 없으면 같은 요청이라도 실행 계획에 따라 결과 순서가 달라지고,
     * 하이브리드 검색은 이 순서를 RRF 순위로 쓰므로 결과가 매번 바뀝니다. 그래서 다음 순서로 고정합니다.</p>
     * <ol>
     *   <li>제목에 키워드가 포함된 도서 우선 (저자·출판사·본문 일치보다 관련도가 높다고 봄)</li>
     *   <li>출간일 최신순 (출간일 없는 도서는 뒤로)</li>
     *   <li>도서 ID 오름차순 (동점 처리, 결정적 순서 보장)</li>
     * </ol>
     * <p>WHERE 조건으로 걸러진 행만 정렬하므로 별도 인덱스 없이도 비용이 크지 않습니다.</p>
     */
    private OrderSpecifier<?>[] keywordOrder(BookSearchRequest request) {
        OrderSpecifier<?> byDate = book.editionPublishDate.desc().nullsLast();
        OrderSpecifier<?> byId = book.id.asc();
        if (StringUtils.isEmpty(request.keyword())) {
            return new OrderSpecifier<?>[]{byDate, byId};
        }
        NumberExpression<Integer> titleMatchRank = new CaseBuilder()
                .when(book.title.containsIgnoreCase(request.keyword())).then(TITLE_MATCH_RANK)
                .otherwise(OTHER_MATCH_RANK);
        return new OrderSpecifier<?>[]{titleMatchRank.asc(), byDate, byId};
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
