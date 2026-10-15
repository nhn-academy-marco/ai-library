-- 테스트 컨테이너 초기화 스크립트 (pgvector/pgvector 이미지)
CREATE EXTENSION IF NOT EXISTS vector;

-- 전문 검색 함수(ts_match_korean)가 사용하는 'korean' 설정.
-- 운영 DB의 형태소 분석 설정 대신 테스트에서는 simple 설정을 복사해 사용합니다.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_ts_config WHERE cfgname = 'korean') THEN
        CREATE TEXT SEARCH CONFIGURATION korean (COPY = simple);
    END IF;
END
$$;
