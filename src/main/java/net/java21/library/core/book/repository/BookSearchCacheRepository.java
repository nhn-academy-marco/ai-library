package net.java21.library.core.book.repository;

import net.java21.library.core.book.domain.BookSearchCache;
import java.util.Optional;

public interface BookSearchCacheRepository {
    BookSearchCache save(BookSearchCache cache);
    Iterable<BookSearchCache> findAll();
    void delete(BookSearchCache cache);
    void deleteAll();
}
