package com.nhnacademy.library.core.book.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SearchContext {
    private final boolean warmUp;
    private final boolean allowAiCall;

    public static SearchContext defaultContext() {
        return SearchContext.builder()
                .warmUp(false)
                .allowAiCall(true)
                .build();
    }

    public static SearchContext warmUpContext() {
        return SearchContext.builder()
                .warmUp(true)
                .allowAiCall(true)
                .build();
    }
}
