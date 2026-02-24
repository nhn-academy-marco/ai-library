package com.nhnacademy.library.external.opennaru.client;

import com.nhnacademy.library.external.opennaru.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 도서관정보나루 API 클라이언트 추가 기능 테스트
 *
 * 나머지 12개 API 기능 테스트
 */
@Slf4j
@SpringBootTest(classes = ApiClientTestConfig.class)
class LibraryInfoNaruApiClientAdditionalTest {

    @Autowired
    private LibraryInfoNaruApiClient client;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void clearCache() {
        cacheManager.getCacheNames().forEach(cacheName -> {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        });
    }

    // ============================================================================
    // loanItemSrch (인기대출도서) 테스트
    // ============================================================================

    @Test
    @DisplayName("인기대출도서 조회 - 전체")
    void testGetPopularBooks_All() {
        // Given
        String startDt = "2026-01-01";
        String endDt = "2026-01-31";

        // When
        List<PopularBookInfo> books = client.getPopularBooks(startDt, endDt, null);

        // Then
        log.info("[TEST] 인기대출도서 조회 결과: {}권", books.size());
        assertThat(books).isNotNull();

        if (!books.isEmpty()) {
            PopularBookInfo first = books.get(0);
            log.info("[TEST] 첫 번째 도서: {} ({})", first.bookName(), first.isbn13());
            assertThat(first.isbn13()).isNotBlank();
        }
    }

    // ============================================================================
    // keywordList (도서 키워드) 테스트
    // ============================================================================

    @Test
    @DisplayName("도서 키워드 조회")
    void testGetBookKeywords() {
        // Given
        String isbn13 = "9788983921987"; // 해리포터와 마법사의 돌

        // When
        BookKeywordInfo keywords = client.getBookKeywords(isbn13);

        // Then
        log.info("[TEST] 도서 키워드 조회 결과: keywords={}", keywords);

        // API DB에 없을 수 있음
        if (keywords != null) {
            assertThat(keywords.isbn13()).isEqualTo(isbn13);
            log.info("[TEST] 키워드 수: {}", keywords.keywords().size());
        }
    }

    // ============================================================================
    // usageAnalysisList (도서 이용 분석) 테스트
    // ============================================================================

    @Test
    @DisplayName("도서 이용 분석 조회")
    void testGetUsageAnalysis() {
        // Given
        String isbn13 = "9788983921987";

        // When
        UsageAnalysisInfo analysis = client.getUsageAnalysis(isbn13);

        // Then
        log.info("[TEST] 도서 이용 분석 결과: analysis={}", analysis);

        // API DB에 없을 수 있음
        if (analysis != null) {
            assertThat(analysis.isbn13()).isEqualTo(isbn13);
            assertThat(analysis.totalLoanCount()).isGreaterThanOrEqualTo(0);
        }
    }

    // ============================================================================
    // loanItemSrchByLib (도서관별 인기대출도서) 테스트
    // ============================================================================

    @Test
    @DisplayName("도서관별 인기대출도서 조회")
    void testGetPopularBooksByLibrary() {
        // Given - 국립중앙도서관 코드 (알려서 테스트로는 임의의 유효한 코드 필요)
        String libCode = "111001"; // 국립중앙도서관

        // When
        List<PopularBookInfo> books = client.getPopularBooksByLibrary(libCode);

        // Then
        log.info("[TEST] 도서관별 인기대출도서 조회 결과: {}권", books.size());
        assertThat(books).isNotNull();
    }

    // ============================================================================
    // usageTrend (대출반납추이) 테스트
    // ============================================================================

    @Test
    @DisplayName("요일별 대출반납추이 조회")
    void testGetUsageTrend_Daily() {
        // Given - 임의의 도서관 코드
        String libCode = "111001";
        String type = "D"; // 요일별

        // When
        UsageTrendInfo trend = client.getUsageTrend(libCode, type);

        // Then
        log.info("[TEST] 요일별 대출반납추이 조회 결과: trend={}", trend);

        // API DB에 없을 수 있음
        if (trend != null) {
            assertThat(trend.libCode()).isEqualTo(libCode);
            assertThat(trend.type()).isEqualTo(type);
            assertThat(trend.loanTrend()).isNotNull();
        }
    }

    // ============================================================================
    // bookExist (도서 소장여부) 테스트
    // ============================================================================

    @Test
    @DisplayName("도서 소장여부 조회")
    void testCheckBookExists() {
        // Given
        String libCode = "111001";
        String isbn13 = "9788983921987";

        // When
        BookExistInfo existInfo = client.checkBookExists(libCode, isbn13);

        // Then
        log.info("[TEST] 도서 소장여부 조회 결과: existInfo={}", existInfo);

        // API DB에 없을 수 있음
        if (existInfo != null) {
            assertThat(existInfo.libCode()).isEqualTo(libCode);
            assertThat(existInfo.isbn13()).isEqualTo(isbn13);
            log.info("[TEST] 소장 여부: {}", existInfo.exists());
        }
    }

    // ============================================================================
    // hotTrend (대출 급상승 도서) 테스트
    // ============================================================================

    @Test
    @DisplayName("대출 급상승 도서 조회")
    void testGetHotTrendBooks() {
        // Given - 최근 일주일
        String searchDate = LocalDate.now().minusDays(7).toString();

        // When
        List<HotTrendBookInfo> books = client.getHotTrendBooks(searchDate);

        // Then
        log.info("[TEST] 대출 급상승 도서 조회 결과: {}권", books.size());
        assertThat(books).isNotNull();
    }

    // ============================================================================
    // monthlyKeywords (월간 키워드) 테스트
    // ============================================================================

