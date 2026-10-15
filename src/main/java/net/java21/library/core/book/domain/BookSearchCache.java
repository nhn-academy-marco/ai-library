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

    private List<BookSearchResponse> books;
    private List<BookAiRecommendationResponse> aiResponse;

    private long createdAt;
}
