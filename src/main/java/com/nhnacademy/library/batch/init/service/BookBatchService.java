package com.nhnacademy.library.batch.init.service;

import com.nhnacademy.library.batch.init.dto.BookRawData;
import com.nhnacademy.library.batch.init.mapper.BookMapper;
import com.nhnacademy.library.core.book.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookBatchService {

    private final BookRepository bookRepository;

    @Transactional
    public void initializeBooks(List<BookRawData> buffer, int batchSize) {
        log.info("Starting book initialization. Buffer size: {}, Batch size: {}", buffer.size(), batchSize);
        log.info("Deleting all existing books before batch load.");
        bookRepository.deleteAll();
        bookRepository.flush();

        for (int i = 0; i < buffer.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, buffer.size());
            log.debug("Saving batch from {} to {}", i, endIndex);
            List<BookRawData> batch = buffer.subList(i, endIndex);
            bookRepository.saveAll(BookMapper.toEntity(batch));
        }
        log.info("Book initialization completed.");
    }
}
