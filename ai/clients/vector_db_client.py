import asyncio
from typing import Any

import psycopg2
from psycopg2.extras import RealDictCursor

from ai.core.config import Settings


class VectorDbClient:
    # 현재 프로젝트에서는 Airflow가 적재하는 news_documents 테이블을 검색 대상으로 사용합니다.
    # 스키마가 자주 바뀌지 않는다고 가정하고, 테이블/컬럼명은 환경변수 대신 코드 상수로 고정합니다.
    TABLE_NAME = "news_documents"
    ID_COLUMN = "news_id"
    CONTENT_COLUMN = "content"
    EMBEDDING_COLUMN = "embedding"
    METADATA_SOURCE = "news_documents"
    EMBEDDING_DIMENSION = 1536

    def __init__(self) -> None:
        # 현재는 테이블/컬럼 구조를 고정해서 사용합니다.
        self.table_name = self.TABLE_NAME
        self.id_column = self.ID_COLUMN
        self.content_column = self.CONTENT_COLUMN
        self.embedding_column = self.EMBEDDING_COLUMN
        self.metadata_source = self.METADATA_SOURCE
        self.embedding_dimension = self.EMBEDDING_DIMENSION

    async def search_similar_by_embedding(
        self,
        query_embedding: list[float],
        top_k: int = 5,
    ) -> list[dict[str, Any]]:
        # 서비스 계층은 async/await 흐름을 사용하므로 외부 인터페이스도 비동기로 유지합니다.
        # 다만 psycopg2 자체는 동기 드라이버이기 때문에, 이벤트 루프를 막지 않도록
        # 실제 DB 조회는 별도 스레드에서 실행합니다.
        return await asyncio.to_thread(
            self._search_similar_by_embedding_sync,
            query_embedding,
            top_k,
        )

    # 벡터 검색을 동기적으로 수행하는 내부 메서드입니다. 입력 검증과 예외 처리를 포함합니다.
    def _search_similar_by_embedding_sync(
        self,
        query_embedding: list[float],
        top_k: int,
    ) -> list[dict[str, Any]]:
        # DB에 붙기 전에 입력값을 먼저 검증해서,
        # 잘못된 벡터나 잘못된 검색 개수 때문에 불필요한 쿼리가 실행되지 않도록 합니다.
        self._validate_query_embedding(query_embedding)

        if top_k < 1:
            raise ValueError("top_k는 1 이상의 정수여야 합니다.")

        # pgvector는 파이썬 리스트를 직접 받지 않으므로,
        # '[0.1,0.2,...]' 형태의 vector 리터럴 문자열로 변환해서 전달합니다.
        query_vector = self._to_pgvector_literal(query_embedding)
        sql = f"""
            SELECT
                {self.id_column} AS id,
                {self.content_column} AS content,
                -- <=> 는 pgvector의 코사인 거리 연산자입니다.
                -- 거리가 작을수록 유사하므로, 1 - distance 형태로 점수처럼 변환합니다.
                GREATEST(1 - ({self.embedding_column} <=> %s::vector), 0) AS score,
                json_build_object(
                    'source', %s,
                    'table', %s
                ) AS metadata
            FROM {self.table_name}
            WHERE {self.embedding_column} IS NOT NULL
            -- 코사인 거리가 가장 작은 문서부터 정렬하면 가장 유사한 문서가 먼저 나옵니다.
            ORDER BY {self.embedding_column} <=> %s::vector
            LIMIT %s
        """

        try:
            # 설정 모듈에서 읽어온 접속 정보로 PostgreSQL/pgvector에 연결합니다.
            with psycopg2.connect(Settings.vector_db_dsn()) as conn:
                with conn.cursor(cursor_factory=RealDictCursor) as cur:
                    # 같은 query vector를 score 계산과 정렬 기준에 모두 사용합니다.
                    cur.execute(
                        sql,
                        (
                            query_vector,
                            self.metadata_source,
                            self.table_name,
                            query_vector,
                            top_k,
                        ),
                    )
                    # RealDictCursor를 사용하므로 각 row를 dict처럼 읽을 수 있습니다.
                    rows = cur.fetchall()
        except psycopg2.Error as exc:
            # 상위 서비스에서 원인을 파악하기 쉽도록 벡터 DB 검색 실패라는 문맥을 덧붙입니다.
            raise RuntimeError(f"벡터 DB 검색 중 오류가 발생했습니다: {exc}") from exc

        # DB 응답을 서비스 계층에서 바로 사용할 수 있는 공통 형태로 정규화합니다.
        return [self._normalize_row(row) for row in rows]

    def _validate_query_embedding(self, query_embedding: list[float]) -> None:
        # 질문 임베딩은 반드시 존재해야 합니다.
        if not query_embedding:
            raise ValueError("query_embedding은 비어 있을 수 없습니다.")

        # 현재 저장된 문서 임베딩 차원과 질의 임베딩 차원이 맞아야
        # pgvector 거리 계산이 가능합니다.
        if len(query_embedding) != self.embedding_dimension:
            raise ValueError(
                f"query_embedding 차원({len(query_embedding)})이 설정된 차원({self.embedding_dimension})과 다릅니다."
            )

        # 벡터 원소는 숫자여야 하므로 문자열이나 None 같은 값은 미리 차단합니다.
        for value in query_embedding:
            if not isinstance(value, (int, float)):
                raise ValueError("query_embedding에는 숫자만 포함되어야 합니다.")

    def _to_pgvector_literal(self, query_embedding: list[float]) -> str:
        # 예: [0.1, 0.2] -> "[0.100000000000,0.200000000000]"
        # pgvector SQL 파라미터에 그대로 바인딩할 수 있는 문자열 형태로 변환합니다.
        return "[" + ",".join(f"{float(value):.12f}" for value in query_embedding) + "]"

    def _normalize_row(self, row: dict[str, Any]) -> dict[str, Any]:
        # 서비스 계층은 DB 스키마를 직접 알 필요가 없도록,
        # 검색 결과를 id/content/score/metadata 구조로 통일해서 반환합니다.
        return {
            "id": row.get("id"),
            "content": row.get("content") or "",
            "score": float(row.get("score") or 0.0),
            "metadata": row.get("metadata") or {},
        }
