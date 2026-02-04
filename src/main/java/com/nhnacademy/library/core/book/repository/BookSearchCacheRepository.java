package com.nhnacademy.library.core.book.repository;

import com.nhnacademy.library.core.book.domain.BookSearchCache;
import com.redis.om.spring.repository.RedisDocumentRepository;

public interface BookSearchCacheRepository extends RedisDocumentRepository<BookSearchCache, String> {
}
