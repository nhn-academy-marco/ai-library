package com.nhnacademy.library.external.telegram.repository;

import com.nhnacademy.library.external.telegram.entity.SearchFeedback;
import com.nhnacademy.library.external.telegram.dto.FeedbackType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 검색 피드백 Repository
 */
public interface SearchFeedbackRepository extends JpaRepository<SearchFeedback, Long> {

    /**
     * 특정 사용자의 최근 피드백 목록을 조회합니다.
     *
     * @param chatId Telegram 사용자 ID
     * @return 최근 피드백 목록 (최신순)
     */
    List<SearchFeedback> findByChatIdOrderByCreatedAtDesc(Long chatId);

    /**
     * 특정 도서의 모든 피드백을 조회합니다.
     *
     * @param bookId 도서 ID
     * @return 해당 도서의 피드백 목록
     */
    List<SearchFeedback> findByBookId(Long bookId);

    /**
     * 특정 검색어의 피드백 목록을 조회합니다.
     *
     * @param query 검색어
     * @return 해당 검색어의 피드백 목록
     */
    List<SearchFeedback> findByQuery(String query);

    /**
     * 특정 사용자가 특정 도서에 대해 이미 피드백을 남겼는지 확인합니다.
     *
     * @param chatId Telegram 사용자 ID
     * @param bookId  도서 ID
     * @return 피드백 존재 여부
     */
    @Query("SELECT sf FROM SearchFeedback sf WHERE sf.chatId = :chatId AND sf.bookId = :bookId")
    Optional<SearchFeedback> findByChatIdAndBookId(@Param("chatId") Long chatId,
                                                   @Param("bookId") Long bookId);

    /**
     * 특정 기간 내의 피드백 목록을 조회합니다.
     *
     * @param startDate 시작일
     * @param endDate   종료일
     * @return 해당 기간의 피드백 목록
     */
    @Query("SELECT sf FROM SearchFeedback sf WHERE sf.createdAt BETWEEN :startDate AND :endDate")
    List<SearchFeedback> findByCreatedAtBetween(@Param("startDate") OffsetDateTime startDate,
                                                  @Param("endDate") OffsetDateTime endDate);
}
