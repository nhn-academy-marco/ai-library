package com.nhnacademy.library.external.opennaru.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.library.external.opennaru.dto.*;
import com.nhnacademy.library.external.opennaru.properties.LibraryApiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 도서관정보나루 API 클라이언트
 *
 * 도서관정보나루(https://www.data4library.kr) API를 호출하여
 * 전국 도서관의 장서 정보를 조회합니다.
 *
 * RestClient를 사용하여 HTTP 요청을 수행합니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LibraryInfoNaruApiClient {

    private final LibraryApiProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient.Builder restClientBuilder;

    /**
     * RestClient 생성
     *
     * @return 설정된 RestClient
     */
    private RestClient createRestClient() {
        return restClientBuilder.build();
    }

    /**
     * URL 인코딩 헬퍼
     */
    private String encode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }

    /**
     * 제목으로 도서 검색
     *
     * @param title 검색할 도서 제목
     * @return 도서 정보 목록
     */
    @Cacheable(value = "librarySearch", key = "#title", unless = "#result.isEmpty()")
    public List<LibraryBookInfo> searchBooksByTitle(String title) {
        log.info("[도서관정보나루] 도서 검색: title={}", title);

        try {
            String url = String.format("%s/srchBooks?keyword=%s&authKey=%s&format=json&pageSize=20",
                properties.getUrl(),
                encode(title),
                properties.getKey());

            String response = createRestClient().get()
                .uri(url)
                .retrieve()
                .body(String.class);

            return parseBookSearchResponse(response);

        } catch (RestClientException e) {
            log.error("[도서관정보나루] API 호출 실패: title={}", title, e);
            return List.of();
        }
    }

    /**
     * ISBN으로 도서 검색
     *
     * @param isbn13 ISBN13
     * @return 도서 정보 목록
     */
    @Cacheable(value = "librarySearchByIsbn", key = "#isbn13", unless = "#result.isEmpty()")
    public List<LibraryBookInfo> searchBooksByIsbn(String isbn13) {
        log.info("[도서관정보나루] ISBN 검색: isbn13={}", isbn13);

        String url = String.format("%s/srchBooks?keyword=%s&authKey=%s&format=json&pageSize=10",
            properties.getUrl(),
            isbn13,
            properties.getKey());

        log.info("[DEBUG] 호출 URL: {}", url); // 디버그용 URL 로그

        try {
            String response = createRestClient().get()
                .uri(url)
                .retrieve()
                .body(String.class);

            return parseBookSearchResponse(response);

        } catch (RestClientException e) {
            log.error("[도서관정보나루] ISBN 검색 실패: isbn13={}, URL={}", isbn13, url, e);
            return List.of();
        }
    }

    /**
     * 저자명으로 도서 검색
     *
     * @param author 저자명
     * @return 도서 정보 목록
     */
    @Cacheable(value = "librarySearchByAuthor", key = "#author", unless = "#result.isEmpty()")
    public List<LibraryBookInfo> searchBooksByAuthor(String author) {
        log.info("[도서관정보나루] 저자 검색: author={}", author);

        try {
            String url = String.format("%s/srchBooks?keyword=%s&authKey=%s&format=json&pageSize=20",
                properties.getUrl(),
                encode(author),
                properties.getKey());

            String response = createRestClient().get()
                .uri(url)
                .retrieve()
                .body(String.class);

            return parseBookSearchResponse(response);

        } catch (RestClientException e) {
            log.error("[도서관정보나루] 저자 검색 실패: author={}", author, e);
            return List.of();
        }
    }

    /**
     * ISBN(10자리)으로 도서 검색
     *
     * @param isbn ISBN(10자리)
     * @return 도서 정보 목록
     */
    @Cacheable(value = "librarySearchByIsbn", key = "'isbn10:' + #isbn", unless = "#result.isEmpty()")
    public List<LibraryBookInfo> searchBooksByIsbn10(String isbn) {
        log.info("[도서관정보나루] ISBN10 검색: isbn={}", isbn);

        try {
            String url = String.format("%s/srchBooks?keyword=%s&authKey=%s&format=json&pageSize=10",
                properties.getUrl(),
                isbn,
                properties.getKey());

            String response = createRestClient().get()
                .uri(url)
                .retrieve()
                .body(String.class);

            return parseBookSearchResponse(response);

        } catch (RestClientException e) {
            log.error("[도서관정보나루] ISBN10 검색 실패: isbn={}", isbn, e);
            return List.of();
        }
    }

    /**
     * 출판사로 도서 검색
     *
     * @param publisher 출판사명
     * @return 도서 정보 목록
     */
    @Cacheable(value = "librarySearchByPublisher", key = "#publisher", unless = "#result.isEmpty()")
    public List<LibraryBookInfo> searchBooksByPublisher(String publisher) {
        log.info("[도서관정보나루] 출판사 검색: publisher={}", publisher);

        try {
            String url = String.format("%s/srchBooks?keyword=%s&authKey=%s&format=json&pageSize=20",
                properties.getUrl(),
                encode(publisher),
                properties.getKey());

            String response = createRestClient().get()
                .uri(url)
                .retrieve()
                .body(String.class);

            return parseBookSearchResponse(response);

        } catch (RestClientException e) {
            log.error("[도서관정보나루] 출판사 검색 실패: publisher={}", publisher, e);
            return List.of();
        }
    }

    /**
     * 대출 가능 도서 조회 (ISBN 기반)
     *
     * @param isbn13 ISBN13
     * @param region 지역 코드 (예: "11" for 서울)
     * @return 대출 가능 도서 정보 목록
     */
    @Cacheable(value = "libraryLoanItems", key = "'isbn:' + #isbn13 + ':region:' + #region", unless = "#result.isEmpty()")
    public List<LoanItemInfo> searchLoanItemsByIsbn(String isbn13, String region) {
        log.info("[도서관정보나루] 대출 가능 도서 조회: isbn13={}, region={}", isbn13, region);

        try {
            String url;
            if (region != null && !region.isEmpty()) {
                url = String.format("%s/libSrchByBook?isbn=%s&region=%s&authKey=%s&format=json&pageSize=50",
                    properties.getUrl(),
                    isbn13,
                    region,
                    properties.getKey());
            } else {
                url = String.format("%s/libSrchByBook?isbn=%s&authKey=%s&format=json&pageSize=50",
                    properties.getUrl(),
                    isbn13,
                    properties.getKey());
            }

            String response = createRestClient().get()
                .uri(url)
                .retrieve()
                .body(String.class);

            return parseLoanItemResponse(response);

        } catch (RestClientException e) {
            log.error("[도서관정보나루] 대출 가능 도서 조회 실패: isbn13={}", isbn13, e);
            return List.of();
        }
    }

    /**
     * 대출 가능 도서 조회 (전국)
     *
     * @param isbn13 ISBN13
     * @return 전국 도서관의 대출 가능 도서 정보 목록
     */
    @Cacheable(value = "libraryLoanItems", key = "'isbn:' + #isbn13 + ':all'", unless = "#result.isEmpty()")
    public List<LoanItemInfo> searchLoanItemsByIsbn(String isbn13) {
        return searchLoanItemsByIsbn(isbn13, null);
    }

    /**
     * 대출 가능 도서 조회 (도서관명 기반)
     *
     * 참고: libSrchByBook API는 도서관명 파라미터를 지원하지 않습니다.
     * 도서관명으로 검색하려면 먼저 libSrch로 도서관을 검색하여 도서관 코드를 얻어야 합니다.
     *
     * @param isbn13 ISBN13
     * @param libraryName 도서관명
     * @return 대출 가능 도서 정보 목록 (현재는 빈 리스트 반환)
     */
    @Cacheable(value = "libraryLoanItems", key = "'isbn:' + #isbn13 + ':library:' + #libraryName", unless = "#result.isEmpty()")
    public List<LoanItemInfo> searchLoanItemsByLibrary(String isbn13, String libraryName) {
        log.info("[도서관정보나루] 도서관별 대출 가능 도서 조회: isbn13={}, library={}", isbn13, libraryName);

        // libSrchByBook은 도서관명 파라미터를 지원하지 않음
        // TODO: libSrch로 도서관 코드를 조회한 후 libSrchByBook을 호출하는 로직 구현 필요
        log.warn("[도서관정보나루] 도서관명 검색은 현재 지원되지 않습니다. libSrch API를 사용하세요.");
        return List.of();
    }

    /**
     * 대출 가능 도서 정보 응답 파싱
     *
     * @param response API 응답 JSON 문자열
     * @return 파싱된 대출 가능 도서 정보 목록
     */
    private List<LoanItemInfo> parseLoanItemResponse(String response) {
        List<LoanItemInfo> loanItems = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(response);

            // 에러 응답 체크
            JsonNode errorNode = root.path("response").path("error");
            if (errorNode.isTextual() && !errorNode.asText().isEmpty()) {
                log.warn("[도서관정보나루] API 에러 응답: {}", errorNode.asText());
                return List.of();
            }

            // 실제 응답 구조: response.libs[].lib
            JsonNode libs = root.path("response").path("libs");

            for (JsonNode libWrapper : libs) {
                JsonNode lib = libWrapper.path("lib");

                loanItems.add(LoanItemInfo.builder()
                    .isbn13("") // libSrchByBook 응답에는 ISBN이 없음 (요청 파라미터로 알고 있음)
                    .bookName("") // libSrchByBook 응답에는 도서명이 없음
                    .authors("")
                    .publisher("")
                    .libraryName(lib.path("libName").asText(""))
                    .region("") // libSrchByBook 응답에는 지역명이 없음
                    .loanAvailable("Y") // 소장하고 있다고 가정
                    .returnDueDate("")
                    .callNumber("")
                    .location(lib.path("address").asText("")) // 주소를 위치로 사용
                    .build());
            }

            log.info("[도서관정보나루] 도서관 정보 파싱 완료: {}개 도서관", loanItems.size());

        } catch (Exception e) {
            log.error("[도서관정보나루] 대출 가능 도서 응답 파싱 실패", e);
        }
        return loanItems;
    }

    /**
     * 도서관정보나루 API 응답 파싱
     *
     * @param response API 응답 JSON 문자열
     * @return 파싱된 도서 정보 목록
     */
    private List<LibraryBookInfo> parseBookSearchResponse(String response) {
        List<LibraryBookInfo> books = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(response);

            // 에러 응답 체크
            if (root.has("error")) {
                String errorMessage = root.path("response").path("error").asText();
                log.warn("[도서관정보나루] API 에러 응답: {}", errorMessage);
                return List.of();
            }

            // 실제 응답 구조: response.docs[].doc
            JsonNode docs = root.path("response").path("docs");

            for (JsonNode docWrapper : docs) {
                JsonNode doc = docWrapper.path("doc");

                // Build LibraryBookInfo with all fields
                LibraryBookInfo bookInfo = LibraryBookInfo.builder()
                    .title(doc.path("bookname").asText(""))
                    .authors(doc.path("authors").asText(""))
                    .publisher(doc.path("publisher").asText(""))
                    .isbn13(doc.path("isbn13").asText(""))
                    .loanAvailable(true) // srchBooks는 loanAvailable 필드 없음
                    .loanCount(doc.path("loan_count").asInt(0))
                    .build();

                books.add(bookInfo);
            }

            log.info("[도서관정보나루] 파싱 완료: {}권", books.size());

        } catch (Exception e) {
            log.error("[도서관정보나루] 응답 파싱 실패", e);
        }
        return books;
    }

    /**
     * 다양한 날짜 형식 파싱
     *
     * @param dateStr 날짜 문자열
     * @return LocalDate
     */
    private LocalDate parsePublishDate(String dateStr) {
        List<String> patterns = List.of(
            "yyyy-MM-dd",
            "yyyyMMdd",
            "yyyy-MM",
            "yyyy"
        );

        for (String pattern : patterns) {
            try {
                return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(pattern));
            } catch (Exception ignored) {
                // 다음 패턴 시도
            }
        }
        throw new IllegalArgumentException("지원하지 않는 날짜 형식: " + dateStr);
    }

    // ============================================================================
    // 미구현 API 추가 구현
    // ============================================================================

    /**
     * 도서관 검색 (libSrch)
     *
     * @param libraryName 도서관명 (URL 인코딩 필요, null일 경우 전체 검색)
     * @param region 지역 코드 (null일 경우 전체)
     * @return 도서관 정보 목록
     */
    @Cacheable(value = "librarySearch", key = "'lib:' + #libraryName + ':region:' + #region", unless = "#result.isEmpty()")
    public List<LibraryInfo> searchLibraries(String libraryName, String region) {
        log.info("[도서관정보나루] 도서관 검색: name={}, region={}", libraryName, region);

        try {
            StringBuilder urlBuilder = new StringBuilder(String.format("%s/libSrch?authKey=%s&format=json&pageSize=50",
                properties.getUrl(),
                properties.getKey()));

            if (libraryName != null && !libraryName.isEmpty()) {
                urlBuilder.append("&libName=").append(encode(libraryName));
            }
            if (region != null && !region.isEmpty()) {
                urlBuilder.append("&region=").append(region);
            }

            String response = createRestClient().get()
                .uri(urlBuilder.toString())
                .retrieve()
                .body(String.class);

            return parseLibrarySearchResponse(response);

        } catch (RestClientException e) {
            log.error("[도서관정보나루] 도서관 검색 실패", e);
            return List.of();
        }
    }

    /**
     * 전체 도서관 검색
     */
    @Cacheable(value = "librarySearchAll", unless = "#result.isEmpty()")
    public List<LibraryInfo> searchAllLibraries() {
        return searchLibraries(null, null);
    }

    /**
     * 지역별 도서관 검색
     */
    @Cacheable(value = "librarySearchByRegion", key = "#region", unless = "#result.isEmpty()")
    public List<LibraryInfo> searchLibrariesByRegion(String region) {
        return searchLibraries(null, region);
    }

    /**
     * 도서관명으로 검색
     */
    @Cacheable(value = "librarySearchByName", key = "#name", unless = "#result.isEmpty()")
    public List<LibraryInfo> searchLibrariesByName(String name) {
        return searchLibraries(name, null);
    }

    /**
     * 도서 상세 정보 조회 (srchDtlList)
     *
     * @param isbn13 ISBN13
     * @param includeLoanInfo 대출 정보 포함 여부
     * @return 도서 상세 정보
     */
    @Cacheable(value = "bookDetail", key = "'isbn:' + #isbn13 + ':loan:' + #includeLoanInfo")
    public BookDetailInfo getBookDetail(String isbn13, boolean includeLoanInfo) {
        log.info("[도서관정보나루] 도서 상세 조회: isbn13={}, loanInfo={}", isbn13, includeLoanInfo);

        try {
            String url = String.format("%s/srchDtlList?authKey=%s&isbn13=%s&format=json&loaninfoYN=%s",
                properties.getUrl(),
                properties.getKey(),
                isbn13,
                includeLoanInfo ? "Y" : "N");

            String response = createRestClient().get()
                .uri(url)
                .retrieve()
                .body(String.class);

            return parseBookDetailResponse(response, includeLoanInfo);

        } catch (RestClientException e) {
            log.error("[도서관정보나루] 도서 상세 조회 실패: isbn13={}", isbn13, e);
            return null;
        }
    }

    /**
     * 추천도서 조회 (recommandList)
     *
     * @param isbn13 ISBN13
     * @param type 추천 타입 (mania=마니아, reader=다독자)
     * @return 추천도서 목록
     */
    @Cacheable(value = "recommendedBooks", key = "'isbn:' + #isbn13 + ':type:' + #type", unless = "#result.isEmpty()")
    public List<RecommendedBookInfo> getRecommendedBooks(String isbn13, String type) {
        log.info("[도서관정보나루] 추천도서 조회: isbn13={}, type={}", isbn13, type);

        try {
            String url = String.format("%s/recommandList?authKey=%s&isbn13=%s&format=json&type=%s",
                properties.getUrl(),
                properties.getKey(),
                isbn13,
                type);

            String response = createRestClient().get()
                .uri(url)
                .retrieve()
                .body(String.class);

            return parseRecommendedBooksResponse(response);

        } catch (RestClientException e) {
            log.error("[도서관정보나루] 추천도서 조회 실패: isbn13={}", isbn13, e);
            return List.of();
        }
    }

    /**
     * 마니아를 위한 추천도서
     */
    @Cacheable(value = "recommendedBooksForMania", key = "#isbn13", unless = "#result.isEmpty()")
    public List<RecommendedBookInfo> getRecommendedBooksForMania(String isbn13) {
        return getRecommendedBooks(isbn13, "mania");
    }

    /**
     * 다독자를 위한 추천도서
     */
    @Cacheable(value = "recommendedBooksForReader", key = "#isbn13", unless = "#result.isEmpty()")
    public List<RecommendedBookInfo> getRecommendedBooksForReader(String isbn13) {
        return getRecommendedBooks(isbn13, "reader");
    }

    /**
     * 도서관 검색 응답 파싱
     */
    private List<LibraryInfo> parseLibrarySearchResponse(String response) {
        List<LibraryInfo> libraries = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(response);

            // 에러 응답 체크
            JsonNode errorNode = root.path("response").path("error");
            if (errorNode.isTextual() && !errorNode.asText().isEmpty()) {
                log.warn("[도서관정보나루] API 에러 응답: {}", errorNode.asText());
                return List.of();
            }

            // 응답 구조: response.libs[].lib
            JsonNode libs = root.path("response").path("libs");

            for (JsonNode libWrapper : libs) {
                JsonNode lib = libWrapper.path("lib");

                libraries.add(LibraryInfo.builder()
                    .libCode(lib.path("libCode").asText(""))
                    .libName(lib.path("libName").asText(""))
                    .address(lib.path("address").asText(""))
                    .tel(lib.path("tel").asText(""))
                    .fax(lib.path("fax").asText(""))
                    .latitude(lib.path("latitude").asText(""))
                    .longitude(lib.path("longitude").asText(""))
                    .homepage(lib.path("homepage").asText(""))
                    .closedDays(lib.path("closed").asText(""))
                    .operatingHours(lib.path("operatingTime").asText(""))
                    .bookCount(lib.path("BookCount").asText(""))
                    .build());
            }

            log.info("[도서관정보나루] 도서관 파싱 완료: {}개 도서관", libraries.size());

        } catch (Exception e) {
            log.error("[도서관정보나루] 도서관 응답 파싱 실패", e);
        }
        return libraries;
    }

    /**
     * 도서 상세 정보 응답 파싱
     */
    private BookDetailInfo parseBookDetailResponse(String response, boolean includeLoanInfo) {
        try {
            JsonNode root = objectMapper.readTree(response);

            // 에러 응답 체크
            JsonNode errorNode = root.path("response").path("error");
            if (errorNode.isTextual() && !errorNode.asText().isEmpty()) {
                log.warn("[도서관정보나루] API 에러 응답: {}", errorNode.asText());
                return null;
            }

            // 응답 구조: response.docs[].doc
            JsonNode docs = root.path("response").path("docs");
            if (!docs.iterator().hasNext()) {
                return null;
            }

            JsonNode doc = docs.get(0).path("doc");

            // 대출 정보 파싱
            BookDetailInfo.LoanInfo loanInfo = null;
            if (includeLoanInfo) {
                JsonNode loanNode = doc.path("loanInfo");
                if (loanNode.isObject()) {
                    loanInfo = BookDetailInfo.LoanInfo.builder()
                        .totalLoanCount(loanNode.path("loanCount").asInt(0))
                        .maleLoanCount(loanNode.path("maleLoanCount").asInt(0))
                        .femaleLoanCount(loanNode.path("femaleLoanCount").asInt(0))
                        .teensLoanCount(loanNode.path("teensLoanCount").asInt(0))
                        .twentiesLoanCount(loanNode.path("twentiesLoanCount").asInt(0))
                        .thirtiesLoanCount(loanNode.path("thirtiesLoanCount").asInt(0))
                        .fortiesLoanCount(loanNode.path("fortiesLoanCount").asInt(0))
                        .fiftiesLoanCount(loanNode.path("fiftiesLoanCount").asInt(0))
                        .sixtiesLoanCount(loanNode.path("sixtiesLoanCount").asInt(0))
                        .build();
                }
            }

            return BookDetailInfo.builder()
                .isbn13(doc.path("isbn13").asText(""))
                .bookname(doc.path("bookname").asText(""))
                .authors(doc.path("authors").asText(""))
                .publisher(doc.path("publisher").asText(""))
                .publicationDate(doc.path("publication_date").asText(""))
                .imageUrl(doc.path("bookImageURL").asText(""))
                .description(doc.path("description").asText(""))
                .category(doc.path("classNo").asText(""))
                .loanCount(doc.path("loanCount").asInt(0))
                .loanInfo(loanInfo)
                .keywords(List.of()) // TODO: keywordList API 추가 시 연동
                .build();

        } catch (Exception e) {
            log.error("[도서관정보나루] 도서 상세 응답 파싱 실패", e);
            return null;
        }
    }

    /**
     * 추천도서 응답 파싱
     */
    private List<RecommendedBookInfo> parseRecommendedBooksResponse(String response) {
        List<RecommendedBookInfo> books = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(response);

            // 에러 응답 체크
            JsonNode errorNode = root.path("response").path("error");
            if (errorNode.isTextual() && !errorNode.asText().isEmpty()) {
                log.warn("[도서관정보나루] API 에러 응답: {}", errorNode.asText());
                return List.of();
            }

            // 응답 구조: response.docs[].book
            JsonNode docs = root.path("response").path("docs");

            if (!docs.isArray() || !docs.iterator().hasNext()) {
                log.warn("[도서관정보나루] 추천도서 응답에 docs가 없음");
                return List.of();
            }

            for (JsonNode bookWrapper : docs) {
                JsonNode book = bookWrapper.path("book");

                String isbn13 = book.path("isbn13").asText("");
                String bookName = book.path("bookname").asText("");

                if (!isbn13.isEmpty()) {
                    books.add(RecommendedBookInfo.builder()
                        .isbn13(isbn13)
                        .bookName(bookName)
                        .authors(book.path("authors").asText(""))
                        .publisher(book.path("publisher").asText(""))
                        .imageUrl(book.path("bookImageURL").asText(""))
                        .description("") // API에 description 필드 없음
                        .recommendationReason("") // API에 recommendationReason 필드 없음
                        .rank(book.path("no").asInt(0))
                        .build());
                }
            }

            log.info("[도서관정보나루] 추천도서 파싱 완료: {}권", books.size());

        } catch (Exception e) {
            log.error("[도서관정보나루] 추천도서 응답 파싱 실패", e);
            log.error("[도서관정보나루] 응답 내용: {}", response);
        }
        return books;
    }

    // ============================================================================
    // 추가 API 구현 (나머지 12개)
    // ============================================================================

    /**
     * 인기대출도서 조회 (loanItemSrch)
     */
    @Cacheable(value = "popularBooks", key = "'from:' + #startDt + ':to:' + #endDt + ':region:' + #region", unless = "#result.isEmpty()")
    public List<PopularBookInfo> getPopularBooks(String startDt, String endDt, String region) {
        log.info("[도서관정보나루] 인기대출도서 조회: {} ~ {}, region={}", startDt, endDt, region);

        try {
            StringBuilder urlBuilder = new StringBuilder(String.format(
                "%s/loanItemSrch?authKey=%s&startDt=%s&endDt=%s&format=json",
                properties.getUrl(), properties.getKey(), startDt, endDt));

            if (region != null && !region.isEmpty()) {
                urlBuilder.append("&region=").append(region);
            }

            String response = createRestClient().get()
                .uri(urlBuilder.toString())
                .retrieve()
                .body(String.class);

            return parsePopularBooksResponse(response);

        } catch (RestClientException e) {
            log.error("[도서관정보나루] 인기대출도서 조회 실패", e);
            return List.of();
        }
    }

    /**
     * 도서 키워드 목록 조회 (keywordList)
     */
    @Cacheable(value = "bookKeywords", key = "#isbn13", unless = "#result == null or #result.keywords.isEmpty()")
    public BookKeywordInfo getBookKeywords(String isbn13) {
        log.info("[도서관정보나루] 도서 키워드 조회: isbn13={}", isbn13);

        try {
            String url = String.format("%s/keywordList?authKey=%s&isbn13=%s&format=json",
                properties.getUrl(), properties.getKey(), isbn13);

            String response = createRestClient().get()
                .uri(url)
                .retrieve()
                .body(String.class);

            return parseBookKeywordsResponse(response);

        } catch (RestClientException e) {
            log.error("[도서관정보나루] 키워드 조회 실패: isbn13={}", isbn13, e);
            return null;
        }
    }

    /**
     * 도서 이용 분석 조회 (usageAnalysisList)
     */
    @Cacheable(value = "usageAnalysis", key = "#isbn13", unless = "#result == null")
    public UsageAnalysisInfo getUsageAnalysis(String isbn13) {
        log.info("[도서관정보나루] 도서 이용 분석 조회: isbn13={}", isbn13);

        try {
            String url = String.format("%s/usageAnalysisList?authKey=%s&isbn13=%s&format=json",
                properties.getUrl(), properties.getKey(), isbn13);

            String response = createRestClient().get()
                .uri(url)
                .retrieve()
                .body(String.class);

            return parseUsageAnalysisResponse(response);

        } catch (RestClientException e) {
            log.error("[도서관정보나루] 이용 분석 조회 실패: isbn13={}", isbn13, e);
            return null;
        }
    }

    /**
     * 도서관별 인기대출도서 조회 (loanItemSrchByLib)
     */
    @Cacheable(value = "popularBooksByLib", key = "#libCode", unless = "#result.isEmpty()")
    public List<PopularBookInfo> getPopularBooksByLibrary(String libCode) {
        log.info("[도서관정보나루] 도서관별 인기대출도서 조회: libCode={}", libCode);

        try {
            String url = String.format("%s/loanItemSrchByLib?authKey=%s&libCode=%s&format=json",
                properties.getUrl(), properties.getKey(), libCode);

            String response = createRestClient().get()
                .uri(url)
                .retrieve()
                .body(String.class);

            return parsePopularBooksResponse(response);

        } catch (RestClientException e) {
            log.error("[도서관정보나루] 도서관별 인기대출도서 조회 실패: libCode={}", libCode, e);
            return List.of();
        }
    }

    /**
     * 대출반납추이 조회 (usageTrend)
     */
    @Cacheable(value = "usageTrend", key = "'lib:' + #libCode + ':type:' + #type", unless = "#result == null")
    public UsageTrendInfo getUsageTrend(String libCode, String type) {
        log.info("[도서관정보나루] 대출반납추이 조회: libCode={}, type={}", libCode, type);

        try {
            String url = String.format("%s/usageTrend?authKey=%s&libCode=%s&format=json&type=%s",
                properties.getUrl(), properties.getKey(), libCode, type);

            String response = createRestClient().get()
                .uri(url)
                .retrieve()
                .body(String.class);

            return parseUsageTrendResponse(response, libCode);

        } catch (RestClientException e) {
            log.error("[도서관정보나루] 대출반납추이 조회 실패: libCode={}", libCode, e);
            return null;
        }
    }

    /**
     * 도서 소장여부 조회 (bookExist)
     */
    @Cacheable(value = "bookExist", key = "'lib:' + #libCode + ':isbn:' + #isbn13", unless = "#result == null")
    public BookExistInfo checkBookExists(String libCode, String isbn13) {
        log.info("[도서관정보나루] 도서 소장여부 조회: libCode={}, isbn13={}", libCode, isbn13);

        try {
            String url = String.format("%s/bookExist?authKey=%s&libCode=%s&isbn13=%s&format=json",
                properties.getUrl(), properties.getKey(), libCode, isbn13);

            String response = createRestClient().get()
                .uri(url)
                .retrieve()
                .body(String.class);

            return parseBookExistResponse(response, libCode);

        } catch (RestClientException e) {
            log.error("[도서관정보나루] 도서 소장여부 조회 실패: libCode={}", libCode, e);
            return null;
        }
    }

    /**
     * 대출 급상승 도서 조회 (hotTrend)
     */
    @Cacheable(value = "hotTrendBooks", key = "#searchDate", unless = "#result.isEmpty()")
    public List<HotTrendBookInfo> getHotTrendBooks(String searchDate) {
        log.info("[도서관정보나루] 대출 급상승 도서 조회: date={}", searchDate);

        try {
            String url = String.format("%s/hotTrend?authKey=%s&searchDt=%s&format=json",
                properties.getUrl(), properties.getKey(), searchDate);

            String response = createRestClient().get()
                .uri(url)
                .retrieve()
                .body(String.class);

            return parseHotTrendResponse(response, searchDate);

        } catch (RestClientException e) {
            log.error("[도서관정보나루] 대출 급상승 도서 조회 실패: date={}", searchDate, e);
            return List.of();
        }
    }

    /**
     * 월간 키워드 조회 (monthlyKeywords)
     */
    @Cacheable(value = "monthlyKeywords", key = "#month", unless = "#result.isEmpty()")
    public List<MonthlyKeywordInfo> getMonthlyKeywords(String month) {
        log.info("[도서관정보나루] 월간 키워드 조회: month={}", month);

        try {
            String url = String.format("%s/monthlyKeywords?authKey=%s&month=%s&format=json",
                properties.getUrl(), properties.getKey(), month);

            String response = createRestClient().get()
                .uri(url)
                .retrieve()
                .body(String.class);

            return parseMonthlyKeywordsResponse(response);

        } catch (RestClientException e) {
            log.error("[도서관정보나루] 월간 키워드 조회 실패: month={}", month, e);
            return List.of();
        }
    }

    /**
     * 지역별 독서량/독서율 조회 (readQt)
     */
    @Cacheable(value = "readingQuantity", key = "'region:' + #region + ':dtl:' + #dtlRegion", unless = "#result == null")
    public ReadingQuantityInfo getReadingQuantity(String region, String dtlRegion) {
        log.info("[도서관정보나루] 독서량/독서율 조회: region={}, dtlRegion={}", region, dtlRegion);

        try {
            StringBuilder urlBuilder = new StringBuilder(String.format(
                "%s/readQt?authKey=%s&region=%s&format=json",
                properties.getUrl(), properties.getKey(), region));

            if (dtlRegion != null && !dtlRegion.isEmpty()) {
                urlBuilder.append("&dtl_region=").append(dtlRegion);
            }

            String response = createRestClient().get()
                .uri(urlBuilder.toString())
                .retrieve()
                .body(String.class);

            return parseReadingQuantityResponse(response);

        } catch (RestClientException e) {
            log.error("[도서관정보나루] 독서량/독서율 조회 실패: region={}", region, e);
            return null;
        }
    }

    // ============================================================================
    // 파싱 메서드
    // ============================================================================

    private List<PopularBookInfo> parsePopularBooksResponse(String response) {
        List<PopularBookInfo> books = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(response);

            JsonNode errorNode = root.path("response").path("error");
            if (errorNode.isTextual() && !errorNode.asText().isEmpty()) {
                log.warn("[도서관정보나루] API 에러 응답: {}", errorNode.asText());
                return List.of();
            }

            JsonNode docs = root.path("response").path("docs");

            for (JsonNode docWrapper : docs) {
                JsonNode doc = docWrapper.path("doc");

                books.add(PopularBookInfo.builder()
                    .isbn13(doc.path("isbn13").asText(""))
                    .bookName(doc.path("bookname").asText(""))
                    .authors(doc.path("authors").asText(""))
                    .publisher(doc.path("publisher").asText(""))
                    .publicationYear(doc.path("publication_year").asText(""))
                    .imageUrl(doc.path("bookImageURL").asText(""))
                    .loanCount(doc.path("loanCount").asInt(0))
                    .ranking(doc.path("ranking").asText(""))
                    .build());
            }

            log.info("[도서관정보나루] 인기대출도서 파싱 완료: {}권", books.size());

        } catch (Exception e) {
            log.error("[도서관정보나루] 인기대출도서 응답 파싱 실패", e);
        }
        return books;
    }

    private BookKeywordInfo parseBookKeywordsResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);

            JsonNode errorNode = root.path("response").path("error");
            if (errorNode.isTextual() && !errorNode.asText().isEmpty()) {
                log.warn("[도서관정보나루] API 에러 응답: {}", errorNode.asText());
                return null;
            }

            JsonNode docs = root.path("response").path("docs");

            if (!docs.iterator().hasNext()) {
                return null;
            }

            JsonNode doc = docs.get(0).path("doc");

            String isbn13 = doc.path("isbn13").asText("");
            String bookName = doc.path("bookname").asText("");

            List<BookKeywordInfo.Keyword> keywords = new ArrayList<>();
            JsonNode keywordNode = doc.path("keywords");
            if (keywordNode.isArray()) {
                for (JsonNode kw : keywordNode) {
                    keywords.add(BookKeywordInfo.Keyword.builder()
                        .word(kw.path("keyword").asText(""))
                        .rank(kw.path("rank").asInt(0))
                        .score(kw.path("score").asInt(0))
                        .build());
                }
            }

            return BookKeywordInfo.builder()
                .isbn13(isbn13)
                .bookName(bookName)
                .keywords(keywords)
                .build();

        } catch (Exception e) {
            log.error("[도서관정보나루] 키워드 응답 파싱 실패", e);
            return null;
        }
    }

    private UsageAnalysisInfo parseUsageAnalysisResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);

            JsonNode errorNode = root.path("response").path("error");
            if (errorNode.isTextual() && !errorNode.asText().isEmpty()) {
                log.warn("[도서관정보나루] API 에러 응답: {}", errorNode.asText());
                return null;
            }

            JsonNode docs = root.path("response").path("docs");

            if (!docs.iterator().hasNext()) {
                return null;
            }

            JsonNode doc = docs.get(0).path("doc");

            String isbn13 = doc.path("isbn13").asText("");
            String bookName = doc.path("bookname").asText("");
            int totalLoanCount = doc.path("loanCount").asInt(0);

            JsonNode genderNode = doc.path("genderLoanInfo");
            UsageAnalysisInfo.GenderLoanInfo genderInfo = UsageAnalysisInfo.GenderLoanInfo.builder()
                .maleCount(genderNode.path("maleLoanCount").asInt(0))
                .femaleCount(genderNode.path("femaleLoanCount").asInt(0))
                .build();

            JsonNode ageNode = doc.path("ageLoanInfo");
            UsageAnalysisInfo.AgeLoanInfo ageInfo = UsageAnalysisInfo.AgeLoanInfo.builder()
                .teensCount(ageNode.path("teensLoanCount").asInt(0))
                .twentiesCount(ageNode.path("twentiesLoanCount").asInt(0))
                .thirtiesCount(ageNode.path("thirtiesLoanCount").asInt(0))
                .fortiesCount(ageNode.path("fortiesLoanCount").asInt(0))
                .fiftiesCount(ageNode.path("fiftiesLoanCount").asInt(0))
                .sixtiesCount(ageNode.path("sixtiesLoanCount").asInt(0))
                .build();

            return UsageAnalysisInfo.builder()
                .isbn13(isbn13)
                .bookName(bookName)
                .totalLoanCount(totalLoanCount)
                .genderLoanInfo(genderInfo)
                .ageLoanInfo(ageInfo)
                .build();

        } catch (Exception e) {
            log.error("[도서관정보나루] 이용 분석 응답 파싱 실패", e);
            return null;
        }
    }

    private UsageTrendInfo parseUsageTrendResponse(String response, String libCode) {
        try {
            JsonNode root = objectMapper.readTree(response);

            JsonNode errorNode = root.path("response").path("error");
            if (errorNode.isTextual() && !errorNode.asText().isEmpty()) {
                log.warn("[도서관정보나루] API 에러 응답: {}", errorNode.asText());
                return null;
            }

            JsonNode doc = root.path("response").path("doc");

            String libName = doc.path("libName").asText("");
            String type = doc.path("type").asText("");

            JsonNode trendNode = doc.path("loanReturnInfo");

            UsageTrendInfo.LoanReturnTrend trend = UsageTrendInfo.LoanReturnTrend.builder()
                .mondayLoan(trendNode.path("mondayLoan").asInt(0))
                .tuesdayLoan(trendNode.path("tuesdayLoan").asInt(0))
                .wednesdayLoan(trendNode.path("wednesdayLoan").asInt(0))
                .thursdayLoan(trendNode.path("thursdayLoan").asInt(0))
                .fridayLoan(trendNode.path("fridayLoan").asInt(0))
                .saturdayLoan(trendNode.path("saturdayLoan").asInt(0))
                .sundayLoan(trendNode.path("sundayLoan").asInt(0))
                .hourlyLoanAvg(trendNode.path("hourlyLoanAvg").asInt(0))
                .build();

            return UsageTrendInfo.builder()
                .libCode(libCode)
                .libName(libName)
                .type(type)
                .loanTrend(trend)
                .build();

        } catch (Exception e) {
            log.error("[도서관정보나루] 대출반납추이 응답 파싱 실패", e);
            return null;
        }
    }

    private BookExistInfo parseBookExistResponse(String response, String libCode) {
        try {
            JsonNode root = objectMapper.readTree(response);

            JsonNode errorNode = root.path("response").path("error");
            if (errorNode.isTextual() && !errorNode.asText().isEmpty()) {
                log.warn("[도서관정보나루] API 에러 응답: {}", errorNode.asText());
                return null;
            }

            JsonNode doc = root.path("response").path("doc");

            boolean exists = doc.has("result") && "Y".equals(doc.path("result").asText());

            return BookExistInfo.builder()
                .libCode(libCode)
                .libName(doc.path("libName").asText(""))
                .isbn13(doc.path("isbn13").asText(""))
                .exists(exists)
                .loanAvailable(doc.path("loanAvailable").asText(""))
                .callNumber(doc.path("callNumber").asText(""))
                .location(doc.path("location").asText(""))
                .build();

        } catch (Exception e) {
            log.error("[도서관정보나루] 도서 소장여부 응답 파싱 실패", e);
            return null;
        }
    }

    private List<HotTrendBookInfo> parseHotTrendResponse(String response, String searchDate) {
        List<HotTrendBookInfo> books = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(response);

            JsonNode errorNode = root.path("response").path("error");
            if (errorNode.isTextual() && !errorNode.asText().isEmpty()) {
                log.warn("[도서관정보나루] API 에러 응답: {}", errorNode.asText());
                return List.of();
            }

            JsonNode docs = root.path("response").path("docs");

            for (JsonNode docWrapper : docs) {
                JsonNode doc = docWrapper.path("doc");

                books.add(HotTrendBookInfo.builder()
                    .isbn13(doc.path("isbn13").asText(""))
                    .bookName(doc.path("bookname").asText(""))
                    .authors(doc.path("authors").asText(""))
                    .publisher(doc.path("publisher").asText(""))
                    .imageUrl(doc.path("bookImageURL").asText(""))
                    .currentRank(doc.path("currentRank").asInt(0))
                    .previousRank(doc.path("previousRank").asInt(0))
                    .rankChange(doc.path("rankChange").asInt(0))
                    .searchDate(searchDate)
                    .build());
            }

            log.info("[도서관정보나루] 대출 급상승 도서 파싱 완료: {}권", books.size());

        } catch (Exception e) {
            log.error("[도서관정보나루] 대출 급상승 도서 응답 파싱 실패", e);
        }
        return books;
    }

    private List<MonthlyKeywordInfo> parseMonthlyKeywordsResponse(String response) {
        List<MonthlyKeywordInfo> keywords = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(response);

            JsonNode errorNode = root.path("response").path("error");
            if (errorNode.isTextual() && !errorNode.asText().isEmpty()) {
                log.warn("[도서관정보나루] API 에러 응답: {}", errorNode.asText());
                return List.of();
            }

            JsonNode docs = root.path("response").path("docs");

            for (JsonNode docWrapper : docs) {
                JsonNode doc = docWrapper.path("doc");

                keywords.add(MonthlyKeywordInfo.builder()
                    .month(doc.path("month").asText(""))
                    .keyword(doc.path("keyword").asText(""))
                    .rank(doc.path("rank").asInt(0))
                    .score(doc.path("score").asInt(0))
                    .build());
            }

            log.info("[도서관정보나루] 월간 키워드 파싱 완료: {}개", keywords.size());

        } catch (Exception e) {
            log.error("[도서관정보나루] 월간 키워드 응답 파싱 실패", e);
        }
        return keywords;
    }

    private ReadingQuantityInfo parseReadingQuantityResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);

            JsonNode errorNode = root.path("response").path("error");
            if (errorNode.isTextual() && !errorNode.asText().isEmpty()) {
                log.warn("[도서관정보나루] API 에러 응답: {}", errorNode.asText());
                return null;
            }

            JsonNode doc = root.path("response").path("doc");

            return ReadingQuantityInfo.builder()
                .region(doc.path("region").asText(""))
                .regionName(doc.path("regionName").asText(""))
                .dtlRegion(doc.path("dtl_region").asText(""))
                .dtlRegionName(doc.path("dtl_regionName").asText(""))
                .readingQuantity(doc.path("readingQuantity").asDouble(0.0))
                .readingRate(doc.path("readingRate").asDouble(0.0))
                .build();

        } catch (Exception e) {
            log.error("[도서관정보나루] 독서량/독서율 응답 파싱 실패", e);
            return null;
        }
    }

    // ============================================================================
    // extends/libSrch (도서관 통합정보) API
    // ============================================================================

    /**
     * 도서관 통합정보 조회
     *
     * @param pageNo 페이지 번호
     * @param pageSize 페이지 크기
     * @return 도서관 통합정보 목록
     */
    @Cacheable(value = "libraryExtendedInfo",
        key = "'page:' + #pageNo + ':size:' + #pageSize",
        unless = "#result.isEmpty()")
    public List<LibraryExtendedInfo> getLibraryExtendedInfo(int pageNo, int pageSize) {
        log.info("[도서관정보나루] 도서관 통합정보 조회: pageNo={}, pageSize={}", pageNo, pageSize);

        try {
            String url = String.format("%s/extends/libSrch?authKey=%s&pageNo=%d&pageSize=%d&format=json",
                properties.getUrl(), properties.getKey(), pageNo, pageSize);

            String response = createRestClient().get()
                .uri(url)
                .retrieve()
                .body(String.class);

            return parseLibraryExtendedInfoResponse(response);

        } catch (RestClientException e) {
            log.error("[도서관정보나루] API 호출 실패", e);
            return List.of();
        }
    }

    /**
     * 도서관 통합정보 응답 파싱
     */
    private List<LibraryExtendedInfo> parseLibraryExtendedInfoResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);

            JsonNode errorNode = root.path("response").path("error");
            if (errorNode.isTextual() && !errorNode.asText().isEmpty()) {
                log.warn("[도서관정보나루] API 에러 응답: {}", errorNode.asText());
                return List.of();
            }

            JsonNode libs = root.path("response").path("libs");
            if (!libs.isArray()) {
                return List.of();
            }

            List<LibraryExtendedInfo> result = new ArrayList<>();
            for (JsonNode libNode : libs) {
                JsonNode lib = libNode.path("lib");

                // 도서관 기본 정보
                JsonNode libInfo = lib.path("libInfo");
                String libCode = libInfo.path("libCode").asText("");
                String libName = libInfo.path("libName").asText("");
                String address = libInfo.path("address").asText("");
                String tel = libInfo.path("tel").asText("");
                String fax = libInfo.path("fax").asText("");
                String latitude = libInfo.path("latitude").asText("");
                String longitude = libInfo.path("longitude").asText("");
                String homepage = libInfo.path("homepage").asText("");
                String closed = libInfo.path("closed").asText("");
                String operatingTime = libInfo.path("operatingTime").asText("");
                int bookCount = libInfo.path("BookCount").asInt(0);

                // 시간대별 대출/반납
                List<LibraryExtendedInfo.HourlyLoanInfo> loanByHours = new ArrayList<>();
                JsonNode hoursNode = lib.path("loanByhours");
                if (hoursNode.isArray()) {
                    for (JsonNode hourNode : hoursNode) {
                        JsonNode hourResult = hourNode.path("result");
                        String hour = hourResult.path("hour").asText("");
                        int loan = hourResult.path("loan").asInt(0);
                        int returnCount = hourResult.path("return").asInt(0);
                        loanByHours.add(LibraryExtendedInfo.HourlyLoanInfo.builder()
                            .hour(hour)
                            .loan(loan)
                            .returnCount(returnCount)
                            .build());
                    }
                }

                // 요일별 대출/반납
                List<LibraryExtendedInfo.WeeklyLoanInfo> loanByDayOfWeek = new ArrayList<>();
                JsonNode daysNode = lib.path("loanByDayofWeek");
                if (daysNode.isArray()) {
                    for (JsonNode dayNode : daysNode) {
                        JsonNode dayResult = dayNode.path("result");
                        String dayOfWeek = dayResult.path("dayOfWeek").asText("");
                        int loan = dayResult.path("loan").asInt(0);
                        int returnCount = dayResult.path("return").asInt(0);
                        loanByDayOfWeek.add(LibraryExtendedInfo.WeeklyLoanInfo.builder()
                            .dayOfWeek(dayOfWeek)
                            .loan(loan)
                            .returnCount(returnCount)
                            .build());
                    }
                }

                // 신착 도서
                List<LibraryExtendedInfo.NewBookInfo> newBooks = new ArrayList<>();
                JsonNode newBooksNode = lib.path("newBooks");
                if (newBooksNode.isArray()) {
                    for (JsonNode bookNode : newBooksNode) {
                        JsonNode book = bookNode.path("book");
                        String bookName = book.path("bookname").asText("");
                        String authors = book.path("authors").asText("");
                        String publisher = book.path("publisher").asText("");
                        String publicationYear = book.path("publication_year").asText("");
                        String isbn13 = book.path("isbn13").asText("");
                        String imageUrl = book.path("bookImageURL").asText("");
                        String regDate = book.path("reg_date").asText("");

                        if (!bookName.isEmpty()) {
                            newBooks.add(LibraryExtendedInfo.NewBookInfo.builder()
                                .bookName(bookName)
                                .authors(authors)
                                .publisher(publisher)
                                .publicationYear(publicationYear)
                                .isbn13(isbn13)
                                .imageUrl(imageUrl)
                                .regDate(regDate)
                                .build());
                        }
                    }
                }

                if (!libCode.isEmpty()) {
                    result.add(LibraryExtendedInfo.builder()
                        .libCode(libCode)
                        .libName(libName)
                        .address(address)
                        .tel(tel)
                        .fax(fax)
                        .latitude(latitude)
                        .longitude(longitude)
                        .homepage(homepage)
                        .closed(closed)
                        .operatingTime(operatingTime)
                        .bookCount(bookCount)
                        .loanByHours(loanByHours)
                        .loanByDayOfWeek(loanByDayOfWeek)
                        .newBooks(newBooks)
                        .build());
                }
            }

            log.info("[도서관정보나루] 도서관 통합정보 파싱 완료: {}개", result.size());
            return result;

        } catch (Exception e) {
            log.error("[도서관정보나루] 도서관 통합정보 응답 파싱 실패", e);
            return List.of();
        }
    }

    // ============================================================================
    // extends/loanItemSrchByLib (연령대별 인기대출도서 통합) API
    // ============================================================================

    /**
     * 도서관별 연령대별 인기대출도서 통합 조회
     *
     * @param libCode 도서관 코드
     * @return 연령대별 인기대출도서 통합 정보
     */
    @Cacheable(value = "extendedPopularBooks", key = "#libCode",
        unless = "#result == null or #result.loanBooks().isEmpty()")
    public ExtendedPopularBooksInfo getExtendedPopularBooks(String libCode) {
        log.info("[도서관정보나루] 연령대별 인기대출도서 통합 조회: libCode={}", libCode);

        try {
            String url = String.format("%s/extends/loanItemSrchByLib?authKey=%s&libCode=%s&format=json",
                properties.getUrl(), properties.getKey(), libCode);

            String response = createRestClient().get()
                .uri(url)
                .retrieve()
                .body(String.class);

            return parseExtendedPopularBooksResponse(response);

        } catch (RestClientException e) {
            log.error("[도서관정보나루] API 호출 실패: libCode={}", libCode, e);
            return null;
        }
    }

    /**
     * 연령대별 인기대출도서 통합 응답 파싱
     */
    private ExtendedPopularBooksInfo parseExtendedPopularBooksResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode resp = root.path("response");

            return ExtendedPopularBooksInfo.builder()
                .loanBooks(parsePopularBooksList(resp.path("loanBooks")))
                .age0Books(parsePopularBooksList(resp.path("age0Books")))
                .age6Books(parsePopularBooksList(resp.path("age6Books")))
                .age8Books(parsePopularBooksList(resp.path("age8Books")))
                .age14Books(parsePopularBooksList(resp.path("age14Books")))
                .age20Books(parsePopularBooksList(resp.path("age20Books")))
                .build();

        } catch (Exception e) {
            log.error("[도서관정보나루] 연령대별 인기대출도서 통합 응답 파싱 실패", e);
            return null;
        }
    }

    /**
     * 인기대출도서 목록 파싱 헬퍼 메서드
     */
    private List<PopularBookInfo> parsePopularBooksList(JsonNode booksNode) {
        if (!booksNode.isArray()) {
            return List.of();
        }

        List<PopularBookInfo> result = new ArrayList<>();
        for (JsonNode bookWrapper : booksNode) {
            JsonNode doc = bookWrapper.path("book");
            String bookName = doc.path("bookname").asText("");
            String isbn13 = doc.path("isbn13").asText("");

            if (!bookName.isEmpty()) {
                result.add(PopularBookInfo.builder()
                    .isbn13(isbn13)
                    .bookName(bookName)
                    .authors(doc.path("authors").asText(""))
                    .publisher(doc.path("publisher").asText(""))
                    .publicationYear(doc.path("publication_year").asText(""))
                    .imageUrl(doc.path("bookImageURL").asText(""))
                    .loanCount(0)
                    .ranking(doc.path("ranking").asText(""))
                    .build());
            }
        }
        return result;
    }
}
