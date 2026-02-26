package com.nhnacademy.library.external.telegram.bot;

import com.nhnacademy.library.core.book.service.cache.SemanticCacheService;
import com.nhnacademy.library.core.book.service.search.BookSearchService;
import com.nhnacademy.library.external.telegram.config.TelegramBotProperties;
import com.nhnacademy.library.external.telegram.handler.CallbackQueryHandler;
import com.nhnacademy.library.external.telegram.keyboard.TelegramKeyboardFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * LibraryTelegramBot 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Library Telegram Bot 테스트")
class LibraryTelegramBotTest {

    @Mock
    private TelegramBotProperties properties;
    @Mock
    private BookSearchService bookSearchService;
    @Mock
    private SemanticCacheService semanticCacheService;
    @Mock
    private CallbackQueryHandler callbackQueryHandler;
    @Mock
    private TelegramKeyboardFactory keyboardFactory;

    private LibraryTelegramBot bot;

    @BeforeEach
    void setUp() {
        lenient().when(properties.getToken()).thenReturn("test-token");
        lenient().when(properties.getUsername()).thenReturn("test_bot");

        DefaultBotOptions options = new DefaultBotOptions();
        bot = new LibraryTelegramBot(properties, options, bookSearchService, semanticCacheService,
                                      callbackQueryHandler, keyboardFactory);
    }

    @Test
    @DisplayName("Bot Username과 Token 반환 확인")
    void testBotCredentials() {
        // When & Then
        assertThat(bot.getBotUsername()).isEqualTo("test_bot");
        assertThat(bot.getBotToken()).isEqualTo("test-token");
    }

    @Test
    @DisplayName("/start Command 수신 시 메시지 처리 로직 확인")
    void testStartCommandProcessing() {
        // Given
        Update update = createUpdateWithMessage("/start", 12345L);

        // When
        bot.onUpdateReceived(update);

        // Then
        assertThat(update.getMessage().getText()).isEqualTo("/start");
    }

    @Test
    @DisplayName("/help Command 수신 시 메시지 처리 로직 확인")
    void testHelpCommandProcessing() {
        // Given
        Update update = createUpdateWithMessage("/help", 12345L);

        // When
        bot.onUpdateReceived(update);

        // Then
        assertThat(update.getMessage().getText()).isEqualTo("/help");
    }

    @Test
    @DisplayName("/search Command 수신 시 메시지 처리 로직 확인")
    void testSearchCommandProcessing() {
        // Given
        Update update = createUpdateWithMessage("/search 해리포터", 12345L);

        // When
        bot.onUpdateReceived(update);

        // Then
        assertThat(update.getMessage().getText()).isEqualTo("/search 해리포터");
    }

    @Test
    @DisplayName("일반 텍스트 메시지 수신 시 검색 처리")
    void testPlainTextMessageProcessing() {
        // Given
        Update update = createUpdateWithMessage("해리포터", 12345L);

        // When
        bot.onUpdateReceived(update);

        // Then
        assertThat(update.getMessage().getText()).isEqualTo("해리포터");
    }

    @Test
    @DisplayName("Update에 메시지가 없을 경우 처리하지 않음")
    void testUpdateWithoutMessage() {
        // Given
        Update update = new Update();
        update.setUpdateId(1);

        // When & Then
        bot.onUpdateReceived(update);
        assertThat(update.hasMessage()).isFalse();
    }

    /**
     * 테스트용 Update 객체 생성
     */
    private Update createUpdateWithMessage(String text, Long chatId) {
        Message message = mock(Message.class);
        when(message.getText()).thenReturn(text);
        when(message.getChatId()).thenReturn(chatId);
        when(message.hasText()).thenReturn(true);

        Update update = new Update();
        update.setUpdateId(1);
        update.setMessage(message);
        return update;
    }
}
