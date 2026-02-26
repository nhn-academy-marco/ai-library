package com.nhnacademy.library.external.telegram.entity;

import com.nhnacademy.library.external.telegram.dto.FeedbackType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;

/**
 * 검색 피드백 Entity
 *
 * <p>Telegram Bot을 통해 사용자가 남긴 검색 결과 피드백을 저장합니다.
 */
@Slf4j
@Entity
@Table(name = "search_feedbacks", indexes = {
    @Index(name = "idx_search_feedbacks_chat_id", columnList = "chat_id"),
    @Index(name = "idx_search_feedbacks_created_at", columnList = "created_at"),
    @Index(name = "idx_search_feedbacks_query_book", columnList = "query,book_id"),
    @Index(name = "idx_search_feedbacks_book_id", columnList = "book_id")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SearchFeedback {

    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "search_feedbacks_generator"
    )
    @SequenceGenerator(
        name = "search_feedbacks_generator",
        sequenceName = "search_feedbacks_sequence",
        allocationSize = 1
    )
    private Long id;

    @Column(name = "chat_id", nullable = false)
    private Long chatId;

    @Column(name = "query", nullable = false, length = 500)
    private String query;

    @Column(name = "book_id")
    private Long bookId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private FeedbackType type;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    /**
     * 새로운 피드백을 생성합니다.
     *
     * @param chatId   Telegram 사용자 ID
     * @param query    검색어
     * @param bookId   도서 ID
     * @param type     피드백 타입
     * @return 생성된 SearchFeedback Entity
     */
    public static SearchFeedback of(Long chatId, String query, Long bookId, FeedbackType type) {
        SearchFeedback feedback = new SearchFeedback();
        feedback.chatId = chatId;
        feedback.query = query;
        feedback.bookId = bookId;
        feedback.type = type;
        return feedback;
    }

    /**
     * 저장 전 생성 시간을 자동으로 설정합니다.
     */
    @PrePersist
    protected void onPrePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        log.debug("SearchFeedback created: chatId={}, query={}, bookId={}, type={}",
            chatId, query, bookId, type);
    }

    // Getters
    public Long getId() {
        return id;
    }

    public Long getChatId() {
        return chatId;
    }

    public String getQuery() {
        return query;
    }

    public Long getBookId() {
        return bookId;
    }

    public FeedbackType getType() {
        return type;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
