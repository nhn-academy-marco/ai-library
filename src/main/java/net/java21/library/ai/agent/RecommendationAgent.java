package net.java21.library.ai.agent;

import net.java21.library.external.opennaru.client.LibraryInfoNaruApiClient;
import net.java21.library.external.opennaru.dto.PopularBookInfo;
import net.java21.library.external.opennaru.dto.RecommendedBookInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 추천 Agent
 * <p>
 * 도서 추천 전담 Agent입니다.
 * 인기 대출도서, 마니아/다독자 추천, 연령대별 인기도서 등을 조회합니다.
 * </p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RecommendationAgent implements Agent {

    private final LibraryInfoNaruApiClient libraryInfoNaruApiClient;

    @Override
    public AgentType getType() {
        return AgentType.RECOMMENDATION;
    }

    @Override
    public String getName() {
        return "RecommendationAgent";
    }

    @Override
    public String getDescription() {
        return "도서 추천 전담 Agent. 인기 대출도서, 마니아/다독자 추천 정보를 제공합니다.";
    }

    @Override
    public String execute(AgentRequest request) {
        log.info("[{}] 도서 추천 실행: query={}", getName(), request.query());

        try {
            String query = request.query().toLowerCase();

            // 1. 쿼리 분석 및 API 호출 결정
            if (query.contains("인기") || query.contains("popular") || query.contains("bestseller")) {
                // 인기 대출도서 조회
                return getPopularBooks(request);
            } else if (query.contains("마니아") || query.contains("mania")) {
                // 마니아 추천
                return getManiaRecommendations(request);
            } else if (query.contains("다독") || query.contains("reader")) {
                // 다독자 추천
                return getReaderRecommendations(request);
            } else {
                // 기본: 인기 도서 조회
                return getPopularBooks(request);
            }

        } catch (Exception e) {
            log.error("[{}] 도서 추천 실패: query={}", getName(), request.query(), e);
            return "도서 추천 조회 중 오류가 발생했습니다.";
        }
    }

    /**
     * 인기 대출도서 조회
     */
    private String getPopularBooks(AgentRequest request) {
        String query = request.query();

        // 날짜 범위 설정 (기본: 최근 1개월)
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

        // 지역 추출
        String region = null;
        if (query.contains("지역") || query.contains("region")) {
            region = extractRegion(query);
        }

        List<PopularBookInfo> popularBooks = libraryInfoNaruApiClient.getPopularBooks(
                startDate.format(formatter),
                endDate.format(formatter),
                region
        );

        if (popularBooks.isEmpty()) {
            return "인기 대출도서 정보를 찾을 수 없습니다.";
        }

        // 결과 포맷팅 (전체 표시)
        StringBuilder response = new StringBuilder();
        response.append("🔥 인기 대출도서\n\n");

        for (int i = 0; i < popularBooks.size(); i++) {
            PopularBookInfo book = popularBooks.get(i);
            response.append(String.format("%d. %s\n", i + 1, book.bookName()));
            response.append(String.format("   저자: %s\n", book.authors()));
            response.append(String.format("   출판사: %s\n", book.publisher()));
            if (book.loanCount() > 0) {
                response.append(String.format("   대출 횟수: %d회\n", book.loanCount()));
            }
            response.append("\n");
        }

        return response.toString();
    }

    /**
     * 마니아 추천도서 조회
     */
    private String getManiaRecommendations(AgentRequest request) {
        String isbn = extractIsbn(request.query());

        if (isbn == null || isbn.length() < 10) {
            return "ISBN을 찾을 수 없습니다. 추천받을 도서의 ISBN을 입력해주세요.";
        }

        List<RecommendedBookInfo> recommendations = libraryInfoNaruApiClient.getRecommendedBooksForMania(isbn);

        if (recommendations.isEmpty()) {
            return String.format("ISBN %s 도서의 마니아 추천 정보를 찾을 수 없습니다.", isbn);
        }

        return formatRecommendations("마니아 추천도서", recommendations);
    }

    /**
     * 다독자 추천도서 조회
     */
    private String getReaderRecommendations(AgentRequest request) {
        String isbn = extractIsbn(request.query());

        if (isbn == null || isbn.length() < 10) {
            return "ISBN을 찾을 수 없습니다. 추천받을 도서의 ISBN을 입력해주세요.";
        }

        List<RecommendedBookInfo> recommendations = libraryInfoNaruApiClient.getRecommendedBooksForReader(isbn);

        if (recommendations.isEmpty()) {
            return String.format("ISBN %s 도서의 다독자 추천 정보를 찾을 수 없습니다.", isbn);
        }

        return formatRecommendations("다독자 추천도서", recommendations);
    }

    /**
     * 추천도서 목록 포맷팅 (전체 표시)
     */
    private String formatRecommendations(String title, List<RecommendedBookInfo> recommendations) {
        StringBuilder response = new StringBuilder();
        response.append(String.format("📚 %s\n\n", title));

        for (int i = 0; i < recommendations.size(); i++) {
            RecommendedBookInfo book = recommendations.get(i);
            response.append(String.format("%d. %s\n", i + 1, book.bookName()));
            response.append(String.format("   저자: %s\n", book.authors()));
            response.append(String.format("   출판사: %s\n", book.publisher()));
            response.append(String.format("   ISBN: %s\n", book.isbn13()));
            response.append("\n");
        }

        return response.toString();
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

    /**
     * 쿼리에서 지역 추출
     */
    private String extractRegion(String query) {
        // 지역명 추출 로직 (간단 구현)
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
        return null;
    }
}
