package net.java21.library.core.book.domain;

import net.java21.library.core.book.dto.BookAiRecommendationResponse;
import net.java21.library.core.book.dto.BookSearchResponse;
import lombok.*;
import org.springframework.data.annotation.Id;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookSearchCache {
    @Id
    private String id;

    private String keyword;

    private float[] vector;

    /**
     * 이전 버전 호환용 필드. 예전에는 AI가 고른 도서 목록을 저장해 화면 목록으로 썼으나,
     * 지금은 목록을 매 요청 하이브리드 검색으로 채우므로 새 항목에는 저장하지 않습니다.
     * 이전 항목이 남아 있으면 도서 정보가 없는 추천 항목을 채우는 데만 사용합니다.
     */
    private List<BookSearchResponse> books;
    /** AI 추천 결과 (추천 패널 표시용 도서 정보 포함) */
    private List<BookAiRecommendationResponse> aiResponse;

    private long createdAt;
}
