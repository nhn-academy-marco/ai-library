package com.nhnacademy.library.core.book.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class BookSearchEvent extends ApplicationEvent {
    private final String keyword;

    public BookSearchEvent(Object source, String keyword) {
        super(source);
        this.keyword = keyword;
    }
}
