package net.java21.library.core.book.service.cover;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import net.java21.library.core.book.domain.Book;
import net.java21.library.core.book.dto.BookCoverTarget;
import net.java21.library.core.book.event.BookCoverMissingEvent;
import net.java21.library.core.book.repository.BookRepository;
import net.java21.library.core.config.CacheConfig;
import net.java21.library.external.opennaru.client.LibraryInfoNaruApiClient;
import net.java21.library.external.opennaru.dto.BookDetailInfo;
import net.java21.library.external.opennaru.properties.LibraryApiProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 도서 표지 보강 서비스
 *
 * <p>흐름:</p>
 * <ol>
 *   <li>{@link #requestEnrichment(List)}: 컨트롤러가 화면에 노출할 도서를 넘기면, 표지가 비어 있고
 *       아직 조회하지 않은 ISBN만 골라 {@link BookCoverMissingEvent}를 발행합니다(동기, DB·API 호출 없음).</li>
 *   <li>{@code BookCoverEventListener}가 별도 스레드에서 {@link #enrichCovers(List)}를 호출합니다.</li>
 *   <li>ISBN마다 도서관정보나루 srchDtlList를 조회하고, 표지 URL이 있으면 {@link Book#fillMissingCoverImage(String)}로
 *       비어 있는 표지만 채워 저장합니다.</li>
 * </ol>
 *
 * <p>API 한도 보호: 조회한 ISBN은 결과와 관계없이 Caffeine 캐시(기본 7일)에 기록해 재호출하지 않고,
 * 한 번의 이벤트에서 최대 {@value #MAX_LOOKUPS_PER_EVENT}건만 조회합니다.
 * {@code cover.enrichment.enabled=false}이거나 API 키가 없으면 아무 것도 하지 않습니다.</p>
 */
@Slf4j
@Service
public class BookCoverService {

    static final int MAX_LOOKUPS_PER_EVENT = 10;
    private static final boolean INCLUDE_LOAN_INFO = false;

    private final LibraryInfoNaruApiClient libraryApiClient;
    private final LibraryApiProperties libraryApiProperties;
    private final BookRepository bookRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Cache<String, Boolean> lookedUpIsbns;
    private final boolean enabled;

    public BookCoverService(LibraryInfoNaruApiClient libraryApiClient,
                            LibraryApiProperties libraryApiProperties,
                            BookRepository bookRepository,
                            ApplicationEventPublisher eventPublisher,
                            @Qualifier(CacheConfig.COVER_LOOKUP_CACHE) Cache<String, Boolean> lookedUpIsbns,
                            @Value("${cover.enrichment.enabled:true}") boolean enabled) {
        this.libraryApiClient = libraryApiClient;
        this.libraryApiProperties = libraryApiProperties;
        this.bookRepository = bookRepository;
        this.eventPublisher = eventPublisher;
        this.lookedUpIsbns = lookedUpIsbns;
        this.enabled = enabled;
    }

    /**
     * 표지가 비어 있는 도서의 보강을 비동기로 요청합니다.
     *
     * <p>요청 스레드에서는 필터링과 이벤트 발행만 하므로 응답 시간에 영향이 거의 없습니다.</p>
     *
     * @param candidates 화면에 노출되는 도서 목록
     */
    public void requestEnrichment(List<BookCoverTarget> candidates) {
        if (!isActive() || candidates == null || candidates.isEmpty()) {
            return;
        }
        List<BookCoverTarget> targets = candidates.stream()
                .filter(BookCoverTarget::isMissingCover)
                .filter(target -> lookedUpIsbns.getIfPresent(normalizeIsbn(target.isbn())) == null)
                .toList();
        if (targets.isEmpty()) {
            return;
        }
        eventPublisher.publishEvent(new BookCoverMissingEvent(targets));
    }

    /**
     * 표지 URL을 조회해 비어 있는 표지를 채웁니다.
     *
     * <p>트랜잭션을 걸지 않습니다. 외부 API 호출 동안 DB 커넥션을 잡지 않도록
     * 조회를 모두 마친 뒤 {@code findAllById}·{@code saveAll}로 한 번에 저장합니다.</p>
     *
     * @param targets 보강 대상 도서
     * @return 표지를 갱신한 도서 수
     */
    public int enrichCovers(List<BookCoverTarget> targets) {
        if (!isActive() || targets == null || targets.isEmpty()) {
            return 0;
        }

        Map<Long, String> foundCovers = new LinkedHashMap<>();
        int lookups = 0;
        for (BookCoverTarget target : targets) {
            if (lookups >= MAX_LOOKUPS_PER_EVENT) {
                log.debug("[COVER] 이벤트당 최대 조회 건수({}) 도달, 나머지는 다음 노출 때 처리", MAX_LOOKUPS_PER_EVENT);
                break;
            }
            if (!target.isMissingCover()) {
                continue;
            }
            String isbn = normalizeIsbn(target.isbn());
            // 원자적으로 조회 이력을 남겨 동시 이벤트에서도 같은 ISBN을 한 번만 호출
            if (lookedUpIsbns.asMap().putIfAbsent(isbn, Boolean.TRUE) != null) {
                continue;
            }
            lookups++;
            String coverUrl = fetchCoverUrl(isbn);
            if (coverUrl != null) {
                foundCovers.put(target.bookId(), coverUrl);
            }
        }

        if (foundCovers.isEmpty()) {
            return 0;
        }
        return saveCovers(foundCovers);
    }

    private int saveCovers(Map<Long, String> foundCovers) {
        List<Book> changed = new ArrayList<>();
        for (Book book : bookRepository.findAllById(foundCovers.keySet())) {
            if (book.fillMissingCoverImage(foundCovers.get(book.getId()))) {
                changed.add(book);
            }
        }
        if (!changed.isEmpty()) {
            bookRepository.saveAll(changed);
            log.info("[COVER] 표지 {}건 갱신", changed.size());
        }
        return changed.size();
    }

    /**
     * 도서관정보나루에서 표지 URL을 조회합니다. 오류·이미지 없음은 null을 반환합니다.
     */
    private String fetchCoverUrl(String isbn) {
        try {
            BookDetailInfo detail = libraryApiClient.getBookDetail(isbn, INCLUDE_LOAN_INFO);
            if (detail == null || detail.imageUrl() == null || detail.imageUrl().isBlank()) {
                log.debug("[COVER] 표지 없음: isbn={}", isbn);
                return null;
            }
            return detail.imageUrl().trim();
        } catch (RuntimeException e) {
            log.warn("[COVER] 표지 조회 실패: isbn={}, error={}", isbn, e.getMessage());
            return null;
        }
    }

    private boolean isActive() {
        if (!enabled) {
            return false;
        }
        String apiKey = libraryApiProperties.getKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.debug("[COVER] 도서관정보나루 API 키가 없어 표지 보강을 건너뜁니다");
            return false;
        }
        return true;
    }

    private static String normalizeIsbn(String isbn) {
        return isbn == null ? "" : isbn.trim();
    }
}
