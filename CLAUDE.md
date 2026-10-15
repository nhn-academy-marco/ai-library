# CLAUDE.md

공공 도서 데이터로 키워드 검색 → 벡터 검색 → RAG → Tool Calling까지 단계별로 구현한 Spring AI 프로젝트이자 교재입니다. 교재는 `docs/`(목차: `docs/index.md`), 코드는 `src/`에 있습니다.

## 빌드·테스트

- Java 21, Spring Boot 3.5, Spring AI 1.1.2, Maven (wrapper 없음 → `mvn` 사용)
- 빌드: `mvn -q clean compile test-compile` (QueryDSL Q클래스가 `target/generated-sources`에 생성되므로 패키지 변경 후에는 `clean` 필요)
- 테스트: `mvn test` 또는 `mvn test -Dtest='클래스명,...'`. LLM(Gemini/Ollama)·외부 API를 실제로 호출하는 테스트는 키와 네트워크가 있어야 통과합니다.
- DB·RabbitMQ를 쓰는 Spring Context 테스트는 Testcontainers로 일회용 컨테이너(pgvector/pgvector:pg16, rabbitmq:3.13)에 연결됩니다. `support/TestcontainersInitializer`가 `src/test/resources/META-INF/spring.factories`로 모든 `@SpringBootTest`에 자동 적용되므로 Docker가 실행 중이어야 합니다. 테스트 설정에 원격 DB·운영 RabbitMQ 주소를 넣지 않습니다(2026-10-15 원격 `library_dev` 스키마가 `create-drop` 테스트로 지워진 사고가 있었음).
- 실제 외부 API를 호출하는 테스트(`LibraryInfoNaruApiCallTest`)는 `LIBRARY_API_KEY`가 있어야 통과합니다.

## 비밀값

- 비밀값은 커밋하지 않습니다. 설정 파일에는 `${ENV_VAR:}` 플레이스홀더만 둡니다.
- 로컬: 프로젝트 루트 `secrets.properties`(gitignore, 템플릿 `secrets.properties.example`)
- 서버: GitHub Actions가 Secrets로 `~/library/secrets.properties`를 생성합니다(`.github/workflows/maven.yml`).
- 키 목록: `DB_PASSWORD`, `GEMINI_API_KEY`, `LIBRARY_API_KEY`, `RABBITMQ_PASSWORD`, `REDIS_PASSWORD`, `TELEGRAM_BOT_TOKEN`
- 문서·테스트 코드에도 실제 값을 넣지 않습니다(테스트는 `TestApiKeys` 사용).

## 아키텍처 요약

패키지 루트: `net.java21.library`

| 패키지 | 역할 |
|---|---|
| `batch.init` | 공공 도서 CSV 파싱·적재 (이벤트 기반) |
| `batch.embedding` | BGE-M3 임베딩 생성 스케줄러 |
| `core.book.service.search` | `BookSearchService` + `SearchStrategy`(Keyword/Vector/Hybrid/RAG), `RrfService`(k=60) |
| `core.book.service.cache` | 시맨틱 캐시(코사인 유사도 0.98, TTL 30분, 인메모리 최대 1,000건) |
| `core.book.service.personalization` | 피드백 기반 선호 벡터(`UserPreferenceVectorCalculator`에서 `@Cacheable`) |
| `core.review` | 리뷰, Map-Reduce 요약, RabbitMQ 큐(재시도 3회 후 DLQ) |
| `ai.function` | `@Tool` 도구: `LibraryTools`(8개), `InternalBookTools`(3개) |
| `ai.agent.orchestrator` | `ToolBasedAiOrchestrationService`(`ChatClient.defaultTools()`) |
| `external.opennaru` | 도서관정보나루 Open API 클라이언트 |
| `external.telegram` | Telegram Bot, 피드백 수집 |
| `front.web` | Thymeleaf 웹 컨트롤러 |

- LLM은 `spring.ai.selected-model`(gemini | ollama)로 `@Primary ChatModel`을 고릅니다(`AiConfig`).
- 이 프로젝트의 오케스트레이션은 단일 LLM이 여러 Tool을 조합하는 방식입니다. A2A(Agent-to-Agent)로 부르지 않습니다.

## 코드 규칙

