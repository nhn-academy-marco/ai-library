# 도서관 정보나루 API 클라이언트 리팩터링 계획

> 상태: **계획 단계 (미적용)**. 아래의 분리 클래스는 아직 만들지 않았습니다. 현재 코드는 `LibraryInfoNaruApiClient` 한 클래스로 동작합니다.

## 현재 구조

- 파일: `src/main/java/net/java21/library/external/opennaru/client/LibraryInfoNaruApiClient.java`
- 크기: 약 1,490줄, 공개 메서드 28개
- 한 클래스가 다음 책임을 모두 가집니다.
  - 도서 검색 (제목, ISBN-13, ISBN-10, 저자, 출판사)
  - 도서관 검색 (전체, 지역, 이름)
  - 대출·소장 조회 (ISBN별, 지역별, 도서관별, 소장 여부)
  - 도서 상세·분석 (상세, 키워드, 이용 분석, 이용 추이)
  - 인기·트렌드 (인기 대출, 도서관별 인기, 대출 급상승, 월간 키워드, 독서량)
  - 추천 (일반, 마니아, 다독자)
  - 확장 API (도서관 확장 정보, 확장 인기 도서)
  - 공통 처리 (RestClient 생성, URL 인코딩, 응답 파싱, 오류 처리, 로깅)

이 클라이언트는 Step 7의 Tool Calling(`LibraryFunctions`, `LibraryTools`, `InternalBookTools`, `LoanCheckFunction`), `ai.agent` 패키지의 Agent 7개, 표지 보강(`BookCoverService`)이 사용합니다.

## 왜 나누려고 하는가

- 하나의 API 영역을 고쳐도 1,490줄 파일 전체를 읽어야 합니다.
- 응답 파싱 로직이 메서드마다 섞여 있어 단위 테스트를 작게 쓰기 어렵습니다.
- 공통 처리(오류, 로깅, 인코딩)가 중복되어 있어 정책을 바꾸면 여러 곳을 고쳐야 합니다.

## 목표 구조

| 클래스 | 책임 | 옮길 메서드 |
|---|---|---|
| `LibraryApiBaseClient` (추상) | RestClient 생성, URL 인코딩, 오류 응답 처리, 로깅 | 공통 private 메서드 |
| `LibraryBookSearchClient` | 도서 검색 | `searchBooksByTitle`, `searchBooksByIsbn`, `searchBooksByIsbn10`, `searchBooksByAuthor`, `searchBooksByPublisher` |
| `LibraryLibrarySearchClient` | 도서관 검색 | `searchLibraries`, `searchAllLibraries`, `searchLibrariesByRegion`, `searchLibrariesByName` |
| `LibraryLoanInfoClient` | 대출·소장 조회 | `searchLoanItemsByIsbn`(3종), `searchLoanItemsByLibrary`, `checkBookExists` |
| `LibraryBookDetailClient` | 도서 상세·분석 | `getBookDetail`, `getBookKeywords`, `getUsageAnalysis`, `getUsageTrend` |
| `LibraryPopularTrendClient` | 인기·트렌드 | `getPopularBooks`, `getPopularBooksByLibrary`, `getHotTrendBooks`, `getMonthlyKeywords`, `getReadingQuantity` |
| `LibraryRecommendationClient` | 추천 | `getRecommendedBooks`, `getRecommendedBooksForMania`, `getRecommendedBooksForReader` |
| `LibraryExtendedApiClient` | 확장 API | `getLibraryExtendedInfo`, `getExtendedPopularBooks` |

응답 파싱은 API 영역별 파서(예: `BookSearchResponseParser`)로 분리해, 실제 API 응답 JSON 샘플로 단위 테스트합니다.

## 진행 순서

1. 기반 클래스와 파서 구조를 만들고, 호출이 가장 많은 도서 검색·도서관 검색·대출 조회 클라이언트부터 분리합니다.
2. 나머지 영역(상세, 인기·트렌드, 추천, 확장)을 분리합니다.
3. 기존 `LibraryInfoNaruApiClient`는 새 클라이언트에 위임하는 퍼사드로 바꾸고 `@Deprecated`를 붙여, 사용하는 쪽 코드를 한 번에 바꾸지 않아도 되게 합니다.
4. 위의 사용처가 새 클라이언트를 직접 쓰도록 옮긴 뒤 퍼사드를 제거합니다.

각 단계마다 기존 테스트(`LibraryInfoNaruApiClientTest`, `LibraryInfoNaruApiClientMockTest` 등)가 계속 통과해야 합니다.

## 사용 예 (목표)

현재:

```java
private final LibraryInfoNaruApiClient apiClient;

public List<LibraryBookInfo> searchBooks(String title) {
    return apiClient.searchBooksByTitle(title);
}
```

분리 후:

```java
private final LibraryBookSearchClient bookSearchClient;

public List<LibraryBookInfo> searchBooks(String title) {
    return bookSearchClient.searchBooksByTitle(title);
}
```

## 적용할 설계 원칙

- 단일 책임 원칙: 클래스 하나가 API 영역 하나만 담당합니다.
- 템플릿 메서드 패턴: 공통 호출·오류 처리는 `LibraryApiBaseClient`에 둡니다.
- 퍼사드 패턴: 전환 기간 동안 기존 클라이언트를 퍼사드로 남겨 호환성을 유지합니다.
