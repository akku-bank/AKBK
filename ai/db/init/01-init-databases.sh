# ai/db/init/01-init-databases.sh
# PostgreSQL 데이터베이스 초기화 스크립트
# vector_db 데이터베이스가 존재하지 않으면 생성하고, pgvector 확장을 활성화합니다.
set -euo pipefail

# Vector DB value from .env
VECTOR_DB_NAME="${VECTOR_DB_NAME:-vector_db}"

# Create vector DB if missing.
if [[ "$(psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres -tAc "SELECT 1 FROM pg_database WHERE datname='${VECTOR_DB_NAME}'")" != "1" ]]; then
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres -c "CREATE DATABASE \"${VECTOR_DB_NAME}\" OWNER \"${POSTGRES_USER}\";"
fi

# Enable pgvector extension in vector DB.
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$VECTOR_DB_NAME" -c "CREATE EXTENSION IF NOT EXISTS vector;"
