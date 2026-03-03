# Step 7: LLM 기반 오케스트레이션 - 도서관 도우미 AI

## 학습 개요

Step 6까지 구현한 **검색, 추천, 개인화 시스템**을 Spring AI Function Calling을 통해 **지능형 도서관 도우미 AI**로 통합합니다. 사용자의 자연어 질문 한 번으로 도서 검색부터 리뷰 확인, 대출 가능 여부까지 LLM이 스스로 필요한 API를 선택하고 호출하는 AI 시스템을 구현합니다.

## 학습 목표

1. **LLM 기반 오케스트레이션 이론**
   - 수동 오케스트레이션 vs LLM 자율 오케스트레이션
   - Function Calling 개념과 Spring AI 구현
   - MCP (Model Context Protocol) 이해

2. **Function Calling 구현**
   - 7개 도서관정보나루 API를 Function으로 변환
   - 타입 안전한 Request/Response DTO 설계
   - ChatClient에 Function 등록

3. **LLM 자율 오케스트레이션**
   - LLM이 필요한 Function 스스로 선택
   - 여러 Function 조합 실행
   - 결과 종합 및 자연어 응답

---

## 전체 커리큘럼에서의 위치

### Step 1~6: 기반 구축

| Step | 주요 내용 | 성과 |
|------|----------|------|
| Step 1 | 데이터 구축, 전통적 검색 | 기본 검색 시스템 |
| Step 2 | 벡터 검색, 하이브리드 검색 | 의미 기반 검색 |
| Step 3 | RAG 기반 AI 추천 | AI 추천 엔진 |
| Step 4 | 성능 최적화, 캐싱 | 빠른 응답 속도 |
| Step 5 | Telegram Bot, 피드백 | 사용자 인터페이스 |
| Step 6 | 개인화 추천 시스템 | 맞춤형 추천 |

### Step 7: LLM 기반 오케스트레이션 (현재)

**기존 방식 (Step 6까지 - 수동 Agent 선택)**:
```
사용자: "토비의 스프링 광주 도서관에서 빌릴까?"
   ↓
[MainAgent] 키워드 매칭으로 Agent 선택
   - BOOK → BookAgent
   - LOAN → LoanAgent
   - LIBRARY → LibraryAgent
   ↓
[개발자 hardcode 로직] 결과 종합
   ↓
사용자: 파편화된 결과
```

**새로운 방식 (Step 7 - LLM 자율 오케스트레이션)**:
```
사용자: "토비의 스프링 광주 도서관에서 빌릴까?"
   ↓
[LLM] 질문 분석 → 필요한 Function 자동 선택
   ↓
[searchBooks Function] → 도서 검색 (ISBN 확보)
[getLibrariesWithBook Function] → 광주(29) 지역 대출 가능 도서관 조회
   ↓
[LLM] 결과 종합 → 자연스러운 한국어 답변
```

---

## LLM 기반 오케스트레이션이란?

### 정의

> **LLM 기반 오케스트레이션** = LLM이 사용자의 질문을 이해하고 스스로 필요한 Function(도구)을 선택하여 시스템을 제어하는 AI 패턴

### 핵심 특징

1. **자율성**: LLM이 스스로 함수 선택 (개발자 hardcode 제거)
2. **유연성**: 복잡한 질문도 동적으로 처리
3. **확장성**: 새로운 Function 추가가 쉬움

### 시나리오 예시

```
❌ 기존 방식 (Step 6 - 수동 Agent 선택)
사용자: "토비의 스프링 3.1 광주 도서관에서 빌려고 하는데 추천해줘"
  → [개발자가 if-else로 Agent 선택]
  → BOOK, LOAN, LIBRARY, RECOMMENDATION Agent 실행
  → 결과가 파편화됨

✅ LLM 기반 오케스트레이션 (Step 7)
사용자: "토비의 스프링 3.1 광주 도서관에서 빌려고 하는데 추천해줘"
  → [LLM이 질문 분석]
  → 1. searchBooks("토비의 스프링 3.1") → ISBN 확보
  → 2. getLibrariesWithBook(isbn, region="29") → 광주 대출 가능 도서관
  → 3. getRecommendedBooks(isbn, "mania") → 마니아 추천
  → [LLM이 결과 종합] 자연스러운 답변
```