- 도메인은 상위 패키지로 나누고(`core.book`, `core.review`), 복잡한 서비스는 기능별 하위 패키지(`search`, `cache`, `embedding`)로 구성합니다. 테스트 패키지 구조는 메인과 같게 유지합니다.
- 정책이 바뀔 수 있는 분기는 전략 패턴으로 분리합니다. 단순 위임만 하는 서비스 계층은 만들지 않습니다.
- Controller → Service → Repository 계층을 지킵니다. 요청·응답은 DTO로 정의하고 Request DTO는 Validation을 적용합니다.
- 매직 넘버·문자열은 `UPPER_SNAKE_CASE` 상수로 정의합니다.
- 예외는 `RuntimeException`을 상속하고 도메인 하위 `exception` 패키지에 둡니다(예: `net.java21.library.core.book.exception`). 처리는 `GlobalExceptionHandler`에서 합니다.
- 로깅은 `@Slf4j`, 정책은 `logback-spring.xml`. SLF4J 자리 표시자는 `{}`만 사용합니다(`{:.4f}` 불가).
- 같은 클래스 내부 호출은 Spring 프록시를 거치지 않으므로 `@Cacheable`, `@Async`, `@Transactional`이 필요한 메서드는 별도 빈으로 분리합니다.
- 공개 API와 RRF, Map-Reduce 같은 복잡한 알고리즘에는 Javadoc 또는 흐름 설명 주석을 둡니다. 사용하지 않는 import는 제거합니다.
- JPA 스키마 자동 생성은 쓰지 않습니다. 운영·개발 설정의 `ddl-auto`는 `validate`만 허용합니다.

## LLM 응답 규칙

- 사용자 응답은 공손한 합니다체로, 근거 없는 추측은 하지 않습니다("추천 사유를 명확히 알 수 없습니다"처럼 명시).
- JSON이 필요하면 순수 JSON만 반환하게 합니다.
- 시스템 프롬프트에는 실제로 등록한 Tool만 언급합니다.

## 테스트 규칙

- 신규 기능과 변경 사항에는 테스트를 작성합니다. 단위 테스트(JUnit 5 + Mockito, Spring Context 없이)를 우선합니다.
- 계층별 권장: Service·Domain·Strategy·Util·External Client는 단위 테스트, Controller는 `@WebMvcTest`, Repository는 `@DataJpaTest`, `@SpringBootTest`는 꼭 필요할 때만.
- Spring Context 테스트에서는 `@MockitoBean`, `@MockitoSpyBean`을 씁니다(`@MockBean`, `@SpyBean` 금지).
- `@DisplayName`으로 목적을 밝히고, 테스트 하나는 행위 하나만 검증합니다. 테스트 간 순서 의존과 실제 외부 시스템 의존을 피합니다.
- 커버리지 목표는 60% 이상이며, 핵심 비즈니스 로직 보호를 우선합니다.

## Frontend

- Thymeleaf + `thymeleaf-layout-dialect`로 공통 레이아웃(`templates/layouts/default.html`)을 재사용하고, 페이지는 콘텐츠 영역만 구현합니다.
- UI는 Tabler(Bootstrap 5 기반)를 사용합니다.

## 문서(docs) 작성 규칙

- 맞춤법·띄어쓰기를 지키고 본문은 합니다체로 씁니다. 목록·표는 명사형 종결을 허용합니다.
- 용어: 애노테이션, 임베딩, 컨텍스트, 환각(Hallucination), Top-K, 적중률, pgvector, Spring AI, Telegram Bot. 축약어는 처음 한 번 전체 용어를 함께 씁니다.
- 헤딩과 본문에 장식용 이모지를 쓰지 않고, 굵게는 핵심 용어에만 씁니다. 감탄형 문장과 과장 표현("완벽한", "강력한")을 피합니다.
- 코드 예제는 실제 소스와 Spring AI 1.1.2 API에 맞춥니다. 학습용으로 단순화한 예제에는 실제 구현 클래스를 안내합니다.
- 문서 간 "다음 문서" 링크는 `docs/index.md`의 파일 목록과 일치시킵니다.

## 커밋

- 커밋 메시지는 한국어, `type: 요약` 형식(feat, fix, docs, refactor, chore, ci)으로 씁니다.
- 개인 포트폴리오 저장소이므로 커밋·PR에 AI 공동 작성자(Co-Authored-By) 줄을 넣지 않습니다.
