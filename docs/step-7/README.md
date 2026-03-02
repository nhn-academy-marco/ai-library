# Step 7: Library Assistant Agent - 도서관 도우미 AI

## 학습 개요

Step 6까지 구현한 **검색, 추천, 개인화 시스템**을 Spring AI Function Calling을 통해 **지능형 도서관 도우미 Agent**로 통합합니다. 사용자의 자연어 질문 한 번으로 도서 검색부터 리뷰 확인, 대출 가능 여부까지 한 번에 처리하는 AI 시스템을 구현합니다.

## 학습 목표

1. **AI Agent 이론**
   - Agent, MCP, A2A 개념 이해
   - AI 시스템 설계 패턴 학습
   - 현대 AI 아키텍처 트렌드 파악

2. **Function Calling 기초**
   - AI가 시스템 도구를 사용하는 방법
   - @Description 어노테이션으로 함수 정의
   - ChatClient에 함수 등록

3. **다중 Function 조합**
   - 검색 → 리뷰 → 대출 가능 여부
   - 함수 간 데이터 전달
   - 오케스트레이션 패턴 이해

4. **외부 API 연동**
   - 도서관정보나루 API 연동
   - 실시간 대출 가능 도서관 조회

5. **개인화 경험**
   - 사용자 선호도 반영
   - Step 6 PersonalizationService 활용

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

### Step 7: Library Assistant Agent (현재)

**기존 방식 (Step 6까지)**:
```
사용자: "자바의 정석 책 정보랑 리뷰도 알려줘"
   ↓
[Telegram Bot]
   ↓
[검색 API] → 검색 결과 1
[리뷰 API] → 리뷰 결과 2
   ↓
사용자: 각각 조회해야 함
```

**Library Assistant Agent 방식**:
```
사용자: "자바의 정석 책 정보랑 리뷰도 알려줘"
   ↓
[AI가 자동으로 함수 선택]
   ↓
[검색 Function] → 도서 정보
[리뷰 Function] → 리뷰 요약
[대출 Function] → 대출 가능 도서관
   ↓
AI가 종합 답변 생성
```

---

## Library Assistant Agent란?

### 정의

> **Library Assistant Agent** = 사용자의 자연어 질문을 이해하고, 필요한 도구(함수)를 자동으로 선택하여 도서관 서비스를 제공하는 AI 시스템

### 핵심 기능

1. **도서 검색**: 자연어로 도서 검색
2. **리뷰 요약**: 도서별 리뷰 요약 제공
3. **대출 가능 여부**: 전국 도서관 대출 가능 현황
4. **개인화 추천**: 사용자 선호도 반영

### 시나리오 예시

```
❌ 기존 방식 (Step 5 Telegram Bot)
사용자: "자바의 정석 책 정보 알려줘"
→ 검색 결과 표시

사용자: "리뷰도 보여줘"
→ 별도 조회 필요

사용자: "대출 가능해?"
→ 또 별도 조회 필요

✅ Library Assistant Agent
사용자: "자바의 정석 책 정보, 리뷰, 대출 가능한 곳 알려줘"
→ AI가 자동으로 3가지 작업 수행
→ 종합 답변 제공
```

---

## 왜 Library Assistant Agent인가?

### 1. Step 6 기반과 완벽한 시너지

| Step 6 구현 기능 | 활용 방법 |
|-----------------|-----------|
| **BookSearchService** | 도서 검색 Function |
| **ReviewService** | 리뷰 조회 Function |
| **PersonalizationService** | 개인화 Function |
| **LibraryInfoNaruApiClient** | 대출 가능 여부 Function |

이미 모든 컴포넌트가 준비되어 있어, 이를 조합하기만 하면 됩니다!

### 2. 교육적 가치

**학습할 수 있는 기술**:
- Spring AI Function Calling
- Multi-step Orchestration
- External API 연동
- Prompt Engineering
- 에러 처리 및 Fallback

### 3. 실무 적용 가능성

도서관뿐만 아니라, 다양한 서비스에 응용 가능:
- 쇼핑몰: 상품 검색 → 재고 확인 → 배송 조회
- 병원: 진료 예약 → 의사 정보 조회 → 결제
- 은행: 계좌 조회 → 잔액 확인 → 이체 내역

