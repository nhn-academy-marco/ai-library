# 공공 도서 데이터 기반 AI 검색 시스템

## 0. 프로젝트 개요

### 프로젝트명
`ai-library-platform`

### 목적
공공 도서 데이터를 활용하여 **전통적인 검색 시스템이 AI 시스템(RAG · Vector · MCP)으로 진화하는 전체 흐름**을 학습합니다.

이 프로젝트는 단순히 AI 기능을 붙이는 것이 아니라,
* **왜** AI가 필요한지
* AI를 기존 백엔드 아키텍처에 **어떻게** 녹이는지
* AI 도입 시 발생하는 **성능·비용·운영** 문제를 어떻게 다루는지
를 단계적으로 경험하는 것을 목표로 합니다.

> **핵심 메시지**
> AI는 처음부터 등장하지 않습니다. **데이터 → 검색 → 의미 → 생성 → 판단 → 도구 활용**으로 진화합니다.

### 학생들을 위한 학습 가이드
본 문서는 단순한 소스코드 제공이 아닌, 각 단계별로 **"무엇을(What)", "어떻게(How)", "왜(Why)"** 구현해야 하는지를 안내합니다. 
코드를 직접 작성하기 전에 해당 주차의 가이드 문서를 충분히 읽고, 제시된 **학습 포인트**와 **참고 링크**를 통해 기술적 배경을 먼저 이해하는 것을 권장합니다.

---

## 1. 프로젝트 패키지 구조

```
src/main/java/com/nhnacademy/library
 ├─ batch.init     # 공공 데이터 적재 및 초기화 로직
 ├─ core.book      # 도서 관련 핵심 비즈니스 로직 및 DB 접근
 ├─ core.config    # 공통 설정 (QueryDSL 등)
 ├─ front.web      # 웹 컨트롤러 및 화면 처리
```

### 각 패키지 역할

* **com.nhnacademy.library.core.book**

    * 도서 조회 및 상세 정보 처리
    * 키워드 검색 / 전문 검색(Full Text Search)
    * 벡터 검색(Vector Search) (예정)
    * DB 중심 비즈니스 로직(Business Logic)

* **com.nhnacademy.library.batch.init**

    * 공공 CSV 데이터 파싱(Parsing) 및 적재(Load)
    * Google Books API 데이터 보강 (예정)
    * 임베딩(Embedding) 생성 (예정)

* **com.nhnacademy.library.front.web**

    * 도서 검색 및 상세 페이지 UI 컨트롤러
    * AI 질의 UI (예정)

> 외부 Open API는 **Batch 또는 MCP를 통해서만 접근**하며,
> 서비스 런타임 검색에서는 직접 호출하지 않는다.

---

## 2. 기술 스택

* Java 21
* Spring Boot 4
* Spring Data JPA (기본 CRUD)
* QueryDSL (동적 쿼리)
* PostgreSQL
* pgvector
* LLM (API 또는 로컬)

---

## 3. 전체 시스템 흐름 요약

1. CSV 기반 데이터 적재
2. 외부 Open API를 통한 데이터 보강 (캐싱)
3. 키워드 기반 검색 구현
4. 검색 품질 및 성능 한계 체감
5. Vector 검색 도입
6. RAG 기반 AI 응답 생성
7. AI 성능·비용 튜닝
8. MCP 기반 AI 도구 활용

---

## 4. 주차별 시나리오

### Week 1 — 데이터 구축과 전통적 검색

#### 주요 흐름

* [01. 데이터 적재: 공공 도서 CSV Batch Load](week-1/01.csv-batch-load.md)
* [02. 기본 검색: LIKE 기반 동적 쿼리 구현](week-1/02.book-search-implementation.md)
* [03. 검색 UI: Thymeleaf와 Tabler 기반 화면 구축](week-1/03.book-search-front.md)
* [04. 성능 최적화: 인덱싱과 전문 검색(Full Text Search)](week-1/04.search-optimization-and-indexing.md)

#### 학습 포인트

* 데이터 구축(Corpus)과 이벤트 기반 아키텍처(EDA) 이해
* QueryDSL을 활용한 타입 세이프(Type-safe) 동적 쿼리
* 디자인 시스템(Tabler)과 레이아웃 엔진 활용 능력
* RDBMS 검색의 성능 한계와 인덱싱(B-Tree, GIN) 전략

---

### Week 2 — 검색의 진화: 벡터 검색(Vector Search)

#### 주요 흐름

* [01. 한계 체감: 키워드 검색의 품질 문제 분석](week-2/01.search-quality-limitations.md)
* [02. 환경 구축: PostgreSQL pgvector 설정](week-2/02.add-pgvector-column.md)
* [03. 지식 수치화: AI 임베딩(Embedding) 생성 Batch](week-2/03.embedding-generation-batch.md)
* [04. 의미 검색: 자연어 기반 벡터 검색 도입](week-2/04.natural-language-search.md)
* [05. 하이브리드 검색: 키워드와 벡터의 결합](week-2/05.hybrid-search.md)

#### 학습 포인트

* **의미 기반 검색(Semantic Search)**: 키워드 매칭의 한계를 이해하고 벡터 공간에서의 유사도 검색 원리를 습득합니다.
* **벡터 데이터베이스 활용**: PostgreSQL `pgvector`를 통해 비정형 데이터(텍스트)를 수치화하여 저장하고 검색하는 실무 기법을 익힙니다.
* **AI 모델 통합**: 외부 임베딩 모델 API를 백엔드 서비스와 연동하고 대량 데이터를 처리하는 배치 프로세스를 경험합니다.

---

### Week 3 — RAG: AI가 답변을 생성하다

#### 주요 흐름

* Vector 검색 결과 기반 Context 구성
* LLM 호출
* 도서 추천 / 요약 / 설명 생성

#### 학습 포인트

* Hallucination 문제
* RAG의 필요성
* Context 품질의 중요성

---

### Week 4 — AI 성능 튜닝

#### 주요 흐름

* 응답 지연 및 비용 문제 인식
* Top-K 튜닝
* Context 길이 제한
* 요약 → 재요약 전략
* 결과 캐싱

#### 학습 포인트

* AI 성능 문제는 설계 문제
* 비용·속도·품질의 균형

---

### Week 5 — MCP: AI가 시스템을 사용하다

#### 주요 흐름

* MCP 도입
* AI가 도구를 선택하여 호출
* 검색 / 보강 / 등록 / Batch 실행

#### 학습 포인트

* AI는 단순 응답기가 아님
* AI 오케스트레이션 개념
* A2A 확장 가능성

---

## 5. MCP 활용 예시 요약

* Vector 검색 MCP
* 외부 도서 정보 보강 MCP
* 신규 도서 등록 MCP
* 임베딩 Batch 트리거 MCP
* 검색 전략 선택 MCP

---

## 6. 이 프로젝트로 가르칠 수 있는 것

* AI 백엔드 시스템 설계 사고방식
* RAG 실전 적용 구조
* Vector 검색과 임베딩 이해
* AI 성능·비용 튜닝 포인트
* MCP 기반 Tool 사용 패턴
* Spring 기반 서비스에 AI 통합

---

## 7. 최종 요약 문장

> 이 프로젝트는 공공 데이터를 출발점으로,
> 검색 시스템이 어떻게 AI 시스템으로 진화하는지를
> **데이터 → 의미 → 생성 → 판단 → 도구 활용**의 흐름으로 경험하는 실습형 AI 백엔드 시나리오다.
