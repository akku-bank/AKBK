from __future__ import annotations

import hashlib
import html
import json
import os
import re
import ssl
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ET
from datetime import datetime, timezone
from email.utils import parsedate_to_datetime
from typing import Iterable

import psycopg2
from airflow import DAG
from airflow.operators.python import PythonOperator


def _env(name: str, default: str) -> str:
    value = os.getenv(name)
    return value if value else default


def _required_env(name: str) -> str:

    value = os.getenv(name)
    if not value:
        raise RuntimeError(f"missing required environment variable: {name}")
    return value


RSS_KEYWORDS = [
    keyword.strip()
    for keyword in ("경제", "금융")
    if keyword.strip()
]
RSS_REGION = "KR"
RSS_LANG = "ko"
OPENAI_API_KEY = _required_env("OPENAI_API_KEY")
OPENAI_EMBED_MODEL = "text-embedding-3-small"
OPENAI_EMBED_DIM = "1536"

# require vector database credentials explicitly to avoid accidental
# defaults that neutralise secret management
VECTOR_DB_HOST = _required_env("VECTOR_DB_HOST")
VECTOR_DB_PORT = int(_required_env("VECTOR_DB_PORT"))
VECTOR_DB_NAME = _required_env("VECTOR_DB_NAME")
VECTOR_DB_USER = _required_env("DB_USERNAME")
VECTOR_DB_PASSWORD = _required_env("DB_PASSWORD")

USER_AGENT = "Mozilla/5.0 (compatible; AKBK-NewsCrawler/1.0)"


def _rss_url(keyword: str) -> str:
    query = urllib.parse.quote(keyword)
    return (
        f"https://news.google.com/rss/search?q={query}"
        f"&hl={RSS_LANG}&gl={RSS_REGION}&ceid={RSS_REGION}:{RSS_LANG}"
    )


def _safe_text(value: str | None) -> str:
    return (value or "").strip()


def _strip_html(value: str | None) -> str:
    raw = _safe_text(value)
    if not raw:
        return ""
    no_tags = re.sub(r"<[^>]+>", " ", raw)
    return re.sub(r"\s+", " ", html.unescape(no_tags)).strip()


def _parse_published_at(raw: str | None) -> datetime:
    if not raw:
        return datetime.now(timezone.utc)
    try:
        dt = parsedate_to_datetime(raw)
        if dt.tzinfo is None:
            return dt.replace(tzinfo=timezone.utc)
        return dt.astimezone(timezone.utc)
    except (TypeError, ValueError):
        return datetime.now(timezone.utc)


def _fetch_rss_items(keyword: str) -> Iterable[dict]:
    request = urllib.request.Request(_rss_url(keyword), headers={"User-Agent": USER_AGENT})
    context = ssl.create_default_context()
    with urllib.request.urlopen(request, timeout=30, context=context) as response:
        xml_bytes = response.read()

    root = ET.fromstring(xml_bytes)
    channel = root.find("channel")
    if channel is None:
        return []

    rows: list[dict] = []
    for item in channel.findall("item"):
        title = _safe_text(item.findtext("title"))
        link = _safe_text(item.findtext("link"))
        source = _safe_text(item.findtext("source")) or "Google News"
        pub_date = _parse_published_at(item.findtext("pubDate"))
        description = _strip_html(item.findtext("description"))

        if not title or not link:
            continue

        news_id = hashlib.sha256(link.encode("utf-8")).hexdigest()
        content = f"{title}\n\n{description}".strip()

        rows.append(
            {
                "news_id": news_id,
                "keyword": keyword,
                "title": title,
                "url": link,
                "source": source,
                "published_at": pub_date,
                "content": content,
            }
        )

    return rows


