package com.nhnacademy.library.core.review.repository;

import com.nhnacademy.library.core.review.domain.BookReview;

import java.util.List;

/**
 * QueryDSL을 사용하는 커스텀 리포지토리 인터페이스
 */
public interface BookReviewRepositoryCustom {

    /**
     * 지정된 도서 ID와 마지막 요약 ID보다 큰 ID를 가진 리뷰를 조회
     *
     * @param bookId 도서 ID
     * @param lastSummarizedCount 마지막으로 요약한 리뷰 ID
     * @return 새로운 리뷰 목록
     */
    List<BookReview> findNewReviewsAfterId(Long bookId, Long lastSummarizedCount);
}
