from __future__ import annotations

import hashlib
import http.client
import html
import json
import os
import re
import ssl
import time
import urllib.error
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ET
from bs4 import BeautifulSoup  # using BeautifulSoup for article extraction
from datetime import datetime, timezone
from email.utils import parsedate_to_datetime
from typing import Iterable, Optional, List, Dict, Any, Tuple

import psycopg2

def _env(name: str, default: str) -> str:
    # 환경변수가 없으면 기본값을 사용한다.
    value = os.getenv(name)
    return value if value else default

RSS_KEYWORDS = [
    keyword.strip()
    for keyword in _env("RSS_KEYWORDS", "경제,금융").split(",")
    if keyword.strip()
]
# Google News RSS 조회 기본 옵션
RSS_REGION = "KR"
RSS_LANG = "ko"

# Docker-compose sets OPENAI_API_KEY=${GMS_API_KEY};
# so just prefer OPENAI_API_KEY and fall back to GMS_API_KEY for safety.
OPENAI_API_KEY = os.getenv("OPENAI_API_KEY") or os.getenv("GMS_API_KEY", "")
OPENAI_API_KEY = OPENAI_API_KEY.strip()

# GMS 새 API 명세에서는 모델 이름에 "models/..." prefix를 붙인다.
OPENAI_EMBED_MODEL = "text-embedding-3-smal"
OPENAI_EMBED_DIM = "1536"

# docker compose 기본값(postgres:5432)에 맞춘 벡터 DB 연결 정보
VECTOR_DB_HOST = _env("VECTOR_DB_HOST", "postgres").strip()
VECTOR_DB_PORT = int(_env("VECTOR_DB_PORT", "5432").strip())
VECTOR_DB_NAME = _env("VECTOR_DB_NAME", "vector_db").strip()
VECTOR_DB_USER = _env("DB_USERNAME", "app_user").strip()
VECTOR_DB_PASSWORD = _env("DB_PASSWORD", "app_pw").strip()

USER_AGENT = "Mozilla/5.0 (compatible; AKBK-NewsCrawler/1.0)"
MAX_CONTENT_CHARS = int(_env("MAX_CONTENT_CHARS", "8000").strip())
CRAWL_TIMEOUT_SEC = int(_env("CRAWL_TIMEOUT_SEC", "12").strip())
EMBED_MAX_CHARS = int(_env("EMBED_MAX_CHARS", "3000").strip())
EMBED_RETRIES = int(_env("EMBED_RETRIES", "3").strip())

# =============================================================================
# =============================================================================
def _rss_url(keyword: str) -> str:
    query = urllib.parse.quote(keyword)
    return (
        f"https://news.google.com/rss/search?q={query}"
        f"&hl={RSS_LANG}&gl={RSS_REGION}&ceid={RSS_REGION}:{RSS_LANG}"
    )


def _safe_text(value: Optional[str]) -> str:
    return (value or "").strip()


def _strip_html(value: Optional[str]) -> str:
    raw = _safe_text(value)
    if not raw:
        return ""
    no_tags = re.sub(r"<[^>]+>", " ", raw)
    return re.sub(r"\s+", " ", html.unescape(no_tags)).strip()


def _download_html(url: str) -> tuple[str, str]:
    request = urllib.request.Request(
        url,
        headers={
            "User-Agent": USER_AGENT,
            "Accept-Language": "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7",
        },
    )
    context = ssl.create_default_context()
    with urllib.request.urlopen(request, timeout=CRAWL_TIMEOUT_SEC, context=context) as response:
        final_url = response.geturl()
        raw = response.read()
        content_type = response.headers.get("Content-Type", "")
        match = re.search(r"charset=([a-zA-Z0-9_-]+)", content_type, re.IGNORECASE)
        charset = match.group(1) if match else "utf-8"
    return final_url, raw.decode(charset, errors="ignore")


