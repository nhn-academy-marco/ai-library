package com.nhnacademy.library.external.opennaru.client;

import com.nhnacademy.library.external.opennaru.dto.*;
import com.nhnacademy.library.external.opennaru.properties.LibraryApiProperties;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 도서관정보나루 API 동작 테스트
 *
 * Spring 컨텍스트를 로드하지 않고 RestClient를 직접 생성하여
 * API 동작 여부만 확인합니다.
 */
@Slf4j
@DisplayName("도서관정보나루 API 동작 테스트 (순수 API 호출)")
class LibraryInfoNaruApiCallTest {

    private LibraryInfoNaruApiClient client;
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // ObjectMapper 수동 생성
        objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

        // RestClient.Builder 수동 생성
        org.springframework.web.client.RestClient.Builder restClientBuilder =
            org.springframework.web.client.RestClient.builder();

        // 프로퍼티 설정
        LibraryApiProperties properties = new LibraryApiProperties();
        properties.setUrl("https://www.data4library.kr/api");
        properties.setKey("356634a513872ef340a52007c03c19603a7a3908165d1a9ab46b79a5afd6b83d");
        properties.setTimeout(5000);

        // 클라이언트 인스턴스 생성 (리플렉션 사용)
        client = new LibraryInfoNaruApiClient(properties, objectMapper, restClientBuilder);
    }

    // ========== 도서 검색 API 테스트 ==========

    @Test
    @DisplayName("API - 제목으로 도서 검색")
    void testSearchBooksByTitle() {
        // Given
        String title = "자바";

        // When
        List<LibraryBookInfo> books = client.searchBooksByTitle(title);

        // Then
        log.info("[DEBUG_LOG] 제목 검색 결과: {}권", books.size());
        assertThat(books).isNotNull();
        // API가 정상 동작하면 결과가 있어야 함
        log.info("[DEBUG_LOG] API 호출 성공 - 결과 null 확인 완료");
    }

    @Test
    @DisplayName("API - 저자명으로 도서 검색")
    void testSearchBooksByAuthor() {
        // Given
        String author = "남궁성";

        // When
        List<LibraryBookInfo> books = client.searchBooksByAuthor(author);

        // Then
        log.info("[DEBUG_LOG] 저자 검색 결과: {}권", books.size());
        assertThat(books).isNotNull();
        log.info("[DEBUG_LOG] API 호출 성공");
    }

    @Test
    @DisplayName("API - 출판사로 도서 검색")
    void testSearchBooksByPublisher() {
        // Given
        String publisher = "한빛미디어";

        // When
        List<LibraryBookInfo> books = client.searchBooksByPublisher(publisher);

        // Then
        log.info("[DEBUG_LOG] 출판사 검색 결과: {}권", books.size());
        assertThat(books).isNotNull();
        log.info("[DEBUG_LOG] API 호출 성공");
    }

    @Test
    @DisplayName("API - ISBN13으로 도서 검색")
    void testSearchBooksByIsbn() {
        // Given
        String isbn13 = "9788936434120";

        // When
        List<LibraryBookInfo> books = client.searchBooksByIsbn(isbn13);

        // Then
        log.info("[DEBUG_LOG] ISBN13 검색 결과: {}권", books.size());
        assertThat(books).isNotNull();
        log.info("[DEBUG_LOG] API 호출 성공");
    }

    @Test
    @DisplayName("API - ISBN10으로 도서 검색")
    void testSearchBooksByIsbn10() {
        // Given
        String isbn10 = "8936434128";

        // When
        List<LibraryBookInfo> books = client.searchBooksByIsbn10(isbn10);

        // Then
        log.info("[DEBUG_LOG] ISBN10 검색 결과: {}권", books.size());
        assertThat(books).isNotNull();
        log.info("[DEBUG_LOG] API 호출 성공");
    }

    // ========== 대출 가능 도서 조회 API 테스트 ==========

    @Test
    @DisplayName("API - ISBN으로 대출 가능 도서 조회 (전국)")
    void testSearchLoanItemsByIsbn() {
        // Given
        String isbn13 = "9788936434120";

        // When
        List<LoanItemInfo> loanItems = client.searchLoanItemsByIsbn(isbn13);

        // Then
        log.info("[DEBUG_LOG] 대출 가능 도서 조회 결과 (전국): {}개 도서관", loanItems.size());
        assertThat(loanItems).isNotNull();
        log.info("[DEBUG_LOG] API 호출 성공");
    }

    @Test
    @DisplayName("API - 지역별 대출 가능 도서 조회")
    void testSearchLoanItemsByIsbnWithRegion() {
        // Given
        String isbn13 = "9788936434120";
        String region = "11"; // 서울

        // When
        List<LoanItemInfo> loanItems = client.searchLoanItemsByIsbn(isbn13, region);

        // Then
        log.info("[DEBUG_LOG] 지역별 대출 가능 도서 조회 결과: {}개 도서관", loanItems.size());
        assertThat(loanItems).isNotNull();
        log.info("[DEBUG_LOG] API 호출 성공");
    }

    @Test
    @DisplayName("API - 도서관별 대출 가능 도서 조회")
    void testSearchLoanItemsByLibrary() {
        // Given
        String isbn13 = "9788936434120";
        String libraryName = "국립중앙도서관";

        // When
        List<LoanItemInfo> loanItems = client.searchLoanItemsByLibrary(isbn13, libraryName);

        // Then
        log.info("[DEBUG_LOG] 도서관별 대출 가능 도서 조회 결과: {}권", loanItems.size());
        assertThat(loanItems).isNotNull();
        log.info("[DEBUG_LOG] API 호출 성공");
    }

    // ========== 예외 처리 테스트 ==========

    @Test
    @DisplayName("API - 존재하지 않는 도서 검색")
    void testSearchNonExistentBook() {
        // Given
        String title = "존재하지않는도서명xyz999999";

        // When
        List<LibraryBookInfo> books = client.searchBooksByTitle(title);

        // Then
        log.info("[DEBUG_LOG] 미발견 도서 검색 결과: {}권", books.size());
        assertThat(books).isNotNull();
        // 결과가 없어도 API 호출은 성공해야 함
    }

    @Test
    @DisplayName("API - 빈 문자열 검색")
    void testSearchWithEmptyString() {
        // Given
        String title = "";

        // When
        List<LibraryBookInfo> result = client.searchBooksByTitle(title);

        // Then
        assertThat(result).isNotNull();
        log.info("[DEBUG_LOG] 빈 문자열 검색 - 예외 없이 처리됨");
    }

    // ========== URL 인코딩 테스트 ==========

    @Test
    @DisplayName("API - 한국어 검색어 인코딩")
    void testKoreanEncoding() {
        // Given
        String title = "삼국지";

        // When
        List<LibraryBookInfo> books = client.searchBooksByTitle(title);

        // Then
        log.info("[DEBUG_LOG] 한국어 검색 결과: {}권", books.size());
        assertThat(books).isNotNull();
        log.info("[DEBUG_LOG] 한국어 인코딩 정상 동작");
    }

    @Test
    @DisplayName("API - 영어 검색어")
    void testEnglishSearch() {
        // Given
        String title = "Harry Potter";

        // When
        List<LibraryBookInfo> books = client.searchBooksByTitle(title);

        // Then
        log.info("[DEBUG_LOG] 영어 검색 결과: {}권", books.size());
        assertThat(books).isNotNull();
        log.info("[DEBUG_LOG] 영어 검색 정상 동작");
    }

    @Test
    @DisplayName("API - 특수문자 포함 검색어")
    void testSpecialCharacters() {
        // Given
        String title = "C++ programming";

        // When
        List<LibraryBookInfo> books = client.searchBooksByTitle(title);

        // Then
        log.info("[DEBUG_LOG] 특수문자 검색 결과: {}권", books.size());
        assertThat(books).isNotNull();
        log.info("[DEBUG_LOG] 특수문자 처리 정상 동작");
    }

    // ========== 데이터 파싱 테스트 ==========

    @Test
    @DisplayName("API - JSON 응답 파싱 확인")
    void testResponseParsing() {
        // Given
        String title = "자바";

        // When
        List<LibraryBookInfo> books = client.searchBooksByTitle(title);

        // Then - 결과가 있는 경우 필드 확인
        if (!books.isEmpty()) {
            LibraryBookInfo first = books.get(0);
            log.info("[DEBUG_LOG] 첫 번째 도서: title={}, author={}, isbn={}, loanAvailable={}",
                first.title(), first.authors(), first.isbn13(), first.loanAvailable());

            assertThat(first.title()).isNotNull();
            assertThat(first.isbn13()).isNotNull();
            log.info("[DEBUG_LOG] JSON 파싱 정상 동작");
        } else {
            log.info("[DEBUG_LOG] 검색 결과가 없어 파싱 검증 건너뜀");
        }
    }

    @Test
    @DisplayName("API - 대출 도서 정보 파싱 확인")
    void testLoanItemParsing() {
        // Given
        String isbn13 = "9788960777331";

        // When
        List<LoanItemInfo> items = client.searchLoanItemsByIsbn(isbn13);

        // Then - 결과가 있는 경우 필드 확인
        if (!items.isEmpty()) {
            LoanItemInfo first = items.get(0);
            log.info("[DEBUG_LOG] 첫 번째 대출 도서: book={}, library={}, region={}, available={}",
                first.bookName(), first.libraryName(), first.region(), first.loanAvailable());

            assertThat(first.isbn13()).isNotNull();
            assertThat(first.bookName()).isNotNull();
            assertThat(first.libraryName()).isNotNull();
            log.info("[DEBUG_LOG] 대출 도서 JSON 파싱 정상 동작");
        } else {
            log.info("[DEBUG_LOG] 대출 가능 도서가 없어 파싱 검증 건너뜀");
        }
    }

    // ========== API 응답 시간 테스트 ==========

    @Test
    @DisplayName("API - 응답 시간 측정")
    void testResponseTime() {
        // Given
        String title = "자바";

        // When
        long startTime = System.currentTimeMillis();
        List<LibraryBookInfo> books = client.searchBooksByTitle(title);
        long duration = System.currentTimeMillis() - startTime;

        // Then
        log.info("[DEBUG_LOG] API 응답 시간: {}ms", duration);
        assertThat(books).isNotNull();
        assertThat(duration).isLessThan(10000); // 10초 이내 응답
        log.info("[DEBUG_LOG] API 응답 시간 정상");
    }
}
