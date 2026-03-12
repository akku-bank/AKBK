from datetime import datetime
import sys

from airflow import DAG
from airflow.operators.python import PythonOperator

# docker-compose에서 /opt/airflow/lib 로 마운트한 공통 로직 경로
if "/opt/airflow/lib" not in sys.path:
    sys.path.append("/opt/airflow/lib")

from news_pipeline import collect_news, filter_new_news, crawl_news, embed_and_upsert_news


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
    collect_news_task = PythonOperator(
        task_id="collect_news",
        python_callable=collect_news,
    )

    filter_new_news_task = PythonOperator(
        task_id="filter_new_news",
        python_callable=filter_new_news,
        op_args=[collect_news_task.output],
    )

    crawl_news_task = PythonOperator(
        task_id="crawl_news",
        python_callable=crawl_news,
        op_args=[filter_new_news_task.output],
    )

    embed_and_upsert_news_task = PythonOperator(
        task_id="embed_and_upsert_news",
        python_callable=embed_and_upsert_news,
        op_args=[crawl_news_task.output],
    )

    collect_news_task >> filter_new_news_task >> crawl_news_task >> embed_and_upsert_news_task
