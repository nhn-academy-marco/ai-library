package com.nhnacademy.library.external.telegram.bot;

import com.nhnacademy.library.core.book.domain.SearchType;
import com.nhnacademy.library.core.book.dto.BookSearchRequest;
import com.nhnacademy.library.core.book.dto.BookSearchResponse;
import com.nhnacademy.library.core.book.dto.BookSearchResult;
import com.nhnacademy.library.core.book.service.cache.SemanticCacheService;
import com.nhnacademy.library.core.book.service.search.BookSearchService;
import com.nhnacademy.library.external.telegram.config.TelegramBotProperties;
import com.nhnacademy.library.external.telegram.dto.FeedbackStats;
import com.nhnacademy.library.external.telegram.handler.CallbackQueryHandler;
import com.nhnacademy.library.external.telegram.keyboard.TelegramKeyboardFactory;
import com.nhnacademy.library.external.telegram.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;

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
    private final CallbackQueryHandler callbackQueryHandler;
    private final TelegramKeyboardFactory keyboardFactory;
    private final FeedbackService feedbackService;

    public LibraryTelegramBot(TelegramBotProperties properties, DefaultBotOptions options,
                              BookSearchService bookSearchService,
                              SemanticCacheService semanticCacheService,
                              CallbackQueryHandler callbackQueryHandler,
                              TelegramKeyboardFactory keyboardFactory,
                              FeedbackService feedbackService) {
        super(options);
        this.properties = properties;
        this.bookSearchService = bookSearchService;
        this.semanticCacheService = semanticCacheService;
        this.callbackQueryHandler = callbackQueryHandler;
        this.keyboardFactory = keyboardFactory;
        this.feedbackService = feedbackService;
    }

    @Override
    public void onUpdateReceived(Update update) {
        log.debug("[Telegram] onUpdateReceived called");
        try {
            // Callback Query 처리를 최우선으로 수행
            if (update.hasCallbackQuery()) {
                log.info("[Telegram] Received callback query");
                callbackQueryHandler.handleCallback(update);
                return;
            }

            // 일반 메시지 처리
            if (update.hasMessage() && update.getMessage().hasText()) {
                String messageText = update.getMessage().getText();
                Long chatId = update.getMessage().getChatId();

                log.info("[Telegram] Received message from chatId {}: {}", chatId, messageText);

                // Command 분기 처리
                if (messageText.startsWith("/")) {
                    log.debug("[Telegram] Handling command: {}", messageText);
                    handleCommand(update, messageText);
                } else {
                    log.debug("[Telegram] Handling search for keyword: {}", messageText);
                    // 일반 텍스트도 검색으로 처리
                    handleSearch(update, messageText);
                }
            } else {
                log.debug("[Telegram] Received update without message/text: {}", update);
            }
        } catch (Exception e) {
            log.error("[Telegram] Error in onUpdateReceived: {}", e.getMessage(), e);
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

        // /stats <query> 형식 처리
        if (command.startsWith("/stats ")) {
            String query = command.substring("/stats ".length()).trim();
            if (!query.isEmpty()) {
                handleFeedbackStats(update, query);
            } else {
                sendSimpleMessage(chatId, "검색어를 입력해주세요.\n예: /stats 해리포터");
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

            case "/stats":
            case "/mystats":
                handleUserStats(update);
                break;

            default:
                sendUnknownCommandMessage(chatId);
                break;
        }
    }

    /**
     * 도서 검색 처리
     *
     * <p>RAG 검색을 수행하여 AI 추천 사유와 함께 도서를 추천합니다.
     * 캐싱된 추천 도서가 있으면 먼저 보여주고, 검색 결과를 표시합니다.
     * 이미지가 있으면 이미지를 함께 전송합니다.
     */
    private void handleSearch(Update update, String keyword) {
        Long chatId = update.getMessage().getChatId();
        log.info("[Telegram] Starting RAG search for keyword: {}, chatId: {}", keyword, chatId);

        try {
            // 1. 최근 검색어 저장 (피드백용)
            callbackQueryHandler.setRecentQuery(chatId, keyword);

            // 2. RAG 검색 실행 (캐시 확인, LLM 추천 사유 생성 포함)
            log.debug("[Telegram] Creating search request for keyword: {}", keyword);
            Pageable pageable = PageRequest.of(0, 5);
            BookSearchRequest request = new BookSearchRequest(keyword, null, SearchType.RAG, null, false);

            log.debug("[Telegram] Calling bookSearchService.searchBooks() with personalization");
            BookSearchResult result = bookSearchService.searchBooks(pageable, request, chatId);
            log.debug("[Telegram] Search completed, preparing response");

            // 3. 응답 전송 (이미지, 점수, AI 추천 사유 포함)
            log.debug("[Telegram] Sending search result to chatId: {}", chatId);
            sendSearchResult(chatId, keyword, result);

            log.info("[Telegram] RAG Search completed for keyword: {}, hasAIResponse: {}, resultCount: {}",
                    keyword, result.getAiResponse() != null, result.getBooks().getTotalElements());

        } catch (Exception e) {
            log.error("[Telegram] Search failed for keyword: {}, chatId: {}, error: {}", keyword, chatId, e.getMessage(), e);
            sendSimpleMessage(chatId, "검색 중 오류가 발생했습니다. 다시 시도해주세요.");
        }
    }

    /**
     * 검색 결과 전송
     *
     * @param chatId Telegram Chat ID
     * @param keyword 검색어
     * @param result 검색 결과 (AI 추천 사유 포함)
     */
    private void sendSearchResult(Long chatId, String keyword, BookSearchResult result) {
        // 추천 도서 목록 확인
        List<BookSearchResponse> books = result.getBooks().getContent();
        if (books.isEmpty()) {
            sendSimpleMessage(chatId, "❌ 검색 결과가 없습니다.");
            return;
        }

        // 헤더 메시지 (한 번에 구성)
        StringBuilder header = new StringBuilder();
        header.append("📚 \"").append(escapeMarkdown(keyword)).append("\" 검색 결과\n\n");

        // AI 추천 사유가 있으면 표시
        if (result.getAiResponse() != null && !result.getAiResponse().isEmpty()) {
            header.append("🤖 AI 추천 사유\n");
            String aiReason = result.getAiResponse().get(0).getWhy();
            if (aiReason == null || aiReason.isBlank()) {
                aiReason = "-";
            } else if (aiReason.length() > 300) {
                aiReason = aiReason.substring(0, 300) + "...";
            }
            header.append("💬 ").append(aiReason).append("\n\n");
        }

        int displayCount = books.size();
        header.append("검색된 도서 (").append(displayCount).append("개)\n\n");

        sendSimpleMessage(chatId, header.toString());

        for (int i = 0; i < books.size(); i++) {
            BookSearchResponse book = books.get(i);
            sendBookWithScore(chatId, keyword, i + 1, book);
        }
    }

    /**
     * 도서 정보와 점수 전송
     *
     * @param chatId Telegram Chat ID
     * @param keyword 검색어
     * @param index 순번
     * @param book 도서 정보
     */
    private void sendBookWithScore(Long chatId, String keyword, int index, BookSearchResponse book) {
        StringBuilder bookInfo = new StringBuilder();

        // 순번과 제목
        bookInfo.append(index).append(". ").append(book.getTitle()).append("\n");
        bookInfo.append("📖 ").append(book.getAuthorName()).append("\n");

        // 출판사
        if (book.getPublisherName() != null) {
            bookInfo.append("🏢 ").append(book.getPublisherName()).append("\n");
        }

        // 검색 점수 정보
        if (book.getSimilarity() != null && book.getSimilarity() > 0) {
            bookInfo.append(String.format("🎯 유사도: %.2f%%\n", book.getSimilarity() * 100));
        }
        if (book.getRrfScore() != null && book.getRrfScore() > 0) {
            bookInfo.append(String.format("📊 RRF 점수: %.2f\n", book.getRrfScore()));
        }

        // 도서 상세 링크
        bookInfo.append("🔗 상세 보기: https://library.java21.net/books/").append(book.getId()).append("\n");

        // 이미지가 있으면 이미지 전송, 아니면 텍스트만 전송
        if (book.getImageUrl() != null && !book.getImageUrl().isBlank()) {
            sendBookImageWithFeedback(chatId, book.getImageUrl(), bookInfo.toString(),
                keyword, book.getId());
        } else {
            sendBookTextWithFeedback(chatId, bookInfo.toString(),
                keyword, book.getId());
        }

        // 구분선 (빈 줄)
        sendSimpleMessage(chatId, " ");
    }

    /**
     * 도서 텍스트와 피드백 키보드 전송
     *
     * @param chatId Telegram Chat ID
     * @param text   도서 정보 텍스트
     * @param query  검색어
     * @param bookId 도서 ID
     */
    private void sendBookTextWithFeedback(Long chatId, String text, String query, Long bookId) {
        log.info("[Telegram] Sending book text with feedback keyboard to chatId: {}, query: {}, bookId: {}",
                chatId, query, bookId);

        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .replyMarkup(keyboardFactory.createFeedbackKeyboard(query, bookId))
                .build();

        try {
            this.execute(message);
            log.info("[Telegram] ✅ Successfully sent book text with feedback keyboard to chatId {}", chatId);
        } catch (TelegramApiException e) {
            log.error("[Telegram] Failed to send message to chatId {}: {}", chatId, e.getMessage(), e);
            // 키보드가 있는 전송이 실패하면 일반 텍스트로 재시도
            sendSimpleMessage(chatId, text);
        }
    }

    /**
     * 도서 이미지와 피드백 키보드 전송
     *
     * @param chatId  Telegram Chat ID
     * @param imageUrl 이미지 URL
     * @param caption 이미지 캡션 (도서 정보)
     * @param query   검색어
     * @param bookId  도서 ID
     */
    private void sendBookImageWithFeedback(Long chatId, String imageUrl, String caption, String query, Long bookId) {
        log.info("[Telegram] Sending book image with feedback keyboard to chatId: {}, query: {}, bookId: {}",
                chatId, query, bookId);

        try {
            SendPhoto photo = SendPhoto.builder()
                    .chatId(chatId)
                    .photo(new InputFile(imageUrl))
                    .caption(caption)
                    .replyMarkup(keyboardFactory.createFeedbackKeyboard(query, bookId))
                    .build();

            this.execute(photo);
            log.info("[Telegram] ✅ Successfully sent book image with feedback keyboard to chatId {}", chatId);
        } catch (TelegramApiException e) {
            log.error("[Telegram] Failed to send image to chatId {}, sending text instead: {}", chatId, e.getMessage(), e);
            // 이미지 전송 실패 시 텍스트로 대체
            sendBookTextWithFeedback(chatId, caption, query, bookId);
        }

        // 구분선 (빈 줄)
        sendSimpleMessage(chatId, " ");
    }

    /**
     * 도서 이미지 전송
     *
     * @param chatId Telegram Chat ID
     * @param imageUrl 이미지 URL
     * @param caption 이미지 캡션 (도서 정보)
     */
    private void sendBookImage(Long chatId, String imageUrl, String caption) {
        try {
            SendPhoto photo = SendPhoto.builder()
                .chatId(chatId)
                .photo(new InputFile(imageUrl))
                .caption(caption)
                .build();

            this.execute(photo);
            log.debug("[Telegram] Sent book image to chatId {}", chatId);
        } catch (TelegramApiException e) {
            log.error("[Telegram] Failed to send image to chatId {}, sending text instead: {}", chatId, e.getMessage());
            // 이미지 전송 실패 시 텍스트로 대체
            sendSimpleMessage(chatId, caption);
        }
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

                사용법:
                • 도서 제목이나 키워드를 입력하면 자동 검색됩니다
                • /search 키워드 Command로도 검색 가능합니다
                • 자연어 검색도 지원합니다 (예: 해리포터 비슷한 책)

                도움이 필요하시면 /help를 입력하세요
                """)
            .build();

        try {
            this.execute(message);
            log.debug("[Telegram] Welcome message sent to chatId {}", chatId);
        } catch (TelegramApiException e) {
            log.error("[Telegram] Failed to send welcome message to chatId {}: {}", chatId, e.getMessage());
        }
    }

    /**
     * 도움말 메시지 전송
     */
    private void sendHelpMessage(Long chatId) {
        SendMessage message = SendMessage.builder()
            .chatId(chatId)
            .text("""
                📖 도움말

                Command:
                /start - Bot 시작
                /search <키워드> - 도서 검색
                /help - 도움말

                검색 예시:
                • 해리포터
                • 마법사의 돌
                • 주식 투자 방법
                • AI 딥러닝 입문

                자연어 검색 예시:
                • 해리포터와 비슷한 판타지 책
                • 주식 초보자가 읽기 좋은 책
                • AI로 세상을 바꾸는 책

                검색 기능:
                • AI 기반 하이브리드 검색 지원
                • 캐싱된 추천 도서가 있으면 함께 표시
                • 상위 5개 결과를 빠르게 반환

                팁:
                • 검색어는 구체적일수록 좋습니다
                • 자연어로 질문하면 더 정확한 결과를 얻을 수 있습니다

                피드백:
                • /stats <검색어> - 검색어별 피드백 통계 확인
                • /mystats - 내 피드백 내역 확인
                """)
            .build();

        try {
            this.execute(message);
            log.debug("[Telegram] Help message sent to chatId {}", chatId);
        } catch (TelegramApiException e) {
            log.error("[Telegram] Failed to send help message to chatId {}: {}", chatId, e.getMessage());
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
        if (text == null || text.isBlank()) {
            log.warn("[Telegram] Skipping empty message to chatId {}", chatId);
            return;
        }

        SendMessage message = SendMessage.builder()
            .chatId(chatId)
            .text(text)
            .build();

        try {
            this.execute(message);
        } catch (TelegramApiException e) {
            log.error("[Telegram] Failed to send message to chatId {}: {}", chatId, e.getMessage());
        }
    }

    /**
     * 검색어별 피드백 통계 처리
     *
     * @param update Telegram Update 객체
     * @param query  검색어
     */
    private void handleFeedbackStats(Update update, String query) {
        Long chatId = update.getMessage().getChatId();
        log.info("[Telegram] Getting feedback stats for query: {}, chatId: {}", query, chatId);

        try {
            FeedbackStats stats = feedbackService.getQueryFeedbackStats(query);

            StringBuilder message = new StringBuilder();
            message.append("📊 \"").append(escapeMarkdown(query)).append("\" 피드백 통계\n\n");
            message.append("👍 좋았음: ").append(stats.goodCount()).append("건\n");
            message.append("👎 별로였음: ").append(stats.badCount()).append("건\n");
            message.append("📈 전체: ").append(stats.totalCount()).append("건\n");
            message.append("💯 긍정 비율: ").append(String.format("%.1f%%", stats.goodRatio() * 100)).append("\n");
            message.append("⭐ 피드백 점수: ").append(String.format("%.3f", stats.feedbackScore())).append("\n");

            sendSimpleMessage(chatId, message.toString());

        } catch (Exception e) {
            log.error("[Telegram] Failed to get feedback stats for query: {}, chatId: {}", query, chatId, e);
            sendSimpleMessage(chatId, "피드백 통계 조회 중 오류가 발생했습니다.");
        }
    }

    /**
     * 사용자별 피드백 내역 처리
     *
     * @param update Telegram Update 객체
     */
    private void handleUserStats(Update update) {
        Long chatId = update.getMessage().getChatId();
        log.info("[Telegram] Getting user feedback history for chatId: {}", chatId);

        try {
            var feedbacks = feedbackService.getUserFeedback(chatId);

            if (feedbacks.isEmpty()) {
                sendSimpleMessage(chatId, "📊 아직 피드백 내역이 없습니다.\n\n검색 후 도서 하단의 👍👎 버튼으로 피드백을 남겨주세요!");
                return;
            }

            StringBuilder message = new StringBuilder();
            message.append("📊 내 피드백 내역 (").append(feedbacks.size()).append("건)\n\n");

            // 최근 10개만 표시
            int displayCount = Math.min(feedbacks.size(), 10);
            for (int i = 0; i < displayCount; i++) {
                var feedback = feedbacks.get(i);
                String emoji = feedback.getType().name().equals("GOOD") ? "👍" : "👎";
                message.append(emoji)
                       .append(" ")
                       .append(escapeMarkdown(feedback.getQuery()))
                       .append("\n");
            }

            if (feedbacks.size() > 10) {
                message.append("\n... 외 ").append(feedbacks.size() - 10).append("건");
            }

            sendSimpleMessage(chatId, message.toString());

        } catch (Exception e) {
            log.error("[Telegram] Failed to get user feedback history for chatId: {}", chatId, e);
            sendSimpleMessage(chatId, "피드백 내역 조회 중 오류가 발생했습니다.");
        }
    }

    /**
     * Markdown 특수문자 이스케이프 처리
     * Telegram API 오류를 방지하기 위해 특수문자를 제거합니다
     */
    private String escapeMarkdown(String text) {
        if (text == null) {
            return "";
        }
        // Markdown 특수문자 제거
        return text.replace("*", "")
                   .replace("_", "")
                   .replace("[", "")
                   .replace("]", "")
                   .replace("(", "")
                   .replace(")", "")
                   .replace("~", "")
                   .replace("`", "")
                   .replace(">", "")
                   .replace("#", "")
                   .replace("+", "")
                   .replace("-", "")
                   .replace("=", "")
                   .replace("|", "")
                   .replace("{", "")
                   .replace("}", "")
                   .replace(".", "")
                   .replace("!", "");
    }
}
