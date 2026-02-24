# Project Guidelines

본 문서는 Spring 기반 프로젝트에서 공통적으로 적용할 수 있는  
코드 작성, 테스트, 아키텍처, 운영 전반에 대한 가이드라인을 정의합니다.

---

## 공통 Guidelines

* 모든 문서는 **맞춤법과 띄어쓰기**를 준수합니다.
* 문장은 간결하고 명확하게 작성합니다.
* 불필요한 중복 표현과 모호한 표현을 지양합니다.

---

## 용어 사용 Guidelines

* 가능한 경우 **공식 용어**를 사용합니다.
* 영어 표현이 더 적절하거나 일반적으로 사용되는 경우 **영어 원문을 그대로 사용**합니다.
* 한 번 선택한 용어는 모든 문서, 코드, 주석에서 **일관되게 사용**합니다.
* 축약어는 최초 1회 전체 용어를 함께 표기합니다.

---

## 코드 작성 공통 Guidelines

### 패키지 및 계층 구조

* 도메인(관심사)은 상위 패키지 레벨에서 명확히 분리합니다.
  * 예시: `core.book`, `core.review`
* 서비스 계층이 복잡한 경우 기능 단위의 하위 패키지를 구성합니다.
  * 예시: `search`, `ai`, `embedding`, `cache`
* 테스트 코드의 패키지 구조는 메인 소스 코드와 **반드시 동일**하게 유지합니다.

---

### 디자인 패턴 및 구현 원칙

* 분기 조건이나 정책 변경 가능성이 있는 로직은  
  **전략 패턴(Strategy Pattern)**을 적용하여 구현체를 분리합니다.
* 계층 간 데이터 전달을 위한 **DTO는 독립된 클래스**로 정의합니다.
* 단순 위임만 수행하는 불필요한 서비스 계층은 제거합니다.

---

### 상수(Constants)

* 의미 있는 값(매직 넘버, 문자열 등)은 반드시 **상수(Constant)**로 정의합니다.
* 상수 이름은 **대문자 + 언더스코어(`UPPER_SNAKE_CASE`)** 규칙을 따릅니다.
* 상수는 관련 클래스 내부 또는 별도의 Constant Class로 관리합니다.

---

### 주석(Comment)

* 공개 API 또는 복잡한 로직에는 **Javadoc** 작성을 권장합니다.
* 의미가 직관적이지 않은 로직이나 값은 주석으로 설명합니다.
* RRF, Map-Reduce 등 **복잡한 알고리즘**은 처리 흐름을 설명하는 주석을 포함합니다.

---

### 기타 코드 품질 규칙

* 사용하지 않는 import는 반드시 제거합니다.
* 코드 스타일과 포맷은 프로젝트 기본 설정을 따릅니다.

---

## Backend 작성 Guidelines

### Spring Boot

* 모듈 단위 구조를 명확히 정의하고 패키지 규칙을 준수합니다.
* **Controller → Service → Repository** 계층 구조를 유지합니다.

---

### REST API 설계

* HTTP Status Code는 표준 정의에 맞게 반환합니다.
* 요청(Request)과 응답(Response)은 반드시 **DTO**로 정의합니다.
  * 요청 DTO 예시: `BookSearchRequest`
  * 응답 DTO 예시: `BookSearchResponse`
* 모든 Request DTO에 대해 **Validation 처리**를 수행합니다.

---

### 예외 처리(Exception Handling)

* **Global Exception Handler**를 사용합니다.
* 모든 커스텀 예외는 `RuntimeException`을 상속합니다.
* 예외 클래스는 도메인 하위 `exception` 패키지에서 관리합니다.
  * 예시: `com.example.book.exception`
* 의미 있는 메시지와 적절한 HTTP 상태 코드를 반환합니다.

---

### 로깅(Logging)

* `@Slf4j` 어노테이션을 사용합니다.
* 로깅 정책은 `logback-spring.xml`에서 관리합니다.
* 목적에 따라 로그 레벨을 명확히 구분합니다.
  * `debug`: 디버깅
  * `info`: 주요 흐름
  * `error`: 오류 상황

---

### AI 및 LLM 활용 Guidelines

* **어조**
  * 사용자 응답은 항상 공손한 어투(`~입니다`, `~하세요`)를 사용합니다.