def _extract_article_text(html_doc: str) -> str:
    # use BeautifulSoup to grab article or paragraphs more robustly
    soup = BeautifulSoup(html_doc, "html.parser")
    article = soup.find("article")
    text = ""
    if article:
        text = article.get_text(separator=" ", strip=True)
    else:
        # fallback to all <p> tags
        paragraphs = soup.find_all("p")
        text = " ".join(p.get_text(separator=" ", strip=True) for p in paragraphs)
    return text.strip()


def _fetch_article_content(url: str, title: str, description: str) -> Optional[Tuple[str, str]]:
    try:
        final_url, html_doc = _download_html(url)
        body = _extract_article_text(html_doc)
        if body:
            merged = f"{title}\n\n{body}".strip()
            return final_url, merged[:MAX_CONTENT_CHARS]
        # if extraction failed, fallback to description
        return final_url, f"{title}\n\n{description}".strip()[:MAX_CONTENT_CHARS]
    except Exception as exc:
        print(f"article fetch failed, skip row: {url} ({exc})")
        return None


def _parse_published_at(raw: Optional[str]) -> datetime:
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
    # 키워드별 RSS를 읽고 메타데이터만 수집한다.
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

        rows.append(
            {
                "keyword": keyword,
                "title": title,
                "url": link,
                "source": source,
                "published_at": pub_date,
                "description": description,
            }
        )

    return rows


def collect_news() -> List[Dict[str, Any]]:
    # 1단계: RSS 메타데이터를 수집하고 XCom 저장 가능한 형태로 변환한다.
    rows: List[Dict[str, Any]] = []
    for keyword in RSS_KEYWORDS:
        for row in _fetch_rss_items(keyword):
            normalized = dict(row)
            # datetime은 XCom 직렬화를 위해 ISO 문자열로 전달한다.
            normalized["published_at"] = row["published_at"].isoformat()
            rows.append(normalized)
    print(f"collect finished: rows={len(rows)}, keywords={len(RSS_KEYWORDS)}")
    return rows


