# Step 0. 시작하기 전에

> 본격적인 구현에 앞서 이 교재 전체에서 쓰는 AI 용어를 익히고, 프로젝트를 내 컴퓨터에서 실행할 수 있는 환경을 갖춥니다.

## 이 단계에서 만드는 것

Step 0에서는 새 기능을 만들지 않습니다. 대신 다음 두 가지를 준비합니다.

1. **개념 지도**: LLM, 토큰, 임베딩, 벡터 검색, RAG, Tool Calling 같은 용어가 무엇이고, 이 교재의 어느 Step에서 직접 구현하는지 한눈에 봅니다.
2. **실행 환경**: JDK 21, Maven, PostgreSQL + pgvector, LLM·임베딩 접속 정보를 준비하고, 애플리케이션을 실행해 브라우저에서 검색 화면을 띄웁니다.

이 단계를 마치면 다음과 같은 상태가 됩니다.

```text
$ mvn spring-boot:run
...
Tomcat started on port 8080 (http) with context path '/'
Started AiLibraryApplication in 9.8 seconds
```

브라우저에서 `http://localhost:8080`에 접속하면 도서 검색 화면이 열립니다.

## 왜 이 단계가 필요한가

AI 백엔드 개발에는 기존 웹 개발에서 보지 못한 용어가 한꺼번에 등장합니다. "임베딩한 벡터를 pgvector에 넣고 코사인 유사도로 Top-K를 뽑아 RAG 컨텍스트를 만든다"는 문장을 처음 보면 무엇부터 찾아봐야 할지 막막합니다. Step 0에서 용어의 큰 그림을 먼저 잡아 두면, 이후 단계에서 각 용어를 만났을 때 "아, 그때 본 그 개념이구나" 하고 코드에 집중할 수 있습니다.

또한 이 프로젝트는 데이터베이스, LLM, 임베딩 서버, 외부 Open API 등 여러 외부 시스템에 연결됩니다. 환경 설정에서 막히면 학습을 시작하기도 전에 지치기 쉬우므로, 필요한 준비물을 한 문서에 모아 순서대로 점검합니다.

## 문서 구성

| 순서 | 문서 | 내용 | 예상 시간 |
|---|---|---|---|
| 01 | [AI 백엔드 개발을 위한 기초 개념](01.ai-basics.md) | LLM과 토큰, 프롬프트, Temperature, 환각, 임베딩과 벡터, 코사인 유사도, 벡터 데이터베이스, RAG, Tool Calling, MCP, Agent, Spring AI | 1.5시간 |
| 02 | [개발 환경 준비](02.environment-setup.md) | JDK 21, Maven, Docker로 PostgreSQL + pgvector 실행, `secrets.properties`, LLM·임베딩 설정, 도서관 정보나루 API 키, 실행 확인 | 1~2시간 |

처음 보는 용어는 언제든 [용어집](../glossary.md)에서 찾아볼 수 있습니다.

## 이 단계를 마치면

- [ ] LLM, 토큰, 임베딩, 벡터 검색, RAG, Tool Calling을 각각 한두 문장으로 설명할 수 있다.
- [ ] 각 개념을 이 교재의 몇 번째 Step에서 구현하는지 말할 수 있다.
- [ ] Spring AI의 `ChatClient`와 `EmbeddingModel`이 무엇을 추상화하는지 설명할 수 있다.
- [ ] `secrets.properties`를 만들고 필요한 키를 채웠다(실제 값은 커밋하지 않는다).
- [ ] `mvn spring-boot:run`으로 애플리케이션을 실행하고 `http://localhost:8080`에서 검색 화면을 확인했다.

## 관련 소스 코드

| 클래스·파일 | 역할 | 경로 |
|---|---|---|
| `AiConfig` | `spring.ai.selected-model` 값으로 사용할 `ChatModel`(Gemini 또는 Ollama)을 고르고 `ChatClient` 빈을 만듦 | `src/main/java/net/java21/library/core/config/AiConfig.java` |
| `EmbeddingService` | `EmbeddingModel`로 텍스트를 1024차원 벡터로 변환 | `src/main/java/net/java21/library/core/book/service/embedding/EmbeddingService.java` |
| `application.properties` | DB, Spring AI, 도서관 정보나루 API, 캐시 등 기본 설정 | `src/main/resources/application.properties` |
| `application-dev.properties` | 기본 활성 프로필(`dev`) 설정: Telegram Bot 비활성화, Redis·RabbitMQ 설정 가져오기 | `src/main/resources/application-dev.properties` |
| `secrets.properties.example` | 비밀값 템플릿(복사해서 `secrets.properties`로 사용) | `secrets.properties.example` |
