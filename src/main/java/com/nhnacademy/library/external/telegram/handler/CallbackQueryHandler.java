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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    // 채팅별 최근 검색어 저장 (콜백 데이터에 검색어를 포함할 수 없을 때 사용)
    private final Map<Long, String> recentQueries = new ConcurrentHashMap<>();

    public CallbackQueryHandler(FeedbackService feedbackService,
                                 @Lazy LibraryTelegramBot libraryTelegramBot) {
        this.feedbackService = feedbackService;
        this.libraryTelegramBot = libraryTelegramBot;
    }

    /**
     * 최근 검색어를 저장합니다.
     *
     * @param chatId Telegram 사용자 ID
     * @param query  검색어
     */
    public void setRecentQuery(Long chatId, String query) {
        recentQueries.put(chatId, query);
        log.debug("[Telegram] Stored recent query for chatId: {}", chatId, query);
    }

    /**
     * 최근 검색어를 가져옵니다.
     *
     * @param chatId Telegram 사용자 ID
     * @return 최근 검색어 (없으면 null)
     */
    public String getRecentQuery(Long chatId) {
        return recentQueries.get(chatId);
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

            // 2. 저장된 최근 검색어 가져오기
            String query = recentQueries.get(chatId);
            if (query == null || query.isBlank()) {
                log.warn("[Telegram] No recent query found for chatId: {}, using empty string", chatId);
                query = "";
            }

            // 3. FeedbackRequest에 검색어 포함하여 재생성
            FeedbackRequest requestWithQuery = new FeedbackRequest(query, request.bookId(), request.type());

            // 4. 중복 피드백 체크
            if (feedbackService.hasExistingFeedback(chatId, query, requestWithQuery.bookId())) {
                log.info("Duplicate feedback detected: chatId={}, query={}, bookId={}",
                         chatId, query, requestWithQuery.bookId());

                // 이미 피드백이 있음을 알림
                AnswerCallbackQuery answerCallback = AnswerCallbackQuery.builder()
                        .callbackQueryId(callbackQueryId)
                        .text("⚠️ 이미 피드백을 남기셨습니다.")
                        .showAlert(false)
                        .build();
                libraryTelegramBot.execute(answerCallback);
                return;
            }

            // 5. 피드백 저장 (chatId를 함께 저장)
            feedbackService.recordFeedback(chatId, requestWithQuery);

            // 6. Callback Query 응답 (로딩 애니메이션 중지)
            AnswerCallbackQuery answerCallback = AnswerCallbackQuery.builder()
                    .callbackQueryId(callbackQueryId)
                    .text("✅ 피드백이 저장되었습니다!")
                    .showAlert(false)
                    .build();
            libraryTelegramBot.execute(answerCallback);

            log.info("Feedback recorded successfully: chatId={}, query={}, bookId={}, type={}",
                    chatId, query, requestWithQuery.bookId(), requestWithQuery.type());

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
     * <p>데이터 포맷: fb:{bookId}:{type}
     * <p>Telegram API 제한으로 인해 검색어는 포함하지 않습니다 (callback_data 최대 64 bytes)
     *
     * @param callbackData Callback 데이터
     * @return 피드백 요청
     * @throws IllegalArgumentException 데이터 포맷이 올바르지 않을 때
     */
    private FeedbackRequest parseCallbackData(String callbackData) {
        try {
            // 콜론(:)으로 분리
            String[] parts = callbackData.split(":");
            if (parts.length != 3 || !parts[0].equals("fb")) {
                throw new IllegalArgumentException("Invalid callback data format: " + callbackData);
            }

            // 검색어는 포함하지 않음 (Telegram API 제한: callback_data 최대 64 bytes)
            String query = ""; // 빈 문자열로 저장
            Long bookId = Long.parseLong(parts[1]);
            FeedbackType type = FeedbackType.valueOf(parts[2]);

            return new FeedbackRequest(query, bookId, type);

        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse callback data: " + callbackData, e);
        }
    }
}
