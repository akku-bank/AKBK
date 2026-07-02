"""
RAG 테스트용 더미 뉴스 데이터 삽입 스크립트.
ai-server 컨테이너 내부에서 실행:
  docker exec ai_server python ai/db/seed_news.py
"""

import sys
sys.path.insert(0, "/app")

import psycopg2
from openai import OpenAI
from ai.core.config import settings

EMBED_DIM = 1536
EMBED_MODEL = "text-embedding-3-small"

DUMMY_NEWS = [
    {
        "news_id": "dummy-001",
        "title": "서브프라임 모기지 사태, 금융위기의 시작",
        "url": "https://dummy.news/001",
        "published_at": "2008-09-15T00:00:00+00:00",
        "content": (
            "서브프라임 모기지 사태, 금융위기의 시작. "
            "2008년 미국에서 신용도가 낮은 차입자에게 무분별하게 주택담보대출(모기지)을 제공한 결과, "
            "대출 부실이 확산되며 대형 투자은행 리먼브라더스가 파산하였다. "
            "이 사건은 전 세계 금융시장에 충격을 주며 글로벌 금융위기로 번졌다. "
            "은행들은 상환 능력을 충분히 검토하지 않고 대출을 남발했고, "
            "이를 기초자산으로 한 파생상품이 전 세계 금융기관에 퍼져 있었기 때문에 피해가 극대화되었다."
        ),
    },
    {
        "news_id": "dummy-002",
        "title": "금융위기 이후 각국 중앙은행의 금리 인하 조치",
        "url": "https://dummy.news/002",
        "published_at": "2008-10-10T00:00:00+00:00",
        "content": (
            "금융위기 이후 각국 중앙은행의 금리 인하 조치. "
            "2008년 금융위기 직후 미국 연방준비제도(Fed)를 비롯한 각국 중앙은행은 "
            "기준금리를 대폭 인하하고 시장에 유동성을 공급하는 양적완화 정책을 시행했다. "
            "금리를 낮추면 기업과 가계의 대출 부담이 줄어들고 소비와 투자가 살아날 수 있다는 판단에서였다. "
            "한국은행도 2008년 하반기 기준금리를 연속으로 인하하며 경기 침체에 대응하였다."
        ),
    },
    {
        "news_id": "dummy-003",
        "title": "최근 가계부채 급증, 금융당국 대출 규제 강화",
        "url": "https://dummy.news/003",
        "published_at": "2024-11-05T00:00:00+00:00",
        "content": (
            "최근 가계부채 급증, 금융당국 대출 규제 강화. "
            "2024년 들어 국내 가계부채가 다시 빠르게 늘어나면서 금융당국이 대출 규제를 강화하고 있다. "
            "은행권은 주택담보대출 한도를 축소하고 DSR(총부채원리금상환비율) 기준을 엄격히 적용하기 시작했다. "
            "전문가들은 과도한 부채가 금융 시스템의 불안정성을 높일 수 있다고 경고하며 "
            "상환 능력 심사를 강화해야 한다고 강조하고 있다."
        ),
    },
    {
        "news_id": "dummy-004",
        "title": "요즘 청소년 금융 교육 중요성 부각, 경제 이해력 키워야",
        "url": "https://dummy.news/004",
        "published_at": "2025-01-20T00:00:00+00:00",
        "content": (
            "요즘 청소년 금융 교육 중요성 부각, 경제 이해력 키워야. "
            "최근 청소년들의 금융 이해력 부족 문제가 사회적 화두로 떠오르고 있다. "
            "전문가들은 저축, 투자, 대출의 개념을 어릴 때부터 가르쳐야 한다고 강조한다. "
            "특히 신용카드 남용과 충동 소비 문제가 심각한 만큼, "
            "예산 계획과 절약 습관을 어릴 때부터 기르는 것이 중요하다고 지적하고 있다."
        ),
    },
    {
        "news_id": "dummy-005",
        "title": "미국 금리 동결, 한국 금융시장 영향 촉각",
        "url": "https://dummy.news/005",
        "published_at": "2025-03-20T00:00:00+00:00",
        "content": (
            "미국 금리 동결, 한국 금융시장 영향 촉각. "
            "미국 연방준비제도가 2025년 3월 기준금리를 동결하면서 한국 금융시장도 영향을 받고 있다. "
            "원달러 환율이 소폭 하락하였고 외국인 투자자들의 국내 채권 매수세가 이어지고 있다. "
            "한국은행은 국내 물가와 경기 상황을 종합적으로 고려해 독자적인 금리 결정을 내릴 것이라고 밝혔다. "
            "시장에서는 연내 한 차례 금리 인하 가능성을 열어두고 있는 상황이다."
        ),
    },
]


def get_embedding(client: OpenAI, text: str) -> list[float]:
    response = client.embeddings.create(input=[text[:1000]], model=EMBED_MODEL)
    return response.data[0].embedding


def ensure_schema(cur) -> None:
    cur.execute("CREATE EXTENSION IF NOT EXISTS vector;")
    cur.execute(
        f"""
        CREATE TABLE IF NOT EXISTS news_documents (
            news_id TEXT PRIMARY KEY,
            title TEXT NOT NULL,
            url TEXT NOT NULL,
            published_at TIMESTAMPTZ,
            content TEXT,
            embedding vector({EMBED_DIM}),
            created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
            updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
        );
        """
    )


def main():
    client = OpenAI(
        api_key=settings.require_openai_api_key(),
        base_url=settings.openai_base_url,
    )

    print("=== news_documents 테이블 생성 중 ===")
    conn = psycopg2.connect(settings.vector_db_dsn())
    try:
        with conn.cursor() as cur:
            ensure_schema(cur)
        conn.commit()
        print("테이블 생성 완료\n")

        print(f"=== 더미 뉴스 {len(DUMMY_NEWS)}건 삽입 중 ===")
        with conn.cursor() as cur:
            for row in DUMMY_NEWS:
                print(f"임베딩 중: {row['title']}")
                vector = get_embedding(client, row["content"])
                embedding_literal = "[" + ",".join(f"{x:.8f}" for x in vector) + "]"

                cur.execute(
                    """
                    INSERT INTO news_documents (news_id, title, url, published_at, content, embedding)
                    VALUES (%s, %s, %s, %s, %s, %s::vector)
                    ON CONFLICT (news_id) DO UPDATE SET
                        content = EXCLUDED.content,
                        embedding = EXCLUDED.embedding,
                        updated_at = NOW();
                    """,
                    (row["news_id"], row["title"], row["url"], row["published_at"], row["content"], embedding_literal),
                )
                print(f"  완료: {row['news_id']}")

        conn.commit()
        print(f"\n총 {len(DUMMY_NEWS)}건 삽입 완료")
    finally:
        conn.close()


if __name__ == "__main__":
    main()
