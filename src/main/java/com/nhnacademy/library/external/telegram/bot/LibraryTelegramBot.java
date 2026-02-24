package com.nhnacademy.library.external.telegram.bot;

import com.nhnacademy.library.core.book.domain.SearchType;
import com.nhnacademy.library.core.book.dto.BookSearchRequest;
import com.nhnacademy.library.core.book.dto.BookSearchResponse;
import com.nhnacademy.library.core.book.dto.BookSearchResult;
import com.nhnacademy.library.core.book.service.cache.SemanticCacheService;
import com.nhnacademy.library.core.book.service.search.BookSearchService;
import com.nhnacademy.library.external.telegram.config.TelegramBotProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;

/**
 * AI Library Telegram Bot
 *
 * <p>Telegram에서 들어오는 메시지를 수신하고 처리합니다.
 * 하이브리드 검색을 지원하며, 캐싱된 추천 도서가 있으면 함께 반환합니다.
 */
@Slf4j
@RequiredArgsConstructor
public class LibraryTelegramBot extends TelegramLongPollingBot {

    private final TelegramBotProperties properties;
    private final BookSearchService bookSearchService;
    private final SemanticCacheService semanticCacheService;

    public LibraryTelegramBot(TelegramBotProperties properties, DefaultBotOptions options,
                              BookSearchService bookSearchService,
                              SemanticCacheService semanticCacheService) {
        super(options);
        this.properties = properties;
        this.bookSearchService = bookSearchService;
        this.semanticCacheService = semanticCacheService;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();

            log.info("[Telegram] Received message from chatId {}: {}", chatId, messageText);

            // Command 분기 처리
            if (messageText.startsWith("/")) {
                handleCommand(update, messageText);
            } else {
                // 일반 텍스트도 검색으로 처리
                handleSearch(update, messageText);
            }
        }
    }

    @Override
    public String getBotUsername() {
        return properties.getUsername();
    }

    @Override
    public String getBotToken() {
        return properties.getToken();
    }

    /**
     * Command 처리
     */
    private void handleCommand(Update update, String command) {
        Long chatId = update.getMessage().getChatId();

        // /search <keyword> 형식 처리
        if (command.startsWith("/search ")) {
            String keyword = command.substring("/search ".length()).trim();
            if (!keyword.isEmpty()) {
                handleSearch(update, keyword);
            } else {
                sendSimpleMessage(chatId, "검색어를 입력해주세요.\n예: /search 해리포터");
            }
            return;
        }

        switch (command) {
            case "/start":
                sendWelcomeMessage(chatId);
                break;

            case "/help":
                sendHelpMessage(chatId);
                break;

            default:
                sendUnknownCommandMessage(chatId);
                break;
        }
    }

    /**
     * 도서 검색 처리
     *
     * <p>하이브리드 검색을 수행하고, 캐싱된 추천 도서가 있으면 함께 반환합니다.
     */
    private void handleSearch(Update update, String keyword) {
        Long chatId = update.getMessage().getChatId();

        try {
            // 1. 하이브리드 검색 실행
            Pageable pageable = PageRequest.of(0, 5);
            BookSearchRequest request = new BookSearchRequest(keyword, null, SearchType.HYBRID, null, false);
            BookSearchResult result = bookSearchService.searchBooks(pageable, request);

            // 2. 캐싱된 추천 도서 확인
            BookSearchRequest ragRequest = new BookSearchRequest(keyword, null, SearchType.RAG, request.vector(), false);
            boolean hasCache = semanticCacheService.findSimilarResult(ragRequest).isPresent();

            // 3. 응답 메시지 생성
            String response = formatSearchResult(keyword, result, hasCache);
            sendSimpleMessage(chatId, response);

            log.info("[Telegram] Search completed for keyword: {}, hasCache: {}, resultCount: {}",
                    keyword, hasCache, result.getBooks().getTotalElements());

        } catch (Exception e) {
            log.error("[Telegram] Search failed for keyword: {}", keyword, e);
            sendSimpleMessage(chatId, "검색 중 오류가 발생했습니다. 다시 시도해주세요.");
        }
    }

    /**
     * 검색 결과 포맷팅
     *
     * @param keyword 검색어
     * @param result 검색 결과
     * @param hasCache 된 추천 도서 존재 여부
     * @return 포맷팅된 메시지
     */
    private String formatSearchResult(String keyword, BookSearchResult result, boolean hasCache) {
        StringBuilder message = new StringBuilder();
        List<BookSearchResponse> books = result.getBooks().getContent();

        // 헤더
        message.append("📚 **\"").append(keyword).append("\"** 검색 결과\n\n");

        // 캐싱된 추천 도서 안내
        if (hasCache) {
            message.append("✨ **AI 추천 도서** (캐시)\n");
            message.append("💡 비슷한 검색어에 대한 추천 도서가 있습니다.\n\n");
        }

        // 검색 결과 (상위 5개)
        if (books.isEmpty()) {
            message.append("❌ 검색 결과가 없습니다.");
        } else {
            message.append("**검색된 도서 (상위 ").append(books.size()).append("개)**\n\n");

            for (int i = 0; i < books.size(); i++) {
                BookSearchResponse book = books.get(i);
                message.append(i + 1).append(". **").append(book.getTitle()).append("**\n");
                message.append("   📖 ").append(book.getAuthorName()).append("\n");

                if (book.getPublisherName() != null) {
                    message.append("   🏢 ").append(book.getPublisherName());
                }
                message.append("\n\n");
            }
        }

        return message.toString();
    }

    /**
     * 환영 메시지 전송
     */
    private void sendWelcomeMessage(Long chatId) {
        SendMessage message = SendMessage.builder()
            .chatId(chatId)
            .text("""
                🎉 AI Library Bot에 오신 것을 환영합니다!

                이 Bot은 AI 기반 하이브리드 검색을 제공합니다.

                **사용법:**
                • 도서 제목이나 키워드를 입력하면 자동 검색됩니다
                • `/search 키워드` Command로도 검색 가능합니다
                • 자연어 검색도 지원합니다 (예: "해리포터 비슷한 책")

                **도움이 필요하시면** `/help`를 입력하세요
                """)
            .parseMode("Markdown")
            .build();

        try {
            this.execute(message);
            log.debug("[Telegram] Welcome message sent to chatId {}", chatId);
        } catch (TelegramApiException e) {
            log.error("[Telegram] Failed to send welcome message to chatId {}", chatId, e);
        }
    }

    /**
     * 도움말 메시지 전송
     */
    private void sendHelpMessage(Long chatId) {
        SendMessage message = SendMessage.builder()
            .chatId(chatId)
            .text("""
                📖 **도움말**

                **Command:**
                /start - Bot 시작
                /search <키워드> - 도서 검색
                /help - 도움말

                **검색 예시:**
                • 해리포터
                • 마법사의 돌
                • 주식 투자 방법
                • AI 딥러닝 입문

                **자연어 검색 예시:**
                • "해리포터와 비슷한 판타지 책"
                • "주식 초보자가 읽기 좋은 책"
                • "AI로 세상을 바꾸는 책"

                **검색 기능:**
                • AI 기반 하이브리드 검색 지원
                • 캐싱된 추천 도서가 있으면 함께 표시
                • 상위 5개 결과를 빠르게 반환

                **팁:**
                • 검색어는 구체적일수록 좋습니다
                • 자연어로 질문하면 더 정확한 결과를 얻을 수 있습니다
                """)
            .parseMode("Markdown")
            .build();

        try {
            this.execute(message);
            log.debug("[Telegram] Help message sent to chatId {}", chatId);
        } catch (TelegramApiException e) {
            log.error("[Telegram] Failed to send help message to chatId {}", chatId, e);
        }
    }

    /**
     * 알 수 없는 Command 메시지 전송
     */
    private void sendUnknownCommandMessage(Long chatId) {
        SendMessage message = SendMessage.builder()
            .chatId(chatId)
            .text("❌ 알 수 없는 Command입니다.\n\n도움이 필요하시면 /help를 입력하세요.")
            .build();

        try {
            this.execute(message);
        } catch (TelegramApiException e) {
            log.error("[Telegram] Failed to send unknown command message to chatId {}", chatId, e);
        }
    }

    /**
     * 간단 메시지 전송
     */
    private void sendSimpleMessage(Long chatId, String text) {
        SendMessage message = SendMessage.builder()
            .chatId(chatId)
            .text(text)
            .build();

        try {
            this.execute(message);
        } catch (TelegramApiException e) {
            log.error("[Telegram] Failed to send message to chatId {}", chatId, e);
        }
    }
}
