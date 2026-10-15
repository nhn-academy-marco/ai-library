package net.java21.library.core.book.event;

import net.java21.library.core.book.dto.BookCoverTarget;

import java.util.List;

/**
 * 표지가 비어 있는 도서가 화면에 노출되었음을 알리는 이벤트
 *
 * @param targets 표지 보강 대상 도서 목록
 */
public record BookCoverMissingEvent(List<BookCoverTarget> targets) {

    public BookCoverMissingEvent {
        targets = targets == null ? List.of() : List.copyOf(targets);
    }
}
