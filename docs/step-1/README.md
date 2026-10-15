# Step 1. 데이터 구축과 전통적 검색

> AI를 쓰기 전에, 공공 도서 데이터 15만여 건을 데이터베이스에 넣고 키워드로 찾는 "전통적인 검색 서비스"를 먼저 완성합니다.

## 이 단계에서 만드는 것

Step 1을 마치면 브라우저에서 `http://localhost:8080/?searchType=KEYWORD&keyword=자바`에 접속해 다음과 같은 화면을 볼 수 있습니다.

```text
도서 검색
[검색 방식: 키워드 검색 v] [ISBN      ] [키워드: 자바                ] [검색]

총 1,234권의 도서가 검색되었습니다.          검색 소요 시간: 0.084s

+------------------+  +------------------+  +------------------+
| (표지 이미지)     |  | (표지 이미지)     |  | (표지 이미지)     |
| 도서 제목         |  | 도서 제목         |  | ...              |
| 저자명            |  | 저자명            |  |                  |
| 출판사 / ISBN     |  | 출판사 / ISBN     |  |                  |
+------------------+  +------------------+  +------------------+

                 prev  1  2  3  4  5  6  7  8  9  10  next
```

(검색 건수와 소요 시간은 예시이며 환경에 따라 다릅니다.)

여기에는 아직 AI가 전혀 등장하지 않습니다. 이 단계의 검색은 "사용자가 입력한 글자가 데이터에 그대로 들어 있는가"만 확인합니다.

```mermaid
flowchart LR
    CSV["공공 도서 CSV<br/>약 15만 건"] -->|"01. 배치 적재"| DB[("PostgreSQL<br/>books 테이블")]
    DB -->|"02. LIKE 검색<br/>QueryDSL"| SVC["검색 서비스"]
    SVC -->|"03. Thymeleaf"| UI["검색 화면"]
    DB -.->|"04. 인덱스와<br/>전문 검색"| SVC
```

## 왜 이 단계가 필요한가

AI 검색, RAG, 챗봇은 모두 "검색할 데이터"가 있어야 동작합니다. 그리고 AI 검색이 무엇을 더 잘하는지 알려면, 먼저 AI 없이 만든 검색이 어디까지 되고 어디서 막히는지 직접 겪어 봐야 합니다.

- Step 0에서는 AI와 LLM이 무엇인지, 개발 환경을 어떻게 준비하는지 살펴봤습니다.
- Step 1에서는 그 환경 위에 데이터와 키워드 검색을 만듭니다. 이것이 이후 모든 단계의 기준선(baseline)이 됩니다.
- Step 2에서는 이 검색으로 "추운 겨울에 읽기 좋은 따뜻한 소설"처럼 자연어로 물었을 때 왜 원하는 결과가 나오지 않는지 확인하고, 임베딩(Embedding)과 벡터 검색으로 넘어갑니다.

즉 이 단계의 목표는 "잘 동작하는 키워드 검색"을 만드는 것과 함께 "키워드 검색이 할 수 없는 일"을 눈으로 확인할 재료를 준비하는 것입니다.

## 문서 구성

| 순서 | 문서 | 내용 | 예상 시간 |
|---|---|---|---|
| 01 | [01. 데이터 적재: 공공 도서 CSV 배치 적재](01.csv-batch-load.md) | CSV 파싱, 이벤트 기반 구조, `saveAll()` 배치 저장 | 2시간 |
| 02 | [02. 기본 검색: LIKE 기반 동적 쿼리 구현](02.basic-search-implementation.md) | 요청·응답 DTO, QueryDSL `BooleanBuilder`, 페이징 | 2시간 |
| 03 | [03. 검색 UI: Thymeleaf와 Tabler 기반 화면 구축](03.search-ui.md) | MVC 흐름, 레이아웃, 검색 폼, 결과 카드, 페이징 프래그먼트 | 2시간 |
| 04 | [04. 성능 최적화: 인덱싱과 전문 검색(Full Text Search)](04.search-optimization.md) | B-Tree·GIN 인덱스, `EXPLAIN ANALYZE`, PostgreSQL 전문 검색, 키워드 검색의 한계 | 2시간 |

## 이 단계를 마치면

- [ ] `init.enable=true`로 애플리케이션을 실행해 `books` 테이블에 도서 데이터를 적재했다
- [ ] 키워드(제목·저자·출판사·부제·권차명)와 ISBN으로 도서를 검색할 수 있다
- [ ] 검색 화면에서 결과 카드와 페이지 이동을 확인했다
- [ ] `EXPLAIN ANALYZE`로 `LIKE '%자바%'`가 전체 테이블을 훑는다는 것을 확인했다
- [ ] 키워드 검색으로는 찾기 어려운 질문의 예를 두 가지 이상 들 수 있다

## 관련 소스 코드

| 클래스 | 역할 | 경로 |
|---|---|---|
| `CsvBookParser` | CSV를 한 줄씩 읽어 `BookParsedEvent` 발행 | `src/main/java/net/java21/library/batch/init/parser/impl/CsvBookParser.java` |
| `BookParsingEventListener` | 이벤트를 모아 두었다가 배치 저장 호출 | `src/main/java/net/java21/library/batch/init/event/listener/BookParsingEventListener.java` |
| `BookBatchService` | `batchSize` 단위로 `saveAll()` 실행 | `src/main/java/net/java21/library/batch/init/service/BookBatchService.java` |
| `Book` | `books` 테이블 엔티티 | `src/main/java/net/java21/library/core/book/domain/Book.java` |
| `BookSearchRequest`, `BookSearchResponse` | 검색 요청·응답 DTO | `src/main/java/net/java21/library/core/book/dto/` |
| `BookRepositoryImpl` | QueryDSL 동적 검색 쿼리 | `src/main/java/net/java21/library/core/book/repository/impl/BookRepositoryImpl.java` |
| `KeywordSearchStrategy` | 키워드 검색 전략 | `src/main/java/net/java21/library/core/book/service/search/strategy/KeywordSearchStrategy.java` |
| `BookSearchController` | 검색 화면 컨트롤러 | `src/main/java/net/java21/library/front/web/BookSearchController.java` |
| `PostgreSQLFunctionContributor` | 전문 검색 함수 `ts_match_korean` 등록 | `src/main/java/net/java21/library/core/config/PostgreSQLFunctionContributor.java` |
| 템플릿 | 레이아웃, 검색 화면, 페이징 | `src/main/resources/templates/` |

---

이전 단계: [Step 0 개요](../step-0/README.md)

## 다음 문서

[01. 데이터 적재: 공공 도서 CSV 배치 적재 →](01.csv-batch-load.md)
