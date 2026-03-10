from __future__ import annotations

import hashlib
import os
import re
import urllib.parse
from datetime import datetime, timezone
from typing import Any, Dict, List
import time, random

import psycopg2
import requests
from bs4 import BeautifulSoup
from openai import OpenAI


# Required fixed constants
MAX_CONTENT_CHARS = 8000
EMBED_MAX_CHARS = 1000
EMBED_DIM = 1536

# Search settings
RSS_KEYWORDS = ["경제", "금융"]
USER_AGENT = (
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
    "AppleWebKit/537.36 (KHTML, like Gecko) "
    "Chrome/91.0.4472.114 Safari/537.36"
)

# DB settings
DB_HOST = "postgres"
DB_PORT = 5432
DB_NAME = os.getenv("VECTOR_DB_NAME", "vector_db")
DB_USER = os.getenv("DB_USERNAME", "app_user")
DB_PASSWORD = os.getenv("DB_PASSWORD", "app_pw")

# Embedding settings
GMS_BASE_URL = "https://gms.ssafy.io/gmsapi/api.openai.com/v1"
OPENAI_API_KEY = (os.getenv("OPENAI_API_KEY") or os.getenv("GMS_API_KEY") or "").strip()
OPENAI_MODEL = "text-embedding-3-small"
CLIENT = OpenAI(api_key=OPENAI_API_KEY, base_url=GMS_BASE_URL) if OPENAI_API_KEY else None


def _build_naver_search_url(query: str) -> str:
    return (
        "https://search.naver.com/search.naver?"
        f"ssc=tab.news.all&query={urllib.parse.quote(query)}&sort=1"
    )


def _ensure_schema(cur) -> None:
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


def _connect_db():
    return psycopg2.connect(
        host=DB_HOST,
        port=DB_PORT,
        dbname=DB_NAME,
        user=DB_USER,
        password=DB_PASSWORD,
    )


# 1) collect_news: 최근순 50페이지 네이버 뉴스 검색 결과에서 기사 url 수집
def get_naverlink(search_urls):
    naverlink_list = []
    seen = set()

    for i in search_urls:
        try:
            r = requests.get(i, timeout=5)
            time.sleep(random.randint(1, 3))
            soup = BeautifulSoup(r.text, "html.parser")
            # 검색 결과 카드의 기사 링크(제목/본문/네이버뉴스) 후보를 전부 수집
            links = []
            for a in soup.select('a[data-heatmap-target=".tit"], a[data-heatmap-target=".body"], a[data-heatmap-target=".nav"]'):
                href = (a.get("href") or "").strip()
                if href:
                    links.append(href)

            # 혹시 selector가 바뀐 경우를 대비한 fallback
            if not links:
                links = re.findall(r'href="(https?://[^"]+)"', r.text)

            for j in links:
                # 기사 링크만 통과
                if (
                    "n.news.naver.com/mnews/article/" in j
                    or "news.naver.com/main/read.naver" in j
                    or ("http" in j and "/article/" in j)
                ):
                    if j not in seen:
                        seen.add(j)
                        naverlink_list.append(j)
        except Exception as e:
            print(f"[collect_news] failed: {i} -> {e}")

    return naverlink_list


def collect_news() -> List[str]:
    rows: List[str] = []
    page_count = 50

    for keyword in RSS_KEYWORDS:
        base_url = _build_naver_search_url(keyword)
        search_urls = [f"{base_url}&start={1 + (idx * 10)}" for idx in range(page_count)]
        print(f"[collect_news] keyword={keyword} pages={page_count} first_url={search_urls[0]}")
        links = get_naverlink(search_urls)

        for link in links:
            rows.append(link)
            print(f"[collect_news][collected] keyword={keyword} url={link}")

    print(f"[collect_news] total_collected={len(rows)}")
    return rows

# 2) filter_new_news: DB에 이미 존재하는 URL 제거
def filter_new_news(rows: List[str]) -> List[str]:
    if not rows:
        print("filter finished: input=0, new=0")
        return []

    urls = [str(url).strip() for url in rows if str(url).strip()]
    if not urls:
        print(f"filter finished: input={len(rows)}, new=0")
        return []

    conn = _connect_db()
    try:
        with conn.cursor() as cur:
            _ensure_schema(cur)
            cur.execute("SELECT url FROM news_documents WHERE url = ANY(%s)", (urls,))
            existing_urls = {item[0] for item in cur.fetchall()}
    finally:
        conn.close()

    filtered = [url for url in urls if url not in existing_urls]
    for url in urls:
        if url in existing_urls:
            print(f"[filter_new_news][skip_existing] url={url}")
        else:
            print(f"[filter_new_news][pass_new] url={url}")
    print(f"filter finished: input={len(rows)}, new={len(filtered)}")
    return filtered


