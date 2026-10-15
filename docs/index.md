# Spring AI로 만드는 AI 도서 검색 서비스

> 키워드 검색에서 출발해 벡터 검색, RAG, Tool Calling까지 직접 구현하며 AI 백엔드 개발을 익히는 실습 교재입니다.

- 완성된 서비스: [https://library.java21.net/](https://library.java21.net/)
- 소스 코드: [저장소 루트](../README.md)
- 모르는 용어: [용어집](glossary.md)

## 이 교재는 누구를 위한 것인가

- Java와 Spring Boot(MVC, JPA)로 간단한 웹 서비스를 만들어 본 대학생과 신입 개발자
- LLM, 임베딩, RAG 같은 말은 들어 봤지만 직접 서비스에 붙여 본 적은 없는 사람
- "AI 기능을 붙였다"에서 그치지 않고, 왜 필요한지와 비용·성능 문제까지 설명할 수 있는 포트폴리오를 만들고 싶은 사람

수학이나 머신러닝 지식은 필요하지 않습니다. 필요한 개념은 Step 0과 각 문서에서 비유와 그림으로 설명합니다.

## 이 교재로 만드는 것

공공 도서 데이터 약 15만 건을 담은 도서 검색 서비스를 단계별로 키워 갑니다. 각 단계는 이전 단계의 한계에서 출발합니다.

```mermaid
flowchart LR
    S0["Step 0<br/>개념·환경"] --> S1["Step 1<br/>키워드 검색"]
    S1 -->|"뜻으로는 못 찾음"| S2["Step 2<br/>벡터·하이브리드 검색"]
    S2 -->|"이유를 설명 못 함"| S3["Step 3<br/>RAG 추천"]
    S3 -->|"느리고 비쌈"| S4["Step 4<br/>성능·비용 최적화"]
    S4 -->|"웹 화면만으로는 부족"| S5["Step 5<br/>Telegram Bot·피드백"]
    S5 -->|"모두에게 같은 결과"| S6["Step 6<br/>개인화"]
    S6 -->|"DB 밖의 정보가 필요"| S7["Step 7<br/>Tool Calling"]
    S7 -.->|"보너스"| S8["Step 8<br/>n8n 자동화"]

    classDef step fill:#EEF2FF,stroke:#4F46E5,color:#0F172A
    classDef bonus fill:#F1F5F9,stroke:#64748B,color:#0F172A
    class S0,S1,S2,S3,S4,S5,S6,S7 step
    class S8 bonus
```

## 학습 방법

번호가 붙은 문서는 모두 같은 순서로 구성되어 있습니다.

| 섹션 | 하는 일 |
|---|---|
| 왜 필요한가 | 이전 단계의 한계를 실제 예시로 확인합니다. |
| 핵심 개념 | 비유 → 정의 → 그림 순서로 개념을 잡습니다. |
| 따라 하기 | 실제 소스 코드를 단계별로 구현하고 실행 결과를 확인합니다. |
| 직접 해 보기 | 기본·도전 미션을 풉니다. 정답은 접어 두었습니다. |
| 자주 하는 실수 | 증상 → 원인 → 해결 순서로 막히는 지점을 정리했습니다. |
| 정리 · 확인 문제 | 핵심을 요약하고 스스로 점검합니다. |

권장 순서는 다음과 같습니다.

1. 각 단계의 README(개요)를 먼저 읽고 "이 단계를 마치면" 체크리스트를 확인합니다.
2. 번호 순서대로 문서를 읽으며 따라 하기를 직접 실행합니다. 코드를 복사하기보다 직접 입력하고 로그를 확인하는 편이 훨씬 오래 남습니다.
3. 확인 문제를 풀고, 실제 소스(`src/main/java/net/java21/library/...`)와 비교해 봅니다.

## 커리큘럼

| 단계 | 주제 | 핵심 개념 | 예상 시간 |
|---|---|---|---|
| [Step 0](step-0/README.md) | 시작하기 전에 | LLM, 토큰, 임베딩, RAG, Tool Calling, 개발 환경 | 3시간 |
| [Step 1](step-1/README.md) | 데이터 구축과 전통적 검색 | 이벤트 기반 배치 적재, QueryDSL, 인덱스, 전문 검색 | 8시간 |
| [Step 2](step-2/README.md) | 벡터 검색 | 임베딩, pgvector, 코사인 유사도, RRF 하이브리드 검색 | 8시간 |
| [Step 3](step-3/README.md) | RAG | 환각, 컨텍스트 구성, ChatClient, 프롬프트 | 6시간 |
| [Step 4](step-4/README.md) | 성능 최적화와 리뷰 시스템 | 토큰 비용, Top-K, 시맨틱 캐시, Map-Reduce 요약, RabbitMQ | 8.5시간 |
| [Step 5](step-5/README.md) | Telegram Bot과 피드백 | Bot API, Callback, 피드백 데이터 설계 | 9.5시간 |
| [Step 6](step-6/README.md) | 개인화 추천 | 사용자 선호 벡터, 재정렬, 콜드 스타트, 캐시 | 7시간 |
| [Step 7](step-7/README.md) | Tool Calling 도서관 도우미 | Function Calling, `@Tool`, MCP, 외부 API 연동 | 13시간 |
| [Step 8](step-8/README.md) | n8n 워크플로우 자동화 (보너스) | 노드 기반 자동화, Webhook | 4시간 |

한 학기(15주) 수업이라면 Step 0~1을 2주, Step 2~6을 각 1~2주, Step 7을 2주, 남은 기간을 개인 프로젝트로 배분하는 구성을 권장합니다.

## 전체 문서 목록

### Step 0. 시작하기 전에
- [01. AI 백엔드 개발을 위한 기초 개념](step-0/01.ai-basics.md)
- [02. 개발 환경 준비](step-0/02.environment-setup.md)

### Step 1. 데이터 구축과 전통적 검색
- [01. 데이터 적재: 공공 도서 CSV 배치 적재](step-1/01.csv-batch-load.md)
- [02. 기본 검색: LIKE 기반 동적 쿼리 구현](step-1/02.basic-search-implementation.md)
- [03. 검색 UI: Thymeleaf와 Tabler 기반 화면 구축](step-1/03.search-ui.md)
- [04. 성능 최적화: 인덱싱과 전문 검색](step-1/04.search-optimization.md)

### Step 2. 벡터 검색
- [01. 키워드 검색의 한계 체감하기](step-2/01.search-quality-limitations.md)
- [02. pgvector 설정](step-2/02.pgvector-setup.md)
- [03. 임베딩 생성 배치](step-2/03.embedding-generation.md)
- [04. 자연어 검색](step-2/04.natural-language-search.md)
- [05. 하이브리드 검색](step-2/05.hybrid-search.md)

### Step 3. RAG
- [01. RAG 이해: LLM의 환각을 검색으로 막기](step-3/01.understanding-rag.md)
- [02. 컨텍스트 구축: 검색 결과를 LLM이 읽는 글로 바꾸기](step-3/02.context-construction.md)
- [03. LLM 연동: ChatClient로 호출하고 응답 처리하기](step-3/03.llm-integration.md)

### Step 4. 성능 최적화와 리뷰 시스템
- [01. 성능과 비용](step-4/01.performance-cost.md)
- [02. Top-K 최적화](step-4/02.topk-optimization.md)
- [03. 시맨틱 캐싱](step-4/03.semantic-caching.md)
- [04. 리뷰 요약 시스템](step-4/04.review-summarization.md)
- [05. 리뷰 RAG 연동](step-4/05.review-rag-integration.md)

### Step 5. Telegram Bot과 사용자 피드백 수집
- [01. Telegram Bot 설정](step-5/01.telegram-bot-setup.md)
- [02. Telegram으로 RAG 검색하기](step-5/02.rag-search-via-telegram.md)
- [03. 피드백 데이터 계층 설계](step-5/03.feedback-entity-repository.md)
- [04. 피드백 버튼과 Callback 처리](step-5/04.feedback-ui-callback-handler.md)
- [05. 피드백 루프 완성: 관리자 API와 검색 연동](step-5/05.feedback-search-integration.md)
- [06. 피드백 기반 검색 개선 설계](step-5/06.llm-prompt-guidelines.md)

### Step 6. 피드백 기반 개인화 추천
- [01. 취향을 벡터로 표현하기](step-6/01.vector-embedding-basics.md)
- [02. 사용자 선호 벡터 계산하기](step-6/02.user-preference-vector.md)
- [03. 검색 결과 개인화 재정렬](step-6/03.personalized-ranking.md)
- [04. 콜드 스타트와 최적화](step-6/04.cold-start-and-optimization.md)
- [05. 전체 흐름 복습: 개인화 검색 한눈에 보기](step-6/05.integration-guide.md)

### Step 7. Tool Calling으로 만드는 도서관 도우미 AI
- [00. Tool Calling, MCP, Agent, A2A 이론](step-7/00.theory.md)
- [01. 개요 및 시나리오](step-7/01.overview.md)
- [02. 단일 Function 구현: 도서 검색](step-7/02.single-function.md)
- [03. 복수 Function 조합: 검색 + 리뷰](step-7/03.multiple-functions.md)
- [04. 외부 API 연동: 대출 가능 도서관 조회](step-7/04.external-api.md)
- [05. @Tool 애노테이션 방식](step-7/05.tool-annotation.md)
- [06. 실습 가이드](step-7/06.practice-guide.md)
- 부록: [A2A 구현 가능성 분석](step-7/A2A_FEASIBILITY_ANALYSIS.md) · [행정구역 코드 목록](step-7/region-codes.md) · [도서관 정보나루 API 매뉴얼](step-7/도서관정보나루_API_Manual.md)

### Step 8. n8n 워크플로우 자동화 (보너스)
- [01. n8n 워크플로우 자동화 연동](step-8/01.n8n-integration.md)

### 참고
- [용어집](glossary.md)
- [실전 트러블슈팅 사례집](troubleshooting.md): 운영 중 겪은 문제의 진단·해결 기록
- [문서 작성 템플릿](TEMPLATE.md)

## 실습 환경 요약

- Java 21, Spring Boot 3.5, Spring AI 1.1.2, Maven
- PostgreSQL + pgvector (Docker로 실행 가능)
- LLM: Google Gemini 또는 Ollama, 임베딩: BGE-M3
- DB를 쓰는 테스트는 Testcontainers가 일회용 컨테이너를 띄우므로 Docker가 실행 중이어야 합니다.

자세한 준비 과정은 [Step 0. 개발 환경 준비](step-0/02.environment-setup.md)를 따릅니다.
