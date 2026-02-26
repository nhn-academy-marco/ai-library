package com.nhnacademy.library.external.telegram.util;

import com.nhnacademy.library.external.telegram.entity.SearchFeedback;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * CSV 내보내기 서비스
 *
 * <p>피드백 데이터를 CSV 형식으로 변환합니다.
 */
@Slf4j
@Service
public class CsvExportService {

    private static final String CSV_HEADER = "ID,ChatId,Query,BookId,Type,CreatedAt";
    private static final String LINE_SEPARATOR = "\n";
    private static final String VALUE_SEPARATOR = ",";

    /**
     * 피드백 목록을 CSV 형식으로 변환합니다.
     *
     * @param feedbacks 피드백 목록
     * @return CSV 문자열
     */
    public String generateCsv(List<SearchFeedback> feedbacks) {
        log.debug("Generating CSV for {} feedbacks", feedbacks.size());

        StringBuilder csv = new StringBuilder();

        // 1. 헤더 추가
        csv.append(CSV_HEADER).append(LINE_SEPARATOR);

        // 2. 데이터 행 추가
        for (SearchFeedback feedback : feedbacks) {
            csv.append(escapeCsvValue(feedback.getId().toString())).append(VALUE_SEPARATOR);
            csv.append(escapeCsvValue(feedback.getChatId().toString())).append(VALUE_SEPARATOR);
            csv.append(escapeCsvValue(feedback.getQuery())).append(VALUE_SEPARATOR);
            csv.append(escapeCsvValue(feedback.getBookId() != null ? feedback.getBookId().toString() : "")).append(VALUE_SEPARATOR);
            csv.append(escapeCsvValue(feedback.getType().name())).append(VALUE_SEPARATOR);
            csv.append(escapeCsvValue(feedback.getCreatedAt().toString()));
            csv.append(LINE_SEPARATOR);
        }

        log.info("CSV generated successfully: {} rows", feedbacks.size() + 1); // +1 for header
        return csv.toString();
    }

    /**
     * CSV 값에 포함된 특수문자를 이스케이프 처리합니다.
     *
     * <p>콤마(,), 큰따옴표("), 개행문자가 포함된 경우:
     * <ul>
     *   <li>전체 값을 큰따옴표로 감쌉니다</li>
     *   <li>내부의 큰따옴표는 두 개("")로 이스케이프합니다</li>
     * </ul>
     *
     * @param value 원본 값
     * @return 이스케이프 처리된 값
     */
    private String escapeCsvValue(String value) {
        if (value == null) {
            return "";
        }

        // 특수문자 포함 여부 확인
        boolean needsEscape = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r");

        if (needsEscape) {
            // 큰따옴표로 감싸고, 내부의 큰따옴표는 두 개로 이스케이프
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }

        return value;
    }
}
