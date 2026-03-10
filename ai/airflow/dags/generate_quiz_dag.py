import sys
import os
from datetime import datetime, timedelta
from airflow import DAG
from airflow.operators.python import PythonOperator

# 1. lib 폴더를 Python 경로에 추가하여 모듈 임포트 가능하게 설정
dag_dir = os.path.dirname(os.path.abspath(__file__))
lib_path = os.path.join(dag_dir, '../lib')
if lib_path not in sys.path:
    sys.path.insert(0, lib_path)

# 우리가 만든 LangGraph 앱 임포트
from graph import quiz_app

def generate_quiz_task(difficulty: str, **kwargs):
    """LangGraph 워크플로우를 실행하여 퀴즈를 생성하고 DB에 저장함"""
    initial_state = {
        "difficulty": difficulty,
        "quiz_data": None,
        "error_history": [],
        "retry_count": 0,
        "is_valid": False
    }
    
    print(f"🚀 [{difficulty.upper()}] 난이도 퀴즈 생성 시작...")
    result = quiz_app.invoke(initial_state)
    
    if result["is_valid"]:
        print(f"✅ {difficulty} 퀴즈 생성 및 저장 성공: {result['quiz_data'].topic}")
    else:
        print(f"❌ {difficulty} 퀴즈 생성 실패: {result['error_history']}")
        raise Exception(f"{difficulty} 퀴즈 생성 중 오류 발생")

# 2. DAG 기본 설정
default_args = {
    'owner': 'akkubaengku',
    'depends_on_past': False,
    'start_date': datetime(2024, 1, 1), # 적절한 시작 날짜로 수정 필요
    'retries': 1,
    'retry_delay': timedelta(minutes=5),
}

with DAG(
    'daily_economy_quiz_generation',
    default_args=default_args,
    description='매일 난이도별 경제 퀴즈 자동 생성',
    schedule_interval='0 4 * * *',  # 매일 새벽 4시 실행 (아이들 등교/등원 전)
    catchup=False,
    tags=['ai', 'quiz', 'langgraph'],
) as dag:

    # 3. 난이도별 태스크 생성
    difficulties = ['easy', 'medium', 'hard']
    
    for diff in difficulties:
        PythonOperator(
            task_id=f'generate_{diff}_quiz',
            python_callable=generate_quiz_task,
            op_kwargs={'difficulty': diff},
        )