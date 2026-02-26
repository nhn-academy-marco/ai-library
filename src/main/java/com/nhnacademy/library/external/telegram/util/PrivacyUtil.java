package com.nhnacademy.library.external.telegram.util;

import lombok.extern.slf4j.Slf4j;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * 프라이버시 보호를 위한 유틸리티 클래스
 *
 * <p>사용자 식별자(chatId)를 해싱하여 개인정보를 보호합니다.
 */
@Slf4j
public class PrivacyUtil {

    private static final String SALT = "ai-library-telegram-salt-2025";

    /**
     * chatId를 해싱합니다.
     *
     * <p>SHA-256 해싱 알고리즘을 사용하며, 솔트(Salt)를 추가하여
     * 레인보우 테이블 공격을 방지합니다.
     *
     * @param chatId Telegram 사용자 ID
     * @return 해싱된 chatId (Base64 인코딩)
     */
    public static String hashChatId(Long chatId) {
        if (chatId == null) {
            return null;
        }

        try {
            String input = chatId + SALT;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);

        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to hash chatId: {}", e.getMessage());
            throw new RuntimeException("해싱 알고리즘을 찾을 수 없습니다.", e);
        }
    }

    /**
     * 해싱된 chatId를 마스킹된 형태로 변환합니다 (로그용).
     *
     * @param chatId Telegram 사용자 ID
     * @return 마스킹된 chatId (예: 123456***)
     */
    public static String maskChatId(Long chatId) {
        if (chatId == null) {
            return "null";
        }
        String chatIdStr = chatId.toString();
        if (chatIdStr.length() <= 3) {
            return "***";
        }
        return chatIdStr.substring(0, Math.min(3, chatIdStr.length())) + "***";
    }

    /**
     * 해싱된 chatId의 원본 여부를 확인합니다 (불가능).
     *
     * <p>단방향 해싱이므로 원본 chatId를 복구할 수 없습니다.
     *
     * @param hashedChatId 해싱된 chatId
     * @return 항상 false (원본 복구 불가)
     */
    public static boolean canRecoverOriginal(String hashedChatId) {
        return false; // 단방향 해싱이므로 복구 불가
    }
}
