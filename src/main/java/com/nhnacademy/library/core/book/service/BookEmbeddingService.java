package com.nhnacademy.library.core.book.service;

import com.nhnacademy.library.core.book.domain.Book;
import com.nhnacademy.library.core.book.repository.BookRepository;
import com.nhnacademy.library.core.book.util.TextPreprocessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookEmbeddingService {

    private final BookRepository bookRepository;
    private final EmbeddingService embeddingService;

    @Transactional
    public int processEmptyEmbeddings(int batchSize) {
        // 1. 제목과 내용이 모두 있고 임베딩이 없는 도서 조회
        List<Book> booksToProcess = bookRepository.findAllByTitleIsNotNullAndBookContentIsNotNullAndEmbeddingIsNull(PageRequest.of(0, batchSize));

        if (booksToProcess.isEmpty()) {
            return 0;
        }

        log.info("Processing {} books for embeddings in core service using batch mode.", booksToProcess.size());

        // 2. 데이터 전처리 및 결합 (유효한 데이터만 필터링)
        List<Book> validBooks = new ArrayList<>();
        List<String> combinedTexts = new ArrayList<>();

        for (Book book : booksToProcess) {
            String combinedText = createCombinedText(book);
            log.info("Preprocessed text for book ID: {}: {}", book.getId(), combinedText);
            if (StringUtils.hasText(combinedText)) {
                validBooks.add(book);
                combinedTexts.add(combinedText);
            } else {
                log.warn("Skipping book ID: {} due to empty combined text after preprocessing.", book.getId());
                // 제목 조차 없는 경우 (최소한의 정보 부족)
                log.warn("Skipping book ID: {} due to empty title and content after preprocessing.", book.getId());
            }
        }

        if (combinedTexts.isEmpty()) {
            return 0;
        }

        try {
            // 3. 한 번에 여러 텍스트 임베딩 생성 (Batch Embedding)
            List<float[]> embeddings = embeddingService.getEmbeddings(combinedTexts);

            // 4. 결과 매핑 및 업데이트 (Dirty Checking 활용)
            for (int i = 0; i < validBooks.size(); i++) {
                validBooks.get(i).updateEmbedding(embeddings.get(i));
            }

            log.info("Successfully generated embeddings for {} books.", validBooks.size());
            return validBooks.size();
        } catch (Exception e) {
            log.error("Failed to process batch embedding. Error: {}", e.getMessage(), e);
            throw e; // 트랜잭션 롤백을 위해 예외 던짐
        }
    }

    private String createCombinedText(Book book) {
        String title = TextPreprocessor.preprocess(book.getTitle());
        String volumeTitle = TextPreprocessor.preprocess(book.getVolumeTitle());
        String subtitle = TextPreprocessor.preprocess(book.getSubtitle());
        String author = TextPreprocessor.preprocess(book.getAuthorName());
        String content = TextPreprocessor.preprocess(book.getBookContent());

        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(title)) {
            sb.append("[제목] ").append(title).append(" ");
        }
        if (StringUtils.hasText(volumeTitle)) {
            sb.append("[권차제목] ").append(volumeTitle).append(" ");
        }
        if (StringUtils.hasText(subtitle)) {
            sb.append("[부제] ").append(subtitle).append(" ");
        }
        if (StringUtils.hasText(author)) {
            sb.append("[저자] ").append(author).append(" ");
        }
        if (StringUtils.hasText(content)) {
            sb.append("[내용] ").append(content);
        }

        return sb.toString().trim();
    }
}
