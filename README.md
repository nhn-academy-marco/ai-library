# AI Library Platform

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.2-brightgreen)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-blue)](https://www.oracle.com/java/technologies/downloads/#java21)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-pgvector-orange)](https://github.com/pgvector/pgvector)

공공 도서 데이터를 활용하여 **전통적인 검색 시스템이 AI 시스템(RAG · Vector · MCP)으로 진화하는 과정**을 학습하는 실습 프로젝트입니다.

## 📖 프로젝트 개요

이 프로젝트는 단순히 AI 기능을 추가하는 것을 넘어, 데이터 구축부터 시작하여 검색 품질 개선, 벡터 검색 도입, 그리고 RAG(Retrieval-Augmented Generation) 기반의 AI 응답 생성까지의 전체 흐름을 단계별로 경험하도록 설계되었습니다.

### 핵심 목표
- **데이터 진화 과정 이해**: 데이터 → 검색 → 의미 → 생성 → 판단 → 도구 활용의 흐름 학습
- **실전 AI 통합**: Spring AI를 활용한 LLM(Gemini, Ollama 등) 및 Embedding API 연동
- **성능 및 비용 최적화**: Top-K 튜닝, 시맨틱 캐싱 등 AI 서비스 운영 시 발생하는 문제 해결

## 🛠 기술 스택

- **Backend**: Java 21, Spring Boot 3.4.2
- **Data Access**: Spring Data JPA, QueryDSL (Type-safe 동적 쿼리)
- **Database**: PostgreSQL with **pgvector**
- **AI/LLM**: Spring AI, Google Gemini, Ollama, BGE-M3 (Embedding)
- **Batch**: Spring Scheduler (임베딩 일괄 처리)
- **Frontend**: Thymeleaf, Tabler (UI/UX)
- **Demo**: [https://ai.java21.net/library/](https://ai.java21.net/library/)

## 📂 프로젝트 구조

```text
src/main/java/com/nhnacademy/library
 ├─ batch.init     # 공공 데이터 적재 및 초기화 로직
 ├─ batch.embedding # 도서 임베딩 생성 스케줄러 및 배치
 ├─ core.book      # 도서 관련 핵심 비즈니스 로직, DB 접근, AI 서비스
 ├─ core.config    # 공통 설정 (QueryDSL, AI, DB 등)
 ├─ core.book.util # 텍스트 전처리(TextPreprocessor) 등 유틸리티
 └─ front.web      # 웹 컨트롤러 및 화면 처리
```

## 🚀 주요 기능

1. **전통적 검색**: QueryDSL을 활용한 키워드 기반 동적 쿼리 및 PostgreSQL 전문 검색(Full Text Search).
2. **벡터 검색**: `pgvector`와 `BGE-M3` 모델을 이용한 의미 기반(Semantic) 자연어 검색.
3. **하이브리드 검색**: RRF(Reciprocal Rank Fusion) 알고리즘을 통한 키워드와 벡터 검색 결과의 최적 조합.
4. **RAG (AI 답변)**: 검색 결과를 문맥(Context)으로 활용하여 LLM이 정확한 답변을 생성하도록 유도.
5. **AI 최적화**: 
   - 질문 의도에 따른 동적 Top-K 설정
   - RRF 점수 임계값 필터링 (Noise Reduction)
   - 'Lost in the Middle' 방지를 위한 컨텍스트 재구성

## 📅 주차별 학습 로드맵

상세 가이드는 [`docs/index.md`](docs/index.md)를 참고하세요.

- **[Week 1] 데이터 구축과 전통적 검색**: CSV 배치 적재 및 LIKE/인덱스 기반 검색 구현.
- **[Week 2] 검색의 진화: 벡터 검색**: 임베딩 생성 및 `pgvector` 기반 의미 검색 도입.
- **[Week 3] RAG: AI가 답변을 생성하다**: LLM 연동 및 검색 결과 기반 프롬프트 엔지니어링.
- **[Week 4] AI 성능 튜닝**: 응답 지연 및 비용 문제 해결을 위한 Top-K 튜닝 및 캐싱 전략.
- **[Week 5] MCP: AI가 시스템을 사용하다**: AI 오케스트레이션 및 도구 활용(Tool Use).

## ⚙️ 실행 방법

### 요구 사항
- Docker (PostgreSQL + pgvector 실행용)
- JDK 21
- Maven

### 로컬 실행
1. **DB 실행**: `pgvector`가 포함된 PostgreSQL 이미지를 실행합니다.
2. **설정 수정**: `src/main/resources/application.properties`에서 DB 연결 정보 및 API 키(Gemini 등)를 설정합니다.
3. **애플리케이션 구동**:
   ```bash
   mvn spring-boot:run
   ```
4. **데이터 초기화**: 서버 시작 시 `src/main/resources/data/init/` 경로의 CSV 파일이 자동으로 적재됩니다.

---
NHN Academy AI Library Project
