-- ============================================
-- 검색 피드백 테이블 생성 스크립트
-- ============================================
-- 목적: Telegram Bot을 통해 수집된 사용자 피드백 저장
-- 활용: 검색어별 피드백 분석, 검색 결과 품질 개선
-- ============================================

-- 테이블 생성
CREATE TABLE IF NOT EXISTS search_feedbacks (
    id BIGSERIAL PRIMARY KEY,
    chat_id BIGINT NOT NULL,
    query VARCHAR(500) NOT NULL,
    book_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('GOOD', 'BAD')),
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 인덱스 생성 (조회 성능 최적화)
CREATE INDEX IF NOT EXISTS idx_search_feedbacks_chat_id ON search_feedbacks(chat_id);
CREATE INDEX IF NOT EXISTS idx_search_feedbacks_created_at ON search_feedbacks(created_at);
CREATE INDEX IF NOT EXISTS idx_search_feedbacks_query_book ON search_feedbacks(query, book_id);
CREATE INDEX IF NOT EXISTS idx_search_feedbacks_book_id ON search_feedbacks(book_id);

-- 코멘트 추가
COMMENT ON TABLE search_feedbacks IS '사용자 검색 피드백 테이블';
COMMENT ON COLUMN search_feedbacks.id IS '기본키';
COMMENT ON COLUMN search_feedbacks.chat_id IS 'Telegram 사용자 ID';
COMMENT ON COLUMN search_feedbacks.query IS '검색어 (예: 해리포터)';
COMMENT ON COLUMN search_feedbacks.book_id IS '피드백 대상 도서 ID';
COMMENT ON COLUMN search_feedbacks.type IS '피드백 타입: GOOD(좋았음), BAD(별로였음)';
COMMENT ON COLUMN search_feedbacks.created_at IS '피드백 생성 시간 (Java: OffsetDateTime)';

-- ============================================
-- 검증 쿼리 예시
-- ============================================

-- 1. 전체 피드백 통계
-- SELECT type, COUNT(*) as count
-- FROM search_feedbacks
-- GROUP BY type;

-- 2. 특정 검색어("해리포터")에 대한 피드백
-- SELECT * FROM search_feedbacks
-- WHERE query = '해리포터'
-- ORDER BY created_at DESC;

-- 3. 특정 도서(book_id=101)의 피드백 통계
-- SELECT
--     book_id,
--     COUNT(CASE WHEN type = 'GOOD' THEN 1 END) as good_count,
--     COUNT(CASE WHEN type = 'BAD' THEN 1 END) as bad_count,
--     COUNT(*) as total_count,
--     ROUND(COUNT(CASE WHEN type = 'GOOD' THEN 1 END)::NUMERIC / COUNT(*) * 100, 2) as good_ratio
-- FROM search_feedbacks
-- WHERE book_id = 101
-- GROUP BY book_id;

-- 4. 검색어별 피드백 통계 (최소 3건 이상)
-- SELECT
--     query,
--     book_id,
--     COUNT(CASE WHEN type = 'GOOD' THEN 1 END) as good_count,
--     COUNT(CASE WHEN type = 'BAD' THEN 1 END) as bad_count,
--     COUNT(*) as total_count,
--     (COUNT(CASE WHEN type = 'GOOD' THEN 1 END) - COUNT(CASE WHEN type = 'BAD' THEN 1 END))::FLOAT / COUNT(*) as feedback_score
-- FROM search_feedbacks
-- GROUP BY query, book_id
-- HAVING COUNT(*) >= 3
-- ORDER BY query, feedback_score DESC;

-- 5. 최근 7일간 긍정 피드백 비율
-- SELECT
--     DATE(created_at) as date,
--     COUNT(CASE WHEN type = 'GOOD' THEN 1 END) as good_count,
--     COUNT(*) as total_count,
--     ROUND(COUNT(CASE WHEN type = 'GOOD' THEN 1 END)::NUMERIC / COUNT(*) * 100, 2) as good_ratio
-- FROM search_feedbacks
-- WHERE created_at >= CURRENT_DATE - INTERVAL '7 days'
-- GROUP BY DATE(created_at)
-- ORDER BY date DESC;

-- 6. 특정 사용자(chat_id=123456789)의 피드백 목록
-- SELECT sf.*, b.title, b.author_name
-- FROM search_feedbacks sf
-- JOIN books b ON sf.book_id = b.id
-- WHERE sf.chat_id = 123456789
-- ORDER BY sf.created_at DESC
-- LIMIT 20;
