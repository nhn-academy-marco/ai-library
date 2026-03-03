package com.nhnacademy.library.ai.agent;

import com.nhnacademy.library.external.opennaru.client.LibraryInfoNaruApiClient;
import com.nhnacademy.library.external.opennaru.dto.BookExistInfo;
import com.nhnacademy.library.external.opennaru.dto.ExtendedPopularBooksInfo;
import com.nhnacademy.library.external.opennaru.dto.LibraryExtendedInfo;
import com.nhnacademy.library.external.opennaru.dto.UsageTrendInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 도서관 통합정보 Agent
 * <p>
 * 도서관별 통합 정보 조회 전담 Agent입니다.
 * 연령대별 인기도서, 대출반납추이, 도서 소장 여부 등을 조회합니다.
 * </p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LibraryExtendedAgent implements Agent {

    private final LibraryInfoNaruApiClient libraryInfoNaruApiClient;

    @Override
    public AgentType getType() {
        return AgentType.LIBRARY_EXTENDED;
    }

    @Override
    public String getName() {
        return "LibraryExtendedAgent";
    }

    @Override
    public String getDescription() {
        return "도서관 통합정보 조회 전담 Agent. 도서관별 연령대별 인기도서, 대출반납추이, 소장 여부를 제공합니다.";
    }

    @Override
    public String execute(AgentRequest request) {
        log.info("[{}] 도서관 통합정보 조회 실행: query={}", getName(), request.query());

        try {
            String query = request.query().toLowerCase();

            // 1. 쿼리 분석 및 API 호출 결정
            if (query.contains("연령") || query.contains("age") || query.contains("인기")) {
                // 연령대별 인기도서 조회
                return getExtendedPopularBooks(request);
            } else if (query.contains("추이") || query.contains("trend")) {
                // 대출반납추이 조회
                return getUsageTrend(request);
            } else if (query.contains("소장") || query.contains("exist")) {
                // 도서 소장 여부 조회
                return checkBookExists(request);
            } else {
                // 기본: 연령대별 인기도서 조회
                return getExtendedPopularBooks(request);
            }

        } catch (Exception e) {
            log.error("[{}] 도서관 통합정보 조회 실패: query={}", getName(), request.query(), e);
            return "도서관 통합정보 조회 중 오류가 발생했습니다.";
        }
    }

    /**
     * 연령대별 인기대출도서 통합 조회
     */
    private String getExtendedPopularBooks(AgentRequest request) {
        // 도서관 코드 추출 필요 (간단 구현을 위해 도서관명 검색 후 코드 추출)
        String query = request.query();

        // TODO: 도서관명으로 도서관 코드를 먼저 조회하는 로직 필요
        // 현재는 기본 도서관 코드 사용
        String libCode = "111004"; // 예: 강남구립도서관

        ExtendedPopularBooksInfo popularBooks = libraryInfoNaruApiClient.getExtendedPopularBooks(libCode);

        if (popularBooks == null) {
            return String.format("도서관 코드 %s의 연령대별 인기도서 정보를 찾을 수 없습니다.", libCode);
        }

        // 결과 포맷팅
        StringBuilder response = new StringBuilder();
        response.append(String.format("👶 연령대별 인기도서 (도서관 코드: %s)\n\n", libCode));

        // 전체 대출 도서
        if (!popularBooks.loanBooks().isEmpty()) {
            response.append("📚 전체 대출 Top 5\n");
            displayTopBooks(response, popularBooks.loanBooks(), 5);
            response.append("\n");
        }

        // 0~5세 인기도서
        if (!popularBooks.age0Books().isEmpty()) {
            response.append("👶 0~5세 인기도서 Top 3\n");
            displayTopBooks(response, popularBooks.age0Books(), 3);
            response.append("\n");
        }

        // 6~7세 인기도서
        if (!popularBooks.age6Books().isEmpty()) {
            response.append("🧒 6~7세 인기도서 Top 3\n");
            displayTopBooks(response, popularBooks.age6Books(), 3);
            response.append("\n");
        }

        // 8~13세 인기도서
        if (!popularBooks.age8Books().isEmpty()) {
            response.append("👦 8~13세 인기도서 Top 3\n");
            displayTopBooks(response, popularBooks.age8Books(), 3);
            response.append("\n");
        }

        // 14~19세 인기도서
        if (!popularBooks.age14Books().isEmpty()) {
            response.append("🧑‍🎓 14~19세 인기도서 Top 3\n");
            displayTopBooks(response, popularBooks.age14Books(), 3);
            response.append("\n");
        }

        // 20대 인기도서
        if (!popularBooks.age20Books().isEmpty()) {
            response.append("👨‍💼 20대 인기도서 Top 3\n");
            displayTopBooks(response, popularBooks.age20Books(), 3);
            response.append("\n");
        }

        return response.toString();
    }

    /**
     * 대출반납추이 조회
     */
    private String getUsageTrend(AgentRequest request) {
        String query = request.query();

        // TODO: 도서관 코드 추출 로직 필요
        String libCode = "111004";

        // type: week (주간), month (월간)
        String type = query.contains("월") || query.contains("month") ? "month" : "week";

        UsageTrendInfo trendInfo = libraryInfoNaruApiClient.getUsageTrend(libCode, type);

        if (trendInfo == null) {
            return String.format("도서관 코드 %s의 대출반납추이 정보를 찾을 수 없습니다.", libCode);
        }

        // 결과 포맷팅
        StringBuilder response = new StringBuilder();
        response.append(String.format("📊 대출반납추이 (%s)\n\n", type.equals("week") ? "주간" : "월간"));
        response.append(String.format("도서관: %s\n\n", trendInfo.libName()));

        if (trendInfo.loanTrend() != null) {
            var trend = trendInfo.loanTrend();

            response.append("요일별 대출 현황\n");
            response.append(String.format("월: %d회, 화: %d회, 수: %d회\n",
                    trend.mondayLoan(), trend.tuesdayLoan(), trend.wednesdayLoan()));
            response.append(String.format("목: %d회, 금: %d회, 토: %d회, 일: %d회\n",
                    trend.thursdayLoan(), trend.fridayLoan(), trend.saturdayLoan(), trend.sundayLoan()));
            response.append(String.format("\n시간대별 평균: %.1f회\n", trend.hourlyLoanAvg()));
        }

        return response.toString();
    }

    /**
     * 도서 소장 여부 조회
     */
    private String checkBookExists(AgentRequest request) {
        String query = request.query();

        // 도서관 코드와 ISBN 추출 필요
        String libCode = "111004"; // TODO: 도서관 코드 추출
        String isbn = extractIsbn(query);

        if (isbn == null || isbn.length() < 10) {
            return "ISBN을 찾을 수 없습니다.";
        }

        BookExistInfo existInfo = libraryInfoNaruApiClient.checkBookExists(libCode, isbn);

        if (existInfo == null) {
            return String.format("도서관 코드 %s에서 ISBN %s 도서의 소장 정보를 찾을 수 없습니다.", libCode, isbn);
        }

        // 결과 포맷팅
        StringBuilder response = new StringBuilder();
        response.append(String.format("📚 도서 소장 여부 확인\n\n"));
        response.append(String.format("도서관: %s\n", existInfo.libName()));
        response.append(String.format("ISBN: %s\n", existInfo.isbn13()));
        response.append(String.format("소장 여부: %s\n", existInfo.exists() ? "✅ 소장 중" : "❌ 미소장"));

        if (existInfo.exists()) {
            response.append(String.format("대출 가능: %s\n", existInfo.loanAvailable()));
            if (existInfo.callNumber() != null && !existInfo.callNumber().isBlank()) {
                response.append(String.format("청구기호: %s\n", existInfo.callNumber()));
            }
            if (existInfo.location() != null && !existInfo.location().isBlank()) {
                response.append(String.format("위치: %s\n", existInfo.location()));
            }
        }

        return response.toString();
    }

    /**
     * 공개 메서드: 도서 소장 여부 조회 (AiOrchestrationService에서 호출)
     */
    public BookExistInfo checkBookExists(String libCode, String isbn13) {
        log.info("[LibraryExtendedAgent] bookExist 호출: libCode={}, isbn={}", libCode, isbn13);
        return libraryInfoNaruApiClient.checkBookExists(libCode, isbn13);
    }

    /**
     * 상위 도서 목록 표시
     */
    private void displayTopBooks(StringBuilder response, List<?> books, int count) {
        int displayCount = Math.min(count, books.size());
        for (int i = 0; i < displayCount; i++) {
            var book = books.get(i);
            // 제목과 저자 등 필드 접근은 실제 DTO 타입에 따라 다름
            response.append(String.format("%d. %s\n", i + 1, book.toString()));
        }
    }

    /**
     * 쿼리에서 ISBN 추출
     */
    private String extractIsbn(String query) {
        String isbnPattern = "\\b\\d{10,13}\\b";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(isbnPattern);
        java.util.regex.Matcher matcher = pattern.matcher(query);

        if (matcher.find()) {
            return matcher.group();
        }
        return query.replaceAll("[^0-9X]", "");
    }
}
