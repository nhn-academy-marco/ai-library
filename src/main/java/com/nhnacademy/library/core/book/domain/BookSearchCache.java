package com.nhnacademy.library.core.book.domain;

import com.nhnacademy.library.core.book.dto.BookAiRecommendationResponse;
import com.nhnacademy.library.core.book.dto.BookSearchResponse;
import com.redis.om.spring.annotations.Document;
import com.redis.om.spring.annotations.Indexed;
import com.redis.om.spring.annotations.VectorIndexed;
import lombok.*;
import org.springframework.data.annotation.Id;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("book_search_cache")
public class BookSearchCache {
    @Id
    private String id;

    @Indexed
    private String keyword;

    private float[] vector;

    private List<BookSearchResponse> books;
    private List<BookAiRecommendationResponse> aiResponse;

    @Indexed
    private long createdAt;
}