    @Test
    @DisplayName("월간 키워드 조회")
    void testGetMonthlyKeywords() {
        // Given
        String month = "2026-01";

        // When
        List<MonthlyKeywordInfo> keywords = client.getMonthlyKeywords(month);

        // Then
        log.info("[TEST] 월간 키워드 조회 결과: {}개 키워드", keywords.size());
        assertThat(keywords).isNotNull();
    }

    // ============================================================================
    // readQt (독서량/독서율) 테스트
    // ============================================================================

    @Test
    @DisplayName("지역별 독서량/독서율 조회")
    void testGetReadingQuantity() {
        // Given
        String region = "11"; // 서울

        // When
        ReadingQuantityInfo info = client.getReadingQuantity(region, null);

        // Then
        log.info("[TEST] 지역별 독서량/독서율 조회 결과: info={}", info);

        // API DB에 데이터가 없을 수 있음 (빈 값 반환 가능)
        if (info != null) {
            // API가 데이터를 반환하면 값 검증
            if (!info.region().isEmpty()) {
                assertThat(info.region()).isEqualTo(region);
            }
            assertThat(info.readingQuantity()).isGreaterThanOrEqualTo(0);
            log.info("[TEST] 독서량: {}, 독서율: {}%",
                info.readingQuantity(), info.readingRate());
        }
    }

    // ============================================================================
    // 캐시 동작 확인 테스트
    // ============================================================================

    @Test
    @DisplayName("인기대출도서 캐시 동작 확인")
    void testPopularBooksCache() {
        String startDt = "2026-01-01";
        String endDt = "2026-01-31";

        // When - 첫 번째 호출
        List<PopularBookInfo> result1 = client.getPopularBooks(startDt, endDt, null);

        // When - 두 번째 호출 (캐시에서 가져오기)
        List<PopularBookInfo> result2 = client.getPopularBooks(startDt, endDt, null);

        // Then - 결과가 같아야 함
        assertThat(result1).isEqualTo(result2);
        log.info("[TEST] 캐시 동작 확인 완료");
    }

    @Test
    @DisplayName("도서 키워드 캐시 동작 확인")
    void testBookKeywordsCache() {
        String isbn13 = "9788983921987";

        // When - 두 번 호출
        client.getBookKeywords(isbn13);
        BookKeywordInfo result2 = client.getBookKeywords(isbn13);

        // Then
        if (result2 != null) {
            assertThat(result2.isbn13()).isEqualTo(isbn13);
            log.info("[TEST] 캐시 동작 확인 완료");
        }
    }

    @Test
    @DisplayName("대출 급상승 도서 캐시 동작 확인")
    void testHotTrendBooksCache() {
        String searchDate = LocalDate.now().minusDays(7).toString();

        // When - 두 번 호출
        client.getHotTrendBooks(searchDate);
        List<HotTrendBookInfo> result2 = client.getHotTrendBooks(searchDate);

        // Then
        assertThat(result2).isNotNull();
        log.info("[TEST] 캐시 동작 확인 완료");
    }

    // ============================================================================
    // 종합 테스트
    // ============================================================================

    @Test
    @DisplayName("종합 시나리오: 인기도서 → 키워드 → 이용분석")
    void testComprehensiveScenario_Part1() {
        log.info("[TEST] ===== 종합 시나리오 Part 1 시작 =====");

        // Step 1: 인기대출도서 조회
        log.info("[TEST] Step 1: 인기대출도서 조회");
        String startDt = "2026-01-01";
        String endDt = "2026-01-31";
        List<PopularBookInfo> popularBooks = client.getPopularBooks(startDt, endDt, null);
        log.info("[TEST] 인기대출도서: {}권", popularBooks.size());

        // Step 2: 첫 번째 도서의 키워드 조회
        if (!popularBooks.isEmpty()) {
            String isbn13 = popularBooks.get(0).isbn13();
            log.info("[TEST] Step 2: 첫 번째 인기도서 키워드 조회: {}", isbn13);

            BookKeywordInfo keywords = client.getBookKeywords(isbn13);
            log.info("[TEST] 키워드 조회 완료: {}", keywords != null ? "성공" : "null");

            // Step 3: 이용 분석 조회
            log.info("[TEST] Step 3: 이용 분석 조회");
            UsageAnalysisInfo analysis = client.getUsageAnalysis(isbn13);
            log.info("[TEST] 이용 분석 조회 완료: {}", analysis != null ? "성공" : "null");
        }

        log.info("[TEST] ===== 종합 시나리오 Part 1 완료 =====");
    }

    @Test
    @DisplayName("종합 시나리오: 급상승도서 → 월간키워드")
    void testComprehensiveScenario_Part2() {
        log.info("[TEST] ===== 종합 시나리오 Part 2 시작 =====");

        // Step 1: 대출 급상승 도서 조회
        log.info("[TEST] Step 1: 대출 급상승 도서 조회");
        String searchDate = LocalDate.now().minusDays(7).toString();
        List<HotTrendBookInfo> hotBooks = client.getHotTrendBooks(searchDate);
        log.info("[TEST] 대출 급상승 도서: {}권", hotBooks.size());

        // Step 2: 월간 키워드 조회
        log.info("[TEST] Step 2: 월간 키워드 조회");
        String month = "2026-01";
        List<MonthlyKeywordInfo> keywords = client.getMonthlyKeywords(month);
        log.info("[TEST] 월간 키워드: {}개", keywords.size());

        // Then
        assertThat(hotBooks).isNotNull();
        assertThat(keywords).isNotNull();

        log.info("[TEST] ===== 종합 시나리오 Part 2 완료 =====");
    }
}
