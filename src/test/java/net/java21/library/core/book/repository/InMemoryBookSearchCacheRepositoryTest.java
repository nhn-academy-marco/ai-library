package net.java21.library.core.book.repository;

import net.java21.library.core.book.domain.BookSearchCache;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("InMemoryBookSearchCacheRepository 단위 테스트")
class InMemoryBookSearchCacheRepositoryTest {

    @Test
    @DisplayName("최대 개수를 초과하면 가장 오래된 항목이 제거되어야 한다")
    void save_ExceedsMaxSize_EvictsOldest() {
        InMemoryBookSearchCacheRepository repository = new InMemoryBookSearchCacheRepository(2);

        repository.save(BookSearchCache.builder().id("a").keyword("a").build());
        repository.save(BookSearchCache.builder().id("b").keyword("b").build());
        repository.save(BookSearchCache.builder().id("c").keyword("c").build());

        List<String> ids = new ArrayList<>();
        repository.findAll().forEach(entry -> ids.add(entry.getId()));
        assertThat(ids).containsExactly("b", "c");
    }

    @Test
    @DisplayName("findAll 결과를 순회하며 삭제해도 예외가 발생하지 않아야 한다")
    void findAll_DeleteWhileIterating_NoException() {
        InMemoryBookSearchCacheRepository repository = new InMemoryBookSearchCacheRepository(10);
        repository.save(BookSearchCache.builder().keyword("a").build());
        repository.save(BookSearchCache.builder().keyword("b").build());

        for (BookSearchCache entry : repository.findAll()) {
            repository.delete(entry);
        }

        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("최대 개수가 0 이하이면 예외가 발생해야 한다")
    void constructor_NonPositiveMaxSize_Throws() {
        assertThatThrownBy(() -> new InMemoryBookSearchCacheRepository(0))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