def filter_new_news(rows: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
    # 2단계: DB에 이미 있는 URL은 제외하고 신규 기사만 다음 단계로 전달한다.
    if not rows:
        print("filter finished: input=0, new=0, skipped_existing=0")
        return rows

    urls = [str(row.get("url", "")).strip() for row in rows if str(row.get("url", "")).strip()]
    if not urls:
        print(f"filter finished: input={len(rows)}, new=0, skipped_existing={len(rows)}")
        return []

    conn = psycopg2.connect(
        host=VECTOR_DB_HOST,
        port=VECTOR_DB_PORT,
        dbname=VECTOR_DB_NAME,
        user=VECTOR_DB_USER,
        password=VECTOR_DB_PASSWORD,
    )
    try:
        with conn.cursor() as cur:
            _ensure_schema(cur)
            cur.execute("SELECT url FROM news_documents WHERE url = ANY(%s)", (urls,))
            existing_urls = {row[0] for row in cur.fetchall()}
    finally:
        conn.close()

    filtered = [row for row in rows if str(row.get("url", "")).strip() not in existing_urls]
    print(
        f"filter finished: input={len(rows)}, new={len(filtered)}, skipped_existing={len(rows) - len(filtered)}"
    )
    return filtered


def crawl_news(rows: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
    # 3단계: 기사 URL에 접속해서 원문을 수집하고 news_id를 만든다.
    crawled: List[Dict[str, Any]] = []
    print(
        f"crawl start: input_rows={len(rows)}, timeout={CRAWL_TIMEOUT_SEC}s"
    )
    skipped_count = 0
    for idx, row in enumerate(rows, start=1):
        title = str(row.get("title", ""))
        description = str(row.get("description", ""))
        original_url = str(row.get("url", ""))
        fetched = _fetch_article_content(original_url, title, description)
        if fetched is None:
            skipped_count += 1
            continue

        final_url, content = fetched
        next_row = dict(row)
        next_row["url"] = final_url
        next_row["content"] = content
        next_row["news_id"] = hashlib.sha256(final_url.encode("utf-8")).hexdigest()
        crawled.append(next_row)
        if idx % 10 == 0:
            print(f"crawl progress: {idx}/{len(rows)}")
    print(f"crawl finished: rows={len(crawled)}, skipped={skipped_count}")
    return crawled


def _embed_text(text: str) -> Optional[list[float]]:
    if not OPENAI_API_KEY:
        return None

    payload_input = text[:EMBED_MAX_CHARS]
    # GMS 새 API 포맷에 맞춘 요청 바디와 URL 구성
    model = OPENAI_EMBED_MODEL
    payload = {
        "model": model,
        "content": {"parts": [{"text": payload_input}]},
    }

    # construct URL according to curl example; include proper path and a slash
    url = (
        f"https://gms.ssafy.io/gmsapi/api.openai.com/v1/embeddings"
        f"models/{urllib.parse.quote(model)}:embedContent?key={urllib.parse.quote(OPENAI_API_KEY)}"
    )

    context = ssl.create_default_context()
    parsed = None
    last_error: Optional[Exception] = None
    for attempt in range(1, EMBED_RETRIES + 1):
        try:
            data = json.dumps(payload).encode("utf-8")
            request = urllib.request.Request(
                url,
                data=data,
                headers={
                    "Content-Type": "application/json",
                },
                method="POST",
            )
            with urllib.request.urlopen(request, timeout=40, context=context) as response:
                parsed = json.loads(response.read().decode("utf-8"))
                break
        except urllib.error.HTTPError as exc:
            body = exc.read().decode("utf-8", errors="ignore")
            raise RuntimeError(f"embedding http error {exc.code}: {body[:500]}")
        except (urllib.error.URLError, TimeoutError, http.client.IncompleteRead, json.JSONDecodeError) as exc:
            last_error = exc
            if attempt < EMBED_RETRIES:
                time.sleep(1.0 * attempt)
                continue
            raise RuntimeError(f"embedding retry exhausted: {exc}")

    if parsed is None:
        raise RuntimeError(f"embedding failed without response: {last_error}")

    vector = parsed["data"][0]["embedding"]
    if len(vector) != OPENAI_EMBED_DIM:
        raise ValueError(
            f"embedding dimension mismatch: expected {OPENAI_EMBED_DIM}, got {len(vector)}"
        )
    return vector


def _vector_literal(vector: Optional[list[float]]) -> Optional[str]:
    if vector is None:
        return None
    return "[" + ",".join(f"{x:.8f}" for x in vector) + "]"


def _ensure_schema(cur: psycopg2.extensions.cursor) -> None:
    # pgvector 확장/테이블을 없으면 생성한다.
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


def _upsert_single_news(cur: psycopg2.extensions.cursor, row: Dict[str, Any]) -> bool:
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
            row.get("embedding_literal"),
        ),
    )
    return bool(cur.fetchone()[0])


def embed_and_upsert_news(rows: List[Dict[str, Any]]) -> Dict[str, int]:
    # 4단계: 임베딩 1건 완료 즉시 DB upsert 한다.
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
    embedded_count = 0
    skipped_count = 0

    try:
        with conn.cursor() as cur:
            _ensure_schema(cur)

            if not OPENAI_API_KEY:
                print("embed disabled: OPENAI_API_KEY/GMS_API_KEY/GMS_KEY is empty")

            for idx, row in enumerate(rows, start=1):
                next_row = dict(row)
                vector = None
                if row.get("content"):
                    try:
                        vector = _embed_text(str(row["content"]))
                    except Exception as exc:
                        print(f"embedding skipped for {row.get('url')}: {exc}")

                next_row["embedding_literal"] = _vector_literal(vector)
                if next_row["embedding_literal"]:
                    embedded_count += 1
                else:
                    skipped_count += 1

                inserted = _upsert_single_news(cur, next_row)
                conn.commit()

                if inserted:
                    total_inserted += 1
                else:
                    total_updated += 1
                if idx % 10 == 0:
                    print(
                        f"embed+upsert progress: {idx}/{len(rows)} inserted={total_inserted} updated={total_updated}"
                    )
        result = {
            "rows": len(rows),
            "embedded": embedded_count,
            "embed_skipped": skipped_count,
            "inserted": total_inserted,
            "updated": total_updated,
        }
        print(f"embed+upsert finished: {result}")
        return result
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()
