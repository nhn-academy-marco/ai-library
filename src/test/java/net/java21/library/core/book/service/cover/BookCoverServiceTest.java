package net.java21.library.core.book.service.cover;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.java21.library.core.book.domain.Book;
import net.java21.library.core.book.dto.BookCoverTarget;
import net.java21.library.core.book.event.BookCoverMissingEvent;
import net.java21.library.core.book.repository.BookRepository;
import net.java21.library.external.opennaru.client.LibraryInfoNaruApiClient;
import net.java21.library.external.opennaru.dto.BookDetailInfo;
import net.java21.library.external.opennaru.properties.LibraryApiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookCoverServiceTest {

    private static final String API_KEY = "test-key";
    private static final String ISBN = "9791156759270";
    private static final String COVER_URL = "https://image.aladin.co.kr/product/cover.jpg";

    @Mock
    private LibraryInfoNaruApiClient libraryApiClient;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private LibraryApiProperties properties;
    private Cache<String, Boolean> lookedUpIsbns;

    @BeforeEach
    void setUp() {
        properties = new LibraryApiProperties();
        properties.setKey(API_KEY);
        lookedUpIsbns = Caffeine.newBuilder().maximumSize(1_000).build();
    }

    private BookCoverService service(boolean enabled) {
        return new BookCoverService(libraryApiClient, properties, bookRepository, eventPublisher, lookedUpIsbns, enabled);
    }

    private static Book bookWithoutCover(Long id, String isbn) {
        Book book = new Book(isbn, null, "제목", "저자", "출판사", null, null, null, null, null, null);
        ReflectionTestUtils.setField(book, "id", id);
        return book;
    }

    private static BookDetailInfo detailWithImage(String imageUrl) {
        return BookDetailInfo.builder().isbn13(ISBN).imageUrl(imageUrl).build();
    }

    @Test
    @DisplayName("API 응답에 표지 URL이 있으면 비어 있던 imageUrl을 갱신해 저장한다")
    void enrichCovers_updatesBookWhenImageFound() {
        Book book = bookWithoutCover(1L, ISBN);
        when(libraryApiClient.getBookDetail(ISBN, false)).thenReturn(detailWithImage(COVER_URL));
        when(bookRepository.findAllById(any())).thenReturn(List.of(book));

        int updated = service(true).enrichCovers(List.of(new BookCoverTarget(1L, ISBN, null)));

        assertThat(updated).isEqualTo(1);
        assertThat(book.getImageUrl()).isEqualTo(COVER_URL);
        verify(bookRepository).saveAll(List.of(book));
    }

    @Test
    @DisplayName("API 응답에 표지 URL이 없으면 DB를 조회·저장하지 않는다")
    void enrichCovers_skipsWhenNoImage() {
        when(libraryApiClient.getBookDetail(ISBN, false)).thenReturn(detailWithImage(""));

        int updated = service(true).enrichCovers(List.of(new BookCoverTarget(1L, ISBN, "")));

        assertThat(updated).isZero();
        verifyNoInteractions(bookRepository);
    }

    @Test
    @DisplayName("API가 예외를 던져도 전파하지 않고 건너뛴다")
    void enrichCovers_swallowsApiError() {
        when(libraryApiClient.getBookDetail(ISBN, false)).thenThrow(new IllegalStateException("boom"));

        int updated = service(true).enrichCovers(List.of(new BookCoverTarget(1L, ISBN, null)));

        assertThat(updated).isZero();
        verifyNoInteractions(bookRepository);
    }

    @Test
    @DisplayName("이미 조회한 ISBN은 결과와 관계없이 다시 호출하지 않는다")
    void enrichCovers_doesNotCallApiTwiceForSameIsbn() {
        when(libraryApiClient.getBookDetail(ISBN, false)).thenReturn(null);
        BookCoverService service = service(true);
        List<BookCoverTarget> targets = List.of(new BookCoverTarget(1L, ISBN, null));

        service.enrichCovers(targets);
        service.enrichCovers(targets);

        verify(libraryApiClient, times(1)).getBookDetail(ISBN, false);
    }

    @Test
    @DisplayName("한 번의 이벤트에서 최대 건수까지만 API를 호출한다")
    void enrichCovers_limitsLookupsPerEvent() {
        List<BookCoverTarget> targets = LongStream.rangeClosed(1, BookCoverService.MAX_LOOKUPS_PER_EVENT + 5)
                .mapToObj(i -> new BookCoverTarget(i, String.valueOf(9790000000000L + i), null))
                .toList();

        service(true).enrichCovers(targets);

        verify(libraryApiClient, times(BookCoverService.MAX_LOOKUPS_PER_EVENT)).getBookDetail(anyString(), anyBoolean());
    }

    @Test
    @DisplayName("비활성화 설정이면 API를 호출하지 않는다")
    void enrichCovers_disabled() {
        int updated = service(false).enrichCovers(List.of(new BookCoverTarget(1L, ISBN, null)));

        assertThat(updated).isZero();
        verifyNoInteractions(libraryApiClient, bookRepository);
    }

    @Test
    @DisplayName("API 키가 없으면 API를 호출하지 않는다")
    void enrichCovers_noApiKey() {
        properties.setKey("");

        service(true).enrichCovers(List.of(new BookCoverTarget(1L, ISBN, null)));

        verifyNoInteractions(libraryApiClient);
    }

    @Test
    @DisplayName("이미 표지가 있는 도서는 API를 호출하지 않는다")
    void enrichCovers_skipsBookWithCover() {
        service(true).enrichCovers(List.of(new BookCoverTarget(1L, ISBN, COVER_URL)));

        verifyNoInteractions(libraryApiClient);
    }

    @Test
    @DisplayName("보강 요청 시 표지가 비어 있고 아직 조회하지 않은 도서만 이벤트로 발행한다")
    void requestEnrichment_publishesOnlyMissingAndNotLookedUp() {
        lookedUpIsbns.put("9790000000002", Boolean.TRUE);
        BookCoverTarget missing = new BookCoverTarget(1L, "9790000000001", null);
        BookCoverTarget alreadyLookedUp = new BookCoverTarget(2L, "9790000000002", null);
        BookCoverTarget hasCover = new BookCoverTarget(3L, "9790000000003", COVER_URL);

        service(true).requestEnrichment(List.of(missing, alreadyLookedUp, hasCover));

        ArgumentCaptor<BookCoverMissingEvent> captor = ArgumentCaptor.forClass(BookCoverMissingEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().targets()).containsExactly(missing);
        verifyNoInteractions(libraryApiClient);
    }

    @Test
    @DisplayName("비활성화 설정이면 보강 이벤트를 발행하지 않는다")
    void requestEnrichment_disabled() {
        service(false).requestEnrichment(List.of(new BookCoverTarget(1L, ISBN, null)));

        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("이미 표지가 있는 도서는 엔티티에서 덮어쓰지 않는다")
    void fillMissingCoverImage_doesNotOverwrite() {
        Book book = new Book(ISBN, null, "제목", null, null, null, null, COVER_URL, null, null, null);

        boolean changed = book.fillMissingCoverImage("https://other/cover.jpg");

        assertThat(changed).isFalse();
        assertThat(book.getImageUrl()).isEqualTo(COVER_URL);
    }

    @Test
    @DisplayName("이벤트 대상 목록은 null이면 빈 목록으로 바뀐다")
    void event_nullTargets() {
        assertThat(new BookCoverMissingEvent(null).targets()).isEmpty();
    }
}
