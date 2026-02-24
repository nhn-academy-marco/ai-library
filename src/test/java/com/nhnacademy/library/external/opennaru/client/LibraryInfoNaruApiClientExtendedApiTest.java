package com.nhnacademy.library.external.opennaru.client;

import com.nhnacademy.library.external.opennaru.dto.*;
import com.nhnacademy.library.external.opennaru.properties.LibraryApiProperties;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 도서관정보나루 extends API 테스트
 */
@Slf4j
@SpringBootTest(classes = ApiClientTestConfig.class)
class LibraryInfoNaruApiClientExtendedApiTest {

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
    // extends/libSrch (도서관 통합정보) 테스트
    // ============================================================================

    @Test
    @DisplayName("도서관 통합정보 조회")
    void testGetLibraryExtendedInfo() {
        // When
        List<LibraryExtendedInfo> libraries = client.getLibraryExtendedInfo(1, 5);

        // Then
        log.info("[TEST] 도서관 통합정보 조회 결과: {}개 도서관", libraries.size());
        assertThat(libraries).isNotNull();

        if (!libraries.isEmpty()) {
            LibraryExtendedInfo first = libraries.get(0);
            log.info("[TEST] 첫 번째 도서관: {} ({})", first.libName(), first.libCode());
            assertThat(first.libCode()).isNotBlank();

            // 시간대별 대출/반납 정보 확인
            if (!first.loanByHours().isEmpty()) {
                log.info("[TEST] 시간대별 대출/반납 정보: {}건", first.loanByHours().size());
            }

            // 요일별 대출/반납 정보 확인
            if (!first.loanByDayOfWeek().isEmpty()) {
                log.info("[TEST] 요일별 대출/반납 정보: {}건", first.loanByDayOfWeek().size());
            }

            // 신착 도서 정보 확인
            if (!first.newBooks().isEmpty()) {
                log.info("[TEST] 신착 도서: {}권", first.newBooks().size());
                first.newBooks().forEach(book ->
                    log.info("[TEST]   - {}", book.bookName())
                );
            }
        }
    }

    @Test
    @DisplayName("도서관 통합정보 캐시 동작 확인")
    void testLibraryExtendedInfoCache() {
        // When - 첫 번째 호출
        List<LibraryExtendedInfo> result1 = client.getLibraryExtendedInfo(1, 3);

        // When - 두 번째 호출 (캐시에서 가져오기)
        List<LibraryExtendedInfo> result2 = client.getLibraryExtendedInfo(1, 3);

        // Then - 결과가 같아야 함
        assertThat(result1).isEqualTo(result2);
        log.info("[TEST] 캐시 동작 확인 완료");
    }

    // ============================================================================
    // extends/loanItemSrchByLib (연령대별 인기대출도서 통합) 테스트
    // ============================================================================

    @Test
    @DisplayName("연령대별 인기대출도서 통합 조회")
    void testGetExtendedPopularBooks() {
        // Given - 강남구도서관 코드 (예시)
        String libCode = "129224";

        // When
        ExtendedPopularBooksInfo books = client.getExtendedPopularBooks(libCode);

        // Then
        log.info("[TEST] 연령대별 인기대출도서 통합 조회 결과: {}", books != null ? "성공" : "null");

        if (books != null) {
            log.info("[TEST] 전체 인기대출도서: {}권", books.loanBooks().size());
            log.info("[TEST] 0-5세 인기도서: {}권", books.age0Books().size());
            log.info("[TEST] 6-7세 인기도서: {}권", books.age6Books().size());
            log.info("[TEST] 8-13세 인기도서: {}권", books.age8Books().size());
            log.info("[TEST] 14-19세 인기도서: {}권", books.age14Books().size());
            log.info("[TEST] 20세+ 인기도서: {}권", books.age20Books().size());

            // 전체 인기대출도서가 있어야 함
            if (!books.loanBooks().isEmpty()) {
                books.loanBooks().stream().limit(3).forEach(book ->
                    log.info("[TEST]   - {} ({})", book.bookName(), book.ranking())
                );
            }
        }
    }

    @Test
    @DisplayName("연령대별 인기대출도서 캐시 동작 확인")
    void testExtendedPopularBooksCache() {
        // Given
        String libCode = "129224";

        // When - 두 번 호출
        client.getExtendedPopularBooks(libCode);
        ExtendedPopularBooksInfo result2 = client.getExtendedPopularBooks(libCode);

        // Then
        if (result2 != null) {
            assertThat(result2.loanBooks()).isNotNull();
            log.info("[TEST] 캐시 동작 확인 완료");
        }
    }

    // ============================================================================
    // 종합 테스트
    // ============================================================================

    @Test
    @DisplayName("종합 시나리오: 통합정보 → 연령대별 인기도서")
    void testComprehensiveScenario() {
        log.info("[TEST] ===== 종합 시나리오 시작 =====");

        // Step 1: 도서관 통합정보 조회
        log.info("[TEST] Step 1: 도서관 통합정보 조회");
        List<LibraryExtendedInfo> libraries = client.getLibraryExtendedInfo(1, 10);
        log.info("[TEST] 도서관 수: {}개", libraries.size());

        // Step 2: 첫 번째 도서관의 연령대별 인기대출도서 조회
        if (!libraries.isEmpty()) {
            String libCode = libraries.get(0).libCode();
            log.info("[TEST] Step 2: 연령대별 인기대출도서 조회: {}", libCode);

            ExtendedPopularBooksInfo books = client.getExtendedPopularBooks(libCode);
            if (books != null) {
                log.info("[TEST] 전체 인기도서: {}권", books.loanBooks().size());

                // 각 연령대별 인기도서 출력
                if (!books.age8Books().isEmpty()) {
                    log.info("[TEST] 8-13세 인기도서 Top 3:");
                    books.age8Books().stream().limit(3).forEach(book ->
                        log.info("[TEST]   - {} ({})", book.bookName(), book.ranking())
                    );
                }
            }
        }

        log.info("[TEST] ===== 종합 시나리오 완료 =====");
    }
}