# 3) crawl_news: 기사 본문 크롤링
def crawl_news(rows: List[str]) -> List[Dict[str, Any]]:
    crawled: List[Dict[str, Any]] = []
    skipped_count = 0
    print(f"crawl start: input_rows={len(rows)}, timeout=3s")

    headers = {"User-Agent": USER_AGENT}

    for row in rows:
        url = str(row).strip()
        if not url:
            skipped_count += 1
            continue

        try:
            response = requests.get(url, headers=headers, timeout=3)
            response.raise_for_status()
            soup = BeautifulSoup(response.text, "html.parser")

            title_el = soup.select_one("h2#title_area span") or soup.select_one("div.media_end_head_title")
            if title_el:
                title = title_el.get_text(" ", strip=True)
            else:
                title_tag = soup.find("title")
                title = title_tag.get_text(" ", strip=True) if title_tag else "untitled"

            body = (
                soup.select_one("div#newsct_article > article#dic_area")
                or soup.select_one("article#dic_area")
                or soup.select_one("div#newsct_article")
                or soup.select_one("article")
            )
            if body is not None:
                full_text = body.get_text(separator=" ", strip=True)
            else:
                paragraphs = soup.find_all("p")
                full_text = " ".join([p.get_text() for p in paragraphs]).strip()

            full_text = re.sub(r"\s+", " ", full_text).strip()
            if len(full_text) < 50:
                raise Exception("Content too short")

            content = f"{title}. {full_text[:MAX_CONTENT_CHARS]}".strip()

            next_row = {
                "news_id": hashlib.sha256(url.encode("utf-8")).hexdigest(),
                "title": title,
                "url": url,
                "published_at": datetime.now(timezone.utc).isoformat(),
                "content": content,
            }
            crawled.append(next_row)
            print(f"[crawl_news][success] url={url} title={title}")

        except Exception as exc:
            skipped_count += 1
            print(f"[crawl_news][failed] url={url} reason={exc}")

    print(f"crawl finished: rows={len(crawled)}, skipped={skipped_count}")
    return crawled


# 4) embed_and_upsert_news: 기사 임베딩 후 DB에 업서트
def embed_and_upsert_news(rows: List[Dict[str, Any]]) -> Dict[str, int]:
    if not rows:
        result = {"rows": 0, "embedded": 0, "embed_skipped": 0, "inserted": 0, "updated": 0}
        print(f"embed+upsert finished: {result}")
        return result

    conn = _connect_db()
    conn.autocommit = False

    inserted = 0
    updated = 0
    embedded = 0
    embed_skipped = 0

    try:
        with conn.cursor() as cur:
            _ensure_schema(cur)

            if CLIENT is None:
                print("embed disabled: OPENAI_API_KEY or GMS_API_KEY is empty")

            for row in rows:
                vector = None
                if CLIENT is not None and row.get("content"):
                    try:
                        response = CLIENT.embeddings.create(
                            input=[str(row["content"])[:EMBED_MAX_CHARS]],
                            model=OPENAI_MODEL,
                        )
                        vector = response.data[0].embedding
                        if len(vector) != EMBED_DIM:
                            raise ValueError(
                                f"embedding dimension mismatch: expected {EMBED_DIM}, got {len(vector)}"
                            )
                    except Exception as exc:
                        print(f"embedding skipped for {row.get('url')}: {exc}")

                embedding_literal = None
                if vector is not None:
                    embedding_literal = "[" + ",".join(f"{x:.8f}" for x in vector) + "]"
                    embedded += 1
                else:
                    embed_skipped += 1

                cur.execute(
                    """
                    INSERT INTO news_documents (
                        news_id, title, url, published_at, content, embedding, updated_at
                    ) VALUES (%s, %s, %s, %s, %s, %s::vector, NOW())
                    ON CONFLICT (news_id) DO UPDATE SET
                        title = EXCLUDED.title,
                        url = EXCLUDED.url,
                        published_at = EXCLUDED.published_at,
                        content = EXCLUDED.content,
                        embedding = COALESCE(EXCLUDED.embedding, news_documents.embedding),
                        updated_at = NOW()
                    RETURNING (xmax = 0) AS inserted;
                    """,
                    (
                        row["news_id"],
                        row["title"],
                        row["url"],
                        row["published_at"],
                        row["content"],
                        embedding_literal,
                    ),
                )
                is_inserted = bool(cur.fetchone()[0])
                conn.commit()
                if is_inserted:
                    inserted += 1
                else:
                    updated += 1

        result = {
            "rows": len(rows),
            "embedded": embedded,
            "embed_skipped": embed_skipped,
            "inserted": inserted,
            "updated": updated,
        }
        print(f"embed+upsert finished: {result}")
        return result
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()
