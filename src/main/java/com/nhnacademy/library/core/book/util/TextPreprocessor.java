package com.nhnacademy.library.core.book.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringEscapeUtils;
import org.springframework.util.StringUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TextPreprocessor {

    private static final String HTML_TAG_PATTERN = "<[^>]*>";
    private static final String SPECIAL_CHARACTER_PATTERN = "[^\\p{L}\\p{N}\\s]";
    private static final String CONTINUOUS_SPACE_PATTERN = "\\s+";

    public static String preprocess(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }

        // 1. HTML 엔티티 디코딩 (예: &nbsp; -> 공백, &amp; -> &)
        String decoded = StringEscapeUtils.unescapeHtml4(text);

        // 2. HTML 태그 제거
        String cleaned = decoded.replaceAll(HTML_TAG_PATTERN, " ");

        // 3. 특수문자 제거 (한글, 영문, 숫자, 공백 제외)
        cleaned = cleaned.replaceAll(SPECIAL_CHARACTER_PATTERN, "");

        // 4. 공백 정규화 (연속된 공백 하나로 통합 및 trim)
        cleaned = cleaned.replaceAll(CONTINUOUS_SPACE_PATTERN, " ").trim();

        // 5. 소문자 변환
        return cleaned.toLowerCase();
    }
}
