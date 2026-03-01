import sys
import re
import json

def analyze(log_path):
    with open(log_path, 'r', encoding='utf-8') as f:
        log_content = f.read()

    # 팀별 키워드 매핑 (제미나이 추천 반영)
    rules = {
        'INFRA': [r'Docker', r'YAML', r'Kubernetes', r'K8s', r'context canceled', r'daemon not running'],
        'AI': [r'Langchain', r'LangGraph', r'RAG', r'Embedding', r'OpenAI', r'VectorDB'],
        'DATA': [r'Spark', r'Airflow', r'Kafka', r'Executor', r'DAG', r'ConsumerGroup'],
        'BACKEND': [r'Spring', r'MyBatis', r'JWT', r'JUnit', r'SQLException', r'Connection pool'],
        'FRONTEND': [r'React Native', r'Zustand', r'JS/TS', r'Node', r'npm', r'yarn']
    }

    target_team = "COMMON_OPS"
    # 로그 하단부(최신 에러)부터 우선순위 검색
    for team, keywords in rules.items():
        if any(re.search(kw, log_content, re.IGNORECASE) for kw in keywords):
            target_team = team
            break

    # 핵심 에러 문구 10줄 추출 (마지막 'Caused by' 혹은 'Error' 주변)
    error_lines = log_content.splitlines()
    summary = "\n".join(error_lines[-15:]) # 마지막 15줄 추출

    return target_team, summary

if __name__ == "__main__":
    team, msg = analyze(sys.argv[1])
    # 결과를 JSON으로 출력하여 젠킨스가 읽게 함
    print(json.dumps({"team": team, "summary": msg}))