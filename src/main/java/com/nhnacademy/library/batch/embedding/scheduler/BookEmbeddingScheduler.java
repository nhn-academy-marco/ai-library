package com.nhnacademy.library.batch.embedding.scheduler;

import com.nhnacademy.library.core.book.service.embedding.BookEmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "scheduler.embedding.enabled", havingValue = "true")
@RequiredArgsConstructor
public class BookEmbeddingScheduler {

    private final BookEmbeddingService bookEmbeddingService;
    private static final int BATCH_SIZE = 32;

    @Scheduled(fixedDelay = 5000)
    public void runEmbeddingBatch() {
        log.info("Embedding batch scheduler started.");
        try {
            int processedCount = bookEmbeddingService.processEmptyEmbeddings(BATCH_SIZE);
            if (processedCount > 0) {
                log.info("Successfully processed {} book embeddings.", processedCount);
            } else {
                log.debug("No books to process for embeddings.");
            }
        } catch (Exception e) {
            log.error("Error occurred during embedding batch execution: {}", e.getMessage(), e);
        }
    }
}