---

## 학습 로드맵

```
Level 1: 단일 Function (2시간)
├─ 도서 검색 Function 구현
├─ ChatClient 설정
└─ AI가 함수 호출 확인

Level 2: 복수 Function (3시간)
├─ 리뷰 조회 Function 추가
├─ 검색 + 리뷰 조합
└─ 함수 간 데이터 전달

Level 3: 외부 API 연동 (2시간)
├─ 대출 가능 여부 Function 추가
├─ 도서관정보나루 API 연동
└─ 전체 워크플로우 통합

총 시간: 7시간
```

---

## 기술 스택

### 필수 기술
- **Spring AI**: Function Calling 프레임워크
- **Google Gemini** 또는 **Ollama**: LLM 모델
- **Java 17+**: Record, Lambda

### 활용 기술 (Step 6에서 이미 구현됨)
- **Spring Boot**: 웹 프레임워크
- **JPA + QueryDSL**: 데이터베이스 조회
- **pgvector**: 벡터 검색
- **Telegram Bot API**: 사용자 인터페이스

---

## 예상 소요 시간

| 단계 | 내용 | 시간 |
|------|------|------|
| **이론 학습** | Function Calling 개념 | 1시간 |
| **Level 1** | 단일 Function 구현 | 2시간 |
| **Level 2** | 복수 Function 조합 | 3시간 |
| **Level 3** | 외부 API 연동 | 2시간 |
| **테스트** | 통합 테스트 및 디버깅 | 1시간 |
| **총계** | | **9시간** |

---

## 시작하기 전 체크리스트

### 환경 점검
- [ ] Spring AI 의존성 추가됨 (pom.xml 확인)
- [ ] LLM API 키 설정됨 (application.properties)
- [ ] Step 6까지 구현 완료됨

### 선행 지식
- [ ] Spring Boot 기초 (@Controller, @Service)
- [ ] JPA 기초 (Entity, Repository)
- [ ] 람다 표현식 이해
- [ ] HTTP API 기초

---

## 학습 체크리스트

완료 후 다음을 할 수 있는지 확인해 보세요:

### 기본 개념
- [ ] Function Calling이 무엇인지 설명할 수 있다
- [ ] AI가 함수를 선택하는 원리를 이해한다
- [ ] @Description의 역할을 안다

### 구현 능력
- [ ] 단일 Function을 정의할 수 있다
- [ ] ChatClient에 함수를 등록할 수 있다
- [ ] 여러 Function을 조합할 수 있다
- [ ] 외부 API를 연동할 수 있다

### 고급 기능
- [ ] 함수 간 데이터 전달을 이해한다
- [ ] 오케스트레이션 패턴을 설계할 수 있다
- [ ] 에러 처리 및 Fallback을 구현할 수 있다

---

## 문서 구조

### [00. MCP, A2A, Agent 이론](./00.theory.md)
- AI Agent의 개념과 정의
- MCP (Model Context Protocol) 이해
- A2A (Agent-to-Agent) 통신 패턴
- Library Assistant Agent 아키텍처

### [01. 개요 및 시나리오](./01.overview.md)
- Library Assistant Agent 소개
- 왜 필요한지, 무엇을 할 수 있는지
- 시나리오 예시

### [02. 단일 Function 구현](./02.single-function.md)
- 도서 검색 Function
- ChatClient 설정
- 테스트 방법

### [03. 복수 Function 조합](./03.multiple-functions.md)
- 리뷰 조회 Function
- 2개 Function 조합
- 데이터 전달

### [04. 외부 API 연동](./04.external-api.md)
- 대출 가능 여부 Function
- 도서관정보나루 API
- 전체 통합

### [05. 실습 가이드](./05.practice-guide.md)
- 단계별 실습 미션
- 테스트 시나리오
- 디버깅 팁

---

## 참고 자료

- [Spring AI Function Calling](https://docs.spring.io/spring-ai/reference/api/guide.html)
- [Google Gemini API](https://ai.google.dev/)
- [도서관정보나루 API](https://www.data4library.kr/)

---

**시작하기**: [00. MCP, A2A, Agent 이론 →](./00.theory.md)
