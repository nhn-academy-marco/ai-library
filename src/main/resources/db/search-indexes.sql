-- =====================================================================
-- 검색 성능 인덱스 (운영 DB에 수동 적용, 애플리케이션이 자동 실행하지 않음)
--
-- 적용: psql -h <host> -U <user> -d library -v ON_ERROR_STOP=1 -f src/main/resources/db/search-indexes.sql
-- CONCURRENTLY는 트랜잭션 블록 안에서 실행할 수 없으므로 psql -f 로 문장별 자동 커밋으로 실행합니다.
-- 되돌리기: DROP INDEX CONCURRENTLY IF EXISTS <인덱스명>;
-- =====================================================================

-- HNSW 그래프를 메모리 안에서 만들도록 이 세션에서만 늘립니다 (기본 64MB면 생성이 크게 느려짐)
SET maintenance_work_mem = '512MB';
-- 병렬 생성은 maintenance_work_mem 크기의 공유 메모리(/dev/shm)를 잡습니다.
-- Docker로 실행한 PostgreSQL은 /dev/shm 기본 한도가 64MB라 "No space left on device"로 실패하므로 병렬 생성을 끕니다.
SET max_parallel_maintenance_workers = 0;

-- 1) 벡터 검색: 코사인 거리(<=>) 근사 최근접 검색용 HNSW 인덱스
--    쿼리가 "ORDER BY embedding <=> :query LIMIT n" 형태여야 인덱스를 사용합니다.
--    검색 시 hnsw.ef_search(기본 40)를 LIMIT 이상으로 올려야 상위 n건을 모두 받을 수 있습니다.
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_books_embedding_hnsw
    ON books USING hnsw (embedding vector_cosine_ops);

-- 2) 키워드 검색: lower(컬럼) LIKE '%검색어%' 를 위한 trigram GIN 인덱스 (pg_trgm 확장 필요)
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_books_title_trgm
    ON books USING gin (lower(title) gin_trgm_ops);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_books_author_trgm
    ON books USING gin (lower(author_name) gin_trgm_ops);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_books_publisher_trgm
    ON books USING gin (lower(publisher_name) gin_trgm_ops);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_books_subtitle_trgm
    ON books USING gin (lower(subtitle) gin_trgm_ops);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_books_volume_title_trgm
    ON books USING gin (lower(volume_title) gin_trgm_ops);

ANALYZE books;
