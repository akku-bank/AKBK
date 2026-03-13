-- PostgreSQL 초기화 SQL
-- 앱 메타데이터 DB(app_db)와 별도로 벡터 검색용 DB(vector_db)를 생성합니다.
-- docker-entrypoint-initdb.d 에서 실행되므로, 새 볼륨으로 최초 기동할 때 한 번 적용됩니다.

-- vector_db가 없을 때만 CREATE DATABASE 문을 동적으로 실행합니다.
SELECT 'CREATE DATABASE vector_db OWNER "' || current_user || '"'
WHERE NOT EXISTS (
    SELECT 1
    FROM pg_database
    WHERE datname = 'vector_db'
)
\gexec

-- 생성된 vector_db로 접속을 전환한 뒤 pgvector 확장을 활성화합니다.
\connect vector_db
CREATE EXTENSION IF NOT EXISTS vector;
