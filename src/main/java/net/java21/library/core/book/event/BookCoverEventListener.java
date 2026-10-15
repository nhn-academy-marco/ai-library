package net.java21.library.core.book.event;

import net.java21.library.core.book.service.cover.BookCoverService;
import net.java21.library.core.config.AsyncConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 표지 보강 이벤트 리스너
 *
 * <p>{@link BookCoverService}와 다른 빈으로 분리해 {@code @Async}가 프록시를 거치게 합니다.
 * 전용 Executor에서 실행되므로 검색·상세 응답 시간에 영향을 주지 않습니다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookCoverEventListener {

    private final BookCoverService bookCoverService;

    @Async(AsyncConfig.COVER_ENRICHMENT_EXECUTOR)
    @EventListener
    public void handleBookCoverMissing(BookCoverMissingEvent event) {
        try {
            int updated = bookCoverService.enrichCovers(event.targets());
            log.debug("[COVER] 표지 보강 완료: 대상 {}건, 갱신 {}건", event.targets().size(), updated);
        } catch (RuntimeException e) {
            log.warn("[COVER] 표지 보강 중 오류: {}", e.getMessage());
        }
    }
}
