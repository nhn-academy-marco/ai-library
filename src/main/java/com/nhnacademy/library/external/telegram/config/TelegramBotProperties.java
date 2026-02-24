package com.nhnacademy.library.external.telegram.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Telegram Bot 설정 프로퍼티
 */
@Component
@ConfigurationProperties(prefix = "telegram.bot")
public class TelegramBotProperties {
    /**
     * Bot 활성화 여부
     */
    private boolean enabled = false;

    /**
     * Bot API Token
     */
    private String token;

    /**
     * Bot 사용자명 (@username)
     */
    private String username;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