---

## 왜 LLM 기반 오케스트레이션인가?

### 1. 기존 한계 극복

| 기존 방식의 문제 | LLM 오케스트레이션 해결 |
|-----------------|---------------------|
| 키워드 매칭만 가능 | 의도 이해 후 Function 선택 |
| 복잡한 if-else 로직 | LLM이 스스로 판단 |
| 새로운 Agent 추가 시 코드 수정 | Function만 추가하면 자동 인식 |
| 파편화된 결과 | LLM이 자연스럽게 종합 |

### 2. 도서관정보나루 API 완벽 활용

**17개 API → 7개 주요 Function으로 변환:**

| Function | API | 설명 |
|----------|-----|------|
| `searchBooks` | srchBooks | 도서 검색 |
| `getLibrariesWithBook` | libSrchByBook | 대출 가능 도서관 조회 |
| `searchLibraries` | libSrch | 도서관 검색 |
| `getBookDetail` | srchDtlList | 도서 상세 정보 |
| `getPopularBooks` | loanItemSrch | 인기대출도서 |
| `getHotTrendBooks` | hotTrend | 대출 급상승 도서 |
| `getRecommendedBooks` | recommandList | 추천도서 |

### 3. 교육적 가치

**학습할 수 있는 기술**:
- Spring AI Function Calling
- LLM 기반 시스템 설계
- 타입 안전한 Function 설계
- 오케스트레이션 패턴
- Prompt Engineering

---

## 구현 아키텍처

### 핵심 컴포넌트

```
┌─────────────────────────────────────────────────────────┐
│                    사용자 질문                          │
│  "토비의 스프링 광주 도서관에서 빌릴까?"              │
└───────────────────┬─────────────────────────────────────┘
                    │
                    ↓
┌─────────────────────────────────────────────────────────┐
│              AiOrchestrationService (MainAgent)         │
│  - 질문 수신                                            │
│  - ChatClient.prompt().user(message).call()           │
│  - LLM이 스스로 Function 선택                            │
└───────────────────┬─────────────────────────────────────┘
                    │
                    ↓
┌─────────────────────────────────────────────────────────┐
│                LibraryFunctions (7개 Function)           │
│  ┌─────────────────┬─────────────────┬───────────────┐  │
│  │  searchBooks    │ getLibrariesWith │ searchLibraries │  │
│  │                 │ Book             │               │  │
│  ├─────────────────┼─────────────────┼───────────────┤  │
│  │ getBookDetail   │ getPopularBooks  │ getHotTrend   │  │
│  │                 │                 │ Books         │  │
│  ├─────────────────┴─────────────────┴───────────────┤  │
│  │          getRecommendedBooks                        │  │
│  └─────────────────────────────────────────────────────┘  │
└───────────────────┬─────────────────────────────────────┘
                    │
                    ↓
┌─────────────────────────────────────────────────────────┐
│          LibraryInfoNaruApiClient (17개 API)            │
│  - 도서관정보나루 API 직접 호출                         │
└─────────────────────────────────────────────────────────┘
```

### 코드 간소화 효과

| 파일 | 기존 (Step 6) | Step 7 | 변화 |
|------|---------------|--------|------|
| AiOrchestrationService | 760줄 | 80줄 | **-89%** |
| selectAgents() | 80줄 | 0줄 | **제거** |
| executeAgents() | 100줄 | 0줄 | **제거** |
| synthesizeResults() | 200줄 | 0줄 | **제거** |
| 결과 | 수동 Agent 선택 | LLM 자율 선택 | **지능화** |

---

## 학습 로드맵

