package com.nhnacademy.library.core.book.repository;

import com.nhnacademy.library.core.book.domain.BookSearchCache;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Repository
public class InMemoryBookSearchCacheRepository implements BookSearchCacheRepository {

    /**
     * 최대 보관 개수. 초과 시 가장 먼저 저장된 항목부터 제거합니다.
     */
    private final int maxSize;

    // 삽입 순서를 유지하는 LinkedHashMap (동시 접근은 synchronized로 보호)
    private final Map<String, BookSearchCache> cache;

    public InMemoryBookSearchCacheRepository(@Value("${cache.semantic.max-size:1000}") int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize must be positive: " + maxSize);
        }
        this.maxSize = maxSize;
        this.cache = new LinkedHashMap<>() {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, BookSearchCache> eldest) {
                return size() > InMemoryBookSearchCacheRepository.this.maxSize;
            }
        };
    }

    @Override
    public synchronized BookSearchCache save(BookSearchCache entry) {
        if (entry.getId() == null) {
            entry.setId(UUID.randomUUID().toString());
        }
        cache.put(entry.getId(), entry);
        return entry;
    }

    @Override
    public synchronized Iterable<BookSearchCache> findAll() {
        // 순회 중 다른 스레드의 수정에 영향받지 않도록 스냅샷 반환
        return new ArrayList<>(cache.values());
    }

    @Override
    public synchronized void delete(BookSearchCache entry) {
        if (entry.getId() != null) {
            cache.remove(entry.getId());
        }
    }

    @Override
    public synchronized void deleteAll() {
        cache.clear();
    }
}
