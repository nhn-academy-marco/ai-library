package com.nhnacademy.library.external.telegram.handler;

import com.nhnacademy.library.external.telegram.bot.LibraryTelegramBot;
import com.nhnacademy.library.external.telegram.dto.FeedbackRequest;
import com.nhnacademy.library.external.telegram.dto.FeedbackType;
import com.nhnacademy.library.external.telegram.service.FeedbackService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Telegram Callback Query 처리 Handler
 *
 * <p>사용자가 Inline Keyboard 버튼을 클릭했을 때의 Callback Query를 처리합니다.
 */
@Slf4j
@Component
public class CallbackQueryHandler {

    private final FeedbackService feedbackService;
    private final LibraryTelegramBot libraryTelegramBot;

    public CallbackQueryHandler(FeedbackService feedbackService,
                                 @Lazy LibraryTelegramBot libraryTelegramBot) {
        this.feedbackService = feedbackService;
        this.libraryTelegramBot = libraryTelegramBot;
    }

    /**
     * Callback Query를 처리합니다.
     *
     * @param update Telegram Update 객체
     */
    public void handleCallback(Update update) {
        if (!update.hasCallbackQuery()) {
            log.warn("Update does not contain callback query");
            return;
        }

        CallbackQuery callbackQuery = update.getCallbackQuery();
        String callbackData = callbackQuery.getData();
        Long chatId = callbackQuery.getMessage().getChatId();
        String callbackQueryId = callbackQuery.getId();

        log.info("Received callback query: chatId={}, data={}", chatId, callbackData);

        try {
            // 1. Callback 데이터 파싱
            FeedbackRequest request = parseCallbackData(callbackData);

            // 2. 피드백 저장 (chatId를 함께 저장)
            feedbackService.recordFeedback(chatId, request);

            // 3. Callback Query 응답 (로딩 애니메이션 중지)
            AnswerCallbackQuery answerCallback = AnswerCallbackQuery.builder()
                    .callbackQueryId(callbackQueryId)
                    .text("✅ 피드백이 저장되었습니다!")
                    .showAlert(false)
                    .build();
            libraryTelegramBot.execute(answerCallback);

            log.info("Feedback recorded successfully: chatId={}, query={}, bookId={}, type={}",
                    chatId, request.query(), request.bookId(), request.type());

        } catch (IllegalArgumentException e) {
            log.error("Invalid callback data: chatId={}, data={}", chatId, callbackData, e);
            answerCallbackWithError(callbackQueryId, "피드백 처리 중 오류가 발생했습니다.");
        } catch (TelegramApiException e) {
            log.error("Failed to answer callback query: chatId={}, data={}", chatId, callbackData, e);
        } catch (Exception e) {
            log.error("Failed to process callback: chatId={}, data={}", chatId, callbackData, e);
            answerCallbackWithError(callbackQueryId, "피드백 처리 중 오류가 발생했습니다.");
        }
    }

    /**
     * Callback Query를 에러 메시지와 함께 응답합니다.
     *
     * @param callbackQueryId Callback Query ID
     * @param text            에러 메시지
     */
    private void answerCallbackWithError(String callbackQueryId, String text) {
        try {
            AnswerCallbackQuery answerCallback = AnswerCallbackQuery.builder()
                    .callbackQueryId(callbackQueryId)
                    .text(text)
                    .showAlert(true)
                    .build();
            libraryTelegramBot.execute(answerCallback);
        } catch (TelegramApiException e) {
            log.error("Failed to send error answer callback: {}", e.getMessage());
        }
    }

    /**
     * Callback 데이터를 파싱합니다.
     *
     * <p>데이터 포맷: feedback:{query}:{bookId}:{type}
     *
     * @param callbackData Callback 데이터
     * @return 피드백 요청
     * @throws IllegalArgumentException 데이터 포맷이 올바르지 않을 때
     */
    private FeedbackRequest parseCallbackData(String callbackData) {
        try {
            // URL 디코딩
            String decodedData = URLDecoder.decode(callbackData, StandardCharsets.UTF_8);

            // 콜론(:)으로 분리
            String[] parts = decodedData.split(":");
            if (parts.length != 4 || !parts[0].equals("feedback")) {
                throw new IllegalArgumentException("Invalid callback data format: " + callbackData);
            }

            String query = parts[1];
            Long bookId = Long.parseLong(parts[2]);
            FeedbackType type = FeedbackType.valueOf(parts[3]);

            return new FeedbackRequest(query, bookId, type);

        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse callback data: " + callbackData, e);
        }
    }
}