```
Phase 1: Function 정의 (1시간)
├─ LibraryFunctions 클래스 생성
├─ 7개 Function<Request, Response> 구현
├─ DTO 정의 (Request/Response)
└─ LibraryInfoNaruApiClient 연동

Phase 2: LLM 오케스트레이션 (1시간)
├─ AiOrchestrationService 단순화
├─ ChatClient 설정
├─ System Prompt 작성
└─ LLM 자율 Function 선택 테스트

Phase 3: 실전 테스트 (1시간)
├─ 다양한 질문 시나리오 테스트
├─ Function 조합 실행 확인
├─ 에러 처리 및 Fallback
└─ 성능 최적화

총 시간: 3시간
```

---

## 기술 스택

### 핵심 기술
- **Spring AI 1.1.2**: Function Calling 프레임워크
- **Java 17+**: Record, Lambda
- **Ollama (qwen2.5:latest)**: Local LLM (Function Calling 지원)

### 활용 기술 (Step 1-6)
- **Spring Boot**: 웹 프레임워크
- **LibraryInfoNaruApiClient**: 도서관정보나루 API 클라이언트
- **Telegram Bot API**: 사용자 인터페이스

---

## 예상 소요 시간

| 단계 | 내용 | 시간 |
|------|------|------|
| **이론 학습** | Function Calling, LLM 오케스트레이션 | 30분 |
| **Phase 1** | Function 정의 | 1시간 |
| **Phase 2** | LLM 오케스트레이션 | 1시간 |
| **Phase 3** | 테스트 및 디버깅 | 30분 |
| **총계** | | **3시간** |

---

## 시작하기 전 체크리스트

### 환경 점검
- [ ] Spring AI 1.1.2 의존성 추가됨
- [ ] Ollama 설치 및 qwen2.5:latest 실행 중
- [ ] Step 6까지 구현 완료됨
- [ ] LibraryInfoNaruApiClient 구현됨

### 선행 지식
- [ ] Spring Boot 기초 (@Component, @Service)
- [ ] Java Record 타입 이해
- [ ] 함수형 인터페이스 (Function<R, T>) 이해
- [ ] LLM 기초 (Prompt, System Role)

---

## 학습 체크리스트

완료 후 다음을 할 수 있는지 확인해 보세요:

### 기본 개념
- [ ] LLM 기반 오케스트레이션을 설명할 수 있다
- [ ] Function Calling이 무엇인지 이해한다
- [ ] 수동 Agent 선택 vs LLM 자율 선택의 차이를 안다

### 구현 능력
- [ ] Function<Request, Response>를 정의할 수 있다
- [ ] 타입 안전한 Request/Response DTO를 설계할 수 있다
- [ ] ChatClient에 Function을 등록할 수 있다
- [ ] LLM이 필요한 Function을 스스로 선택하게 할 수 있다

### 고급 기능
- [ ] 여러 Function 조합 실행을 이해한다
- [ ] System Prompt으로 LLM 행동을 제어할 수 있다
- [ ] 에러 처리 및 Fallback을 구현할 수 있다

---

## 문서 구조

### [00. MCP, A2A, Agent 이론](./00.theory.md)
- AI Agent의 개념과 정의
- MCP (Model Context Protocol) 이해
- A2A (Agent-to-Agent) 통신 패턴

### [01. 개요 및 시나리오](./01.overview.md)
- LLM 기반 오케스트레이션 소개
- 왜 필요한지, 무엇을 할 수 있는지
- 시나리오 예시

### [02. 단일 Function 구현](./02.single-function.md)
- 도서 검색 Function
- DTO 설계
- 테스트 방법

### [03. 복수 Function 조합](./03.multiple-functions.md)
- 7개 Function 구현
- Function 조합 실행
- 데이터 전달

### [04. LLM 오케스트레이션](./04.external-api.md)
- AiOrchestrationService 단순화
- ChatClient 설정
- System Prompt 작성

### [05. 실습 가이드](./05.practice-guide.md)
- 단계별 실습 미션
- 테스트 시나리오
- 디버깅 팁

---

## 참고 자료

- [Spring AI Function Calling](https://docs.spring.io/spring-ai/reference/api/guide.html)
- [Ollama Function Calling](https://ollama.com/blog/function-calling-support)
- [도서관정보나루 API](https://www.data4library.kr/)

---

**시작하기**: [00. MCP, A2A, Agent 이론 →](./00.theory.md)
