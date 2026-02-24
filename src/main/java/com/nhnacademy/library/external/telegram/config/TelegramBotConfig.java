package com.nhnacademy.library.external.telegram.config;

import com.nhnacademy.library.core.book.service.cache.SemanticCacheService;
import com.nhnacademy.library.core.book.service.search.BookSearchService;
import com.nhnacademy.library.external.telegram.bot.LibraryTelegramBot;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.generics.BotSession;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

/**
 * Telegram Bot 설정 클래스
 *
 * <p>Long Polling 방식으로 Telegram 서버에서 메시지를 수신합니다.
 * 애플리케이션 시작 시 Bot을 등록하고, 종료 시 정리합니다.
 */
@Slf4j
@Configuration
public class TelegramBotConfig {

    private final TelegramBotProperties properties;
    private BotSession botSession;

    public TelegramBotConfig(TelegramBotProperties properties) {
        this.properties = properties;
    }

    /**
     * Telegram Long Polling Bot Bean 등록
     */
    @Bean
    public LibraryTelegramBot libraryTelegramBot(BookSearchService bookSearchService,
                                                   SemanticCacheService semanticCacheService) {
        // DefaultBotOptions 생성 (필요시 추가 설정 가능)
        DefaultBotOptions options = new DefaultBotOptions();
        // 예: options.setMaxThreads(5);

        return new LibraryTelegramBot(properties, options, bookSearchService, semanticCacheService);
    }

    /**
     * 애플리케이션 시작 후 Bot 실행
     *
     * <p>TelegramBotsApi를 통해 Bot을 등록하고 Long Polling을 시작합니다.
     * 이 메서드는 별도 스레드에서 Telegram 서버를 지속적으로 폴링합니다.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void startBot(ApplicationReadyEvent event) {
        LibraryTelegramBot bot = event.getApplicationContext().getBean(LibraryTelegramBot.class);
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botSession = botsApi.registerBot(bot);
            log.info("[Telegram] Bot registered successfully: @{}", properties.getUsername());
            log.info("[Telegram] Long Polling thread started in background");
        } catch (TelegramApiException e) {
            log.error("[Telegram] Failed to register bot", e);
        }
    }

    /**
     * 애플리케이션 종료 시 Bot 정리
     *
     * <p>Spring Container가 종료될 때 호출되어 Bot 스레드를 안전하게 종료합니다.
     * 이를 통해 Zombie Thread가 생성되는 것을 방지합니다.
     */
    @PreDestroy
    public void stopBot() {
        if (botSession != null && botSession.isRunning()) {
            try {
                botSession.stop();
                log.info("[Telegram] Bot stopped successfully");
            } catch (Exception e) {
                log.error("[Telegram] Failed to stop bot", e);
            }
        }
    }
}
