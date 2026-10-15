package net.java21.library.core.review.repository;

import net.java21.library.core.review.domain.BookReview;

import java.util.List;

/**
 * QueryDSL을 사용하는 커스텀 리포지토리 인터페이스
 */
public interface BookReviewRepositoryCustom {

    /**
     * 지정된 도서의 리뷰 중 마지막 요약 이후에 추가된 리뷰를 조회
     *
     * <p>도서별 리뷰를 ID 오름차순으로 정렬한 뒤, 이미 요약된 개수({@code lastSummarizedCount})만큼
     * 건너뛴 나머지를 반환합니다. (리뷰 삭제가 없다는 전제)</p>
     *
     * @param bookId 도서 ID
     * @param lastSummarizedCount 마지막 요약 시점의 리뷰 개수
     * @return 새로운 리뷰 목록
     */
    List<BookReview> findNewReviewsAfterId(Long bookId, Long lastSummarizedCount);
}
