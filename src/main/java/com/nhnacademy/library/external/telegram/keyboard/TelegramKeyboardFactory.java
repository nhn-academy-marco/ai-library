package com.nhnacademy.library.external.telegram.keyboard;

import com.nhnacademy.library.external.telegram.dto.FeedbackType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Telegram Inline Keyboard 생성 Factory
 */
@Slf4j
@Component
public class TelegramKeyboardFactory {

    /**
     * 피드백 Inline Keyboard를 생성합니다.
     *
     * @param query  검색어
     * @param bookId 도서 ID
     * @return Inline Keyboard Markup
     */
    public InlineKeyboardMarkup createFeedbackKeyboard(String query, Long bookId) {
        log.info("[Telegram] Creating feedback keyboard for query: {}, bookId: {}", query, bookId);

        // 검색어 URL 인코딩
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);

        // 콜백 데이터 생성: feedback:{query}:{bookId}:{type}
        String goodCallback = String.format("feedback:%s:%d:GOOD", encodedQuery, bookId);
        String badCallback = String.format("feedback:%s:%d:BAD", encodedQuery, bookId);

        log.debug("[Telegram] Callback data - GOOD: {}, BAD: {}", goodCallback, badCallback);

        // 버튼 생성
        InlineKeyboardButton goodButton = InlineKeyboardButton.builder()
                .text("👍 좋았음")
                .callbackData(goodCallback)
                .build();

        InlineKeyboardButton badButton = InlineKeyboardButton.builder()
                .text("👎 별로였음")
                .callbackData(badCallback)
                .build();

        // 키보드 행 구성
        List<InlineKeyboardButton> row = List.of(goodButton, badButton);
        List<List<InlineKeyboardButton>> keyboardRows = List.of(row);

        log.info("[Telegram] Feedback keyboard created with {} buttons", row.size());

        // 키보드 Markup 생성
        return InlineKeyboardMarkup.builder()
                .keyboard(keyboardRows)
                .build();
    }

    /**
     * 빈 키보드를 생성합니다.
     *
     * @return 빈 Inline Keyboard Markup
     */
    private InlineKeyboardMarkup createEmptyKeyboard() {
        return InlineKeyboardMarkup.builder()
                .keyboard(new ArrayList<>())
                .build();
    }
}
