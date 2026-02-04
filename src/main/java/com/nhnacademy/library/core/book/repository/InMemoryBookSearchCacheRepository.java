package com.nhnacademy.library.core.book.repository;

import com.nhnacademy.library.core.book.domain.BookSearchCache;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryBookSearchCacheRepository implements BookSearchCacheRepository {
    private final Map<String, BookSearchCache> cache = new ConcurrentHashMap<>();

    @Override
    public BookSearchCache save(BookSearchCache entry) {
        if (entry.getId() == null) {
            entry.setId(UUID.randomUUID().toString());
        }
        cache.put(entry.getId(), entry);
        return entry;
    }

    @Override
    public Iterable<BookSearchCache> findAll() {
        return cache.values();
    }

    @Override
    public void delete(BookSearchCache entry) {
        if (entry.getId() != null) {
            cache.remove(entry.getId());
        }
    }

    @Override
    public void deleteAll() {
        cache.clear();
    }
}