def _embed_text(text: str) -> list[float] | None:
    if not OPENAI_API_KEY:
        return None

    payload = {
        "model": OPENAI_EMBED_MODEL,
        "input": text,
    }

    data = json.dumps(payload).encode("utf-8")
    request = urllib.request.Request(
        "https://api.openai.com/v1/embeddings",
        data=data,
        headers={
            "Authorization": f"Bearer {OPENAI_API_KEY}",
            "Content-Type": "application/json",
        },
        method="POST",
    )

    context = ssl.create_default_context()
    with urllib.request.urlopen(request, timeout=40, context=context) as response:
        parsed = json.loads(response.read().decode("utf-8"))

    vector = parsed["data"][0]["embedding"]
    if len(vector) != OPENAI_EMBED_DIM:
        raise ValueError(
            f"embedding dimension mismatch: expected {OPENAI_EMBED_DIM}, got {len(vector)}"
        )
    return vector


def _vector_literal(vector: list[float] | None) -> str | None:
    if vector is None:
        return None
    return "[" + ",".join(f"{x:.8f}" for x in vector) + "]"


def _crawl_and_store_news() -> None:
    conn = psycopg2.connect(
        host=VECTOR_DB_HOST,
        port=VECTOR_DB_PORT,
        dbname=VECTOR_DB_NAME,
        user=VECTOR_DB_USER,
        password=VECTOR_DB_PASSWORD,
    )
    conn.autocommit = False

    total_inserted = 0
    total_updated = 0

    try:
        with conn.cursor() as cur:
            cur.execute("CREATE EXTENSION IF NOT EXISTS vector;")
            cur.execute(
                f"""
                CREATE TABLE IF NOT EXISTS news_documents (
                    news_id TEXT PRIMARY KEY,
                    keyword TEXT NOT NULL,
                    title TEXT NOT NULL,
                    url TEXT NOT NULL,
                    source TEXT,
                    published_at TIMESTAMPTZ,
                    content TEXT,
                    embedding vector({OPENAI_EMBED_DIM}),
                    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
                );
                """
            )

            for keyword in RSS_KEYWORDS:
                for row in _fetch_rss_items(keyword):
                    vector = None
                    if row["content"]:
                        try:
                            vector = _embed_text(row["content"])
                        except Exception as exc:
                            print(f"embedding skipped for {row['url']}: {exc}")

                    embedding_literal = _vector_literal(vector)

                    cur.execute(
                        """
                        INSERT INTO news_documents (
                            news_id,
                            keyword,
                            title,
                            url,
                            source,
                            published_at,
                            content,
                            embedding,
                            updated_at
                        ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s::vector, NOW())
                        ON CONFLICT (news_id) DO UPDATE SET
                            keyword = EXCLUDED.keyword,
                            title = EXCLUDED.title,
                            url = EXCLUDED.url,
                            source = EXCLUDED.source,
                            published_at = EXCLUDED.published_at,
                            content = EXCLUDED.content,
                            embedding = COALESCE(EXCLUDED.embedding, news_documents.embedding),
                            updated_at = NOW()
                        RETURNING (xmax = 0) AS inserted;
                        """,
                        (
                            row["news_id"],
                            row["keyword"],
                            row["title"],
                            row["url"],
                            row["source"],
                            row["published_at"],
                            row["content"],
                            embedding_literal,
                        ),
                    )
                    inserted = bool(cur.fetchone()[0])

                    if inserted:
                        total_inserted += 1
                    else:
                        total_updated += 1

        conn.commit()
        print(
            f"news crawl finished: inserted={total_inserted}, updated={total_updated}, keywords={len(RSS_KEYWORDS)}"
        )
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()


default_args = {
    "owner": "airflow",
    "depends_on_past": False,
    "retries": 1,
}

with DAG(
    dag_id="news_rss_to_vectordb",
    default_args=default_args,
    start_date=datetime(2026, 1, 1),
    schedule="0 6 * * *",
    catchup=False,
    max_active_runs=1,
    tags=["rss", "news", "vectordb"],
) as dag:
    crawl_and_store_task = PythonOperator(
        task_id="crawl_and_store_news",
        python_callable=_crawl_and_store_news,
    )

    crawl_and_store_task