* **불확실성 처리**
  * 근거 없는 추측을 하지 않습니다.
  * `"추천 사유를 명확히 알 수 없습니다"`와 같이 명시적으로 표현합니다.
* **응답 포맷**
  * JSON 응답이 필요한 경우 **순수 JSON**만 반환합니다.
* **의미적 캐싱(Semantic Caching)**
  * 벡터 유사도 기반 캐시를 사용하며 임계값과 TTL을 명확히 정의합니다.
* **명시적 흐름 제어**
  * `isWarmUp` 등 명시적인 플래그를 사용합니다.

---

### JPA DDL Auto 전략 규칙 (테스트 환경 포함)

* `create`, `create-drop`, `update` 전략은 **절대 사용하지 않습니다.**
* JPA를 통한 스키마 자동 생성 및 변경을 금지합니다.
* DB 스키마 관리는 **SQL 스크립트 또는 마이그레이션 도구(Flyway, Liquibase)**로 수행합니다.
* `ddl-auto` 값은 `validate`만 허용합니다.

---

## Test Code Guidelines

### 테스트 기본 원칙

* 모든 신규 기능과 변경 사항에는 **테스트 코드 작성을 원칙**으로 합니다.
* 테스트는 가능한 한 **단위 테스트(Unit Test)** 위주로 작성합니다.
* 테스트는 빠르고 독립적이며 반복 실행 가능해야 합니다.

---

### 테스트 수준(Test Level)

#### 1. 단위 테스트 (Unit Test) — 최우선

* 대상
  * Service, Domain, Policy, Strategy, Util
* 특징
  * Spring Context 로딩 없음
  * 외부 의존성은 Mock 처리
* 사용 도구
  * JUnit 5
  * Mockito (`@Mock`, `@InjectMocks`)

> 전체 테스트의 70~80% 이상을 단위 테스트로 구성하는 것을 권장합니다.

---

#### 2. 슬라이스 테스트 (Slice Test)

* Controller: `@WebMvcTest`
* Repository: `@DataJpaTest`
* 특정 계층의 동작만 검증합니다.
* 외부 API 테스트는 단위테스트로 진행한다. 
  * 별도로 통합테스트가 필요한 API는 해당 API만 별도의 테스트 클레스로 분리해서 테스트 코드를 작성한다.


---

#### 3. 통합 테스트 (Integration Test)

* `@SpringBootTest` 사용
* 여러 계층의 연동을 검증합니다.
* 반드시 필요한 경우에만 최소한으로 작성합니다.

---

### Spring Test 사용 규칙

* Spring Boot 3.4 이상에서는  
  **`@MockBean`, `@SpyBean` 사용을 금지**합니다.
* Spring Context 기반 테스트에서는
  * `@MockitoBean`
  * `@MockitoSpyBean`
    을 사용합니다.
* 순수 단위 테스트에서는
  * `@Mock`
  * `@Spy`
    를 사용합니다.

---

### 테스트 대상 범위 가이드

| 계층 | 권장 테스트 |
|----|-----------|
| Controller | Slice Test |
| Service | Unit Test |
| Domain / Policy | Unit Test |
| Repository | Slice Test |
| External Client | Unit Test (Mock 기반) |

---

### 테스트 커버리지 기준

* **전체 테스트 커버리지는 60% 이상을 목표**로 합니다.
* 커버리지는 수치 자체보다 **핵심 비즈니스 로직 보호**를 목적으로 합니다.
* 신규 코드 및 변경 코드에는 반드시 테스트가 포함되어야 합니다.

---

### 테스트 네이밍 및 가독성

* `@DisplayName`을 사용하여 테스트 목적을 명확히 표현합니다.
* 하나의 테스트는 **하나의 행위만 검증**합니다.

---

### 테스트에서 지양할 사항

* 불필요한 `@SpringBootTest` 사용
* 테스트 간 실행 순서 의존
* 실제 외부 시스템에 의존하는 테스트
* 하나의 테스트에서 여러 시나리오 검증

---

## Frontend 작성 Guidelines

### UI Framework

* **Tabler (Bootstrap 5 기반)**를 사용합니다.

---

### Layout 구조

* 모든 페이지는 **Thymeleaf** 기반으로 작성합니다.

---

### Layout 활용

* `thymeleaf-layout-dialect` 라이브러리를 사용합니다.
* 공통 레이아웃을 재사용하고, 페이지별로 콘텐츠 영역만 구현합니다.
