# Step 7: 개인화 추천 시스템

## 개요

Step 5에서 수집한 피드백 데이터를 활용하여 사용자별 맞춤 도서 추천 시스템을 구축합니다.

## 학습 목표

- 익명 사용자 환경에서의 개인화 한계 이해
- 벡터 임베딩을 활용한 사용자 선호도 학습
- 코사인 유사도 기반 개인화 재정렬
- Telegram chatId 기반 세션 개인화 구현

## 선행 조건

**Step 5 완료 필수**
- SearchFeedback Entity ✅
- FeedbackService ✅
- Telegram Bot 피드백 수집 ✅
- 피드백 데이터가 일부 쌓여 있어야 함

## 문서 목록

### 1. [개인화 계획](./01.personalization-plan.md)
- 개인화 접근 방식 설계
- 제약사항 분석 (로그인 없는 환경)
- 3단계 구현 계획

### 2. [구현 가이드](./02.personalization-implementation.md)
- Phase 1: 도서별 피드백 점수화
- Phase 2: 사용자 선호 벡터 계산
- Phase 3: Telegram Bot 연동

## 제약사항

**⚠️ 로그인 시스템 없음**
```
사용자 식별: Telegram chatId (익명)
데이터 원천: 최근 피드백 기록만
개인화 범위: 세션 기반 단기 개인화
```

## 기술 스택

- **BGE-M3 임베딩**: 1024차원 float[] 배열 (기존 보유)
- **코사인 유사도**: 사용자 선호 벡터 매칭
- **RRF 알고리즘**: 기존 검색 결과와 조합
- **Telegram Bot API**: chatId 기반 세션 관리

## 예상 소요 시간

- **Phase 1**: 2-3시간
- **Phase 2**: 3-4시간
- **Phase 3**: 1시간
- **총계**: 6-8시간

## 시작하기

```bash
# 1. 피드백 데이터 확인
SELECT book_id, type, COUNT(*)
FROM search_feedbacks
GROUP BY book_id, type;

# 2. Step 7 문서 순서대로 학습
docs/step-7/01.personalization-plan.md

# 3. 구현 진행
docs/step-7/02.personalization-implementation.md
```

## 주의사항

### 콜드 스타트 문제
- 신규 사용자는 피드백이 없어 개인화 불가
- 해결: 피드백 3개 이상 시 개인화 시작

### Telegram chatId 한계
- Bot 삭제 후 재설치 → chatId 변경
- 해결: 단기 세션으로만 활용, 주기적 정리

### 성능 고려사항
- 선호 벡터 계산은 캐싱 필수
- 피드백 데이터 많아지면 비동기 처리 필요

## 다음 단계

Step 7 완료 후:
- [ ] 개인화 효과 A/B 테스트
- [ ] 피드백 점수 시각화 대시보드
- [ ] 장기적으로 로그인 시스템 도입 고려
