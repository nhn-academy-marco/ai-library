package com.nhnacademy.library.core.book.event;

import com.nhnacademy.library.core.book.service.cache.BookSearchCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookSearchEventListener {

    private final BookSearchCacheService bookSearchCacheService;

    @Async
    @EventListener
    public void handleBookSearchEvent(BookSearchEvent event) {
        log.info("[EVENT_LISTENER] Received search event for keyword: {}", event.getKeyword());
        bookSearchCacheService.warmUpRagCache(event.getKeyword());
    }
}
