package com.nhnacademy.library.ai.agent;

import com.nhnacademy.library.external.opennaru.client.LibraryInfoNaruApiClient;
import com.nhnacademy.library.external.opennaru.dto.HotTrendBookInfo;
import com.nhnacademy.library.external.opennaru.dto.MonthlyKeywordInfo;
import com.nhnacademy.library.external.opennaru.dto.ReadingQuantityInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 트렌드 Agent
 * <p>
 * 도서관 트렌드 조회 전담 Agent입니다.
 * 대출 급상승 도서, 월간 키워드, 독서량/독서율 등 트렌드 정보를 조회합니다.
 * </p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TrendAgent implements Agent {

    private final LibraryInfoNaruApiClient libraryInfoNaruApiClient;

    @Override
    public AgentType getType() {
        return AgentType.TREND;
    }

    @Override
    public String getName() {
        return "TrendAgent";
    }

    @Override
    public String getDescription() {
        return "트렌드 조회 전담 Agent. 대출 급상승 도서, 월간 키워드, 독서량/독서율 트렌드를 제공합니다.";
    }

    @Override
    public String execute(AgentRequest request) {
        log.info("[{}] 트렌드 조회 실행: query={}", getName(), request.query());

        try {
            String query = request.query().toLowerCase();

            // 1. 쿼리 분석 및 API 호출 결정
            if (query.contains("급상승") || query.contains("hot") || query.contains("핫")) {
                // 대출 급상승 도서 조회
                return getHotTrendBooks(request);
            } else if (query.contains("키워드") || query.contains("keyword")) {
                // 월간 키워드 조회
                return getMonthlyKeywords(request);
            } else if (query.contains("독서량") || query.contains("독서율") || query.contains("reading")) {
                // 독서량/독서율 조회
                return getReadingQuantity(request);
            } else {
                // 기본: 대출 급상승 도서 조회
                return getHotTrendBooks(request);
            }

        } catch (Exception e) {
            log.error("[{}] 트렌드 조회 실패: query={}", getName(), request.query(), e);
            return "트렌드 조회 중 오류가 발생했습니다.";
        }
    }

    /**
     * 대출 급상승 도서 조회
     */
    private String getHotTrendBooks(AgentRequest request) {
        // 기본: 오늘 날짜
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

        List<HotTrendBookInfo> hotTrendBooks = libraryInfoNaruApiClient.getHotTrendBooks(today.format(formatter));

        if (hotTrendBooks.isEmpty()) {
            return "대출 급상승 도서 정보를 찾을 수 없습니다.";
        }

        // 결과 포맷팅 (전체 표시)
        StringBuilder response = new StringBuilder();
        response.append("📈 대출 급상승 도서\n\n");

        for (int i = 0; i < hotTrendBooks.size(); i++) {
            HotTrendBookInfo book = hotTrendBooks.get(i);
            response.append(String.format("%d. %s (현재 순위: %d)\n",
                    i + 1, book.bookName(), book.currentRank()));
            response.append(String.format("   저자: %s\n", book.authors()));
            response.append(String.format("   출판사: %s\n", book.publisher()));

            // 순위 변화 표시
            if (book.rankChange() > 0) {
                response.append(String.format("   순위 상승: +%d\n", book.rankChange()));
            } else if (book.rankChange() < 0) {
                response.append(String.format("   순위 하락: %d\n", book.rankChange()));
            }

            response.append("\n");
        }

        return response.toString();
    }

    /**
     * 월간 키워드 조회
     */
    private String getMonthlyKeywords(AgentRequest request) {
        // 기본: 현재 월
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMM");

        String month = today.format(formatter);

        List<MonthlyKeywordInfo> keywords = libraryInfoNaruApiClient.getMonthlyKeywords(month);

        if (keywords.isEmpty()) {
            return String.format("%s월의 키워드 정보를 찾을 수 없습니다.", month);
        }

        // 결과 포맷팅
        StringBuilder response = new StringBuilder();
        response.append(String.format("🔑 %s월 인기 키워드\n\n", month));

        int displayCount = Math.min(10, keywords.size());
        for (int i = 0; i < displayCount; i++) {
            MonthlyKeywordInfo keyword = keywords.get(i);
            response.append(String.format("%d. %s (점수: %d)\n",
                    i + 1, keyword.keyword(), keyword.score()));
        }

        return response.toString();
    }

    /**
     * 독서량/독서율 조회
     */
    private String getReadingQuantity(AgentRequest request) {
        String query = request.query();

        // 지역 추출
        String region = extractRegionCode(query);
        String regionName = extractRegionName(query);

        ReadingQuantityInfo readingInfo = libraryInfoNaruApiClient.getReadingQuantity(region, null);

        if (readingInfo == null) {
            return String.format("'%s' 지역의 독서량/독서율 정보를 찾을 수 없습니다.", regionName);
        }

        // 결과 포맷팅
        StringBuilder response = new StringBuilder();
        response.append(String.format("📊 %s 독서량/독서율\n\n", readingInfo.regionName()));
        response.append(String.format("지역: %s\n", readingInfo.regionName()));
        response.append(String.format("독서량: %.1f권\n", readingInfo.readingQuantity()));
        response.append(String.format("독서율: %.1f%%\n", readingInfo.readingRate() * 100));

        return response.toString();
    }

    /**
     * 쿼리에서 지역 코드 추출
     */
    private String extractRegionCode(String query) {
        if (query.contains("서울")) return "11";
        if (query.contains("부산")) return "21";
        if (query.contains("대구")) return "22";
        if (query.contains("인천")) return "23";
        if (query.contains("광주")) return "24";
        if (query.contains("대전")) return "25";
        if (query.contains("울산")) return "26";
        if (query.contains("세종")) return "29";
        if (query.contains("경기")) return "41";
        if (query.contains("강원")) return "42";
        if (query.contains("충북")) return "43";
        if (query.contains("충남")) return "44";
        if (query.contains("전북")) return "45";
        if (query.contains("전남")) return "46";
        if (query.contains("경북")) return "47";
        if (query.contains("경남")) return "48";
        if (query.contains("제주")) return "50";
        return "11"; // 기본: 서울
    }

    /**
     * 쿼리에서 지역명 추출
     */
    private String extractRegionName(String query) {
        if (query.contains("서울")) return "서울";
        if (query.contains("부산")) return "부산";
        if (query.contains("대구")) return "대구";
        if (query.contains("인천")) return "인천";
        if (query.contains("광주")) return "광주";
        if (query.contains("대전")) return "대전";
        if (query.contains("울산")) return "울산";
        if (query.contains("세종")) return "세종";
        if (query.contains("경기")) return "경기";
        if (query.contains("강원")) return "강원";
        if (query.contains("충북")) return "충북";
        if (query.contains("충남")) return "충남";
        if (query.contains("전북")) return "전북";
        if (query.contains("전남")) return "전남";
        if (query.contains("경북")) return "경북";
        if (query.contains("경남")) return "경남";
        if (query.contains("제주")) return "제주";
        return "서울"; // 기본: 서울
    }
}
