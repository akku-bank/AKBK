import sys
import re
import json

def analyze():
    # 인자 순서: log_path, job_name, build_num, build_url
    log_path = sys.argv[1]
    job_name = sys.argv[2]
    build_num = sys.argv[3]
    build_url = sys.argv[4]

    with open(log_path, 'r', encoding='utf-8') as f:
        log_content = f.read()

    # 팀 분류 로직
    rules = {
        'INFRA': [r'Docker', r'YAML', r'context canceled'],
        'AI': [r'Langchain', r'LangGraph', r'VectorDB'],
        'DATA': [r'Spark', r'Airflow', r'Kafka'],
        'BACKEND': [r'Spring', r'MyBatis', r'NullPointerException', r'SQLException'],
        'FRONTEND': [r'React Native', r'Zustand']
    }

    target_team = "COMMON_OPS"
    for team, keywords in rules.items():
        if any(re.search(kw, log_content, re.IGNORECASE) for kw in keywords):
            target_team = team
            break

    # 마지막 15줄 추출
    summary = "\n".join(log_content.splitlines()[-15:])

    # Mattermost 페이로드 생성
    payload = {
        "username": "Jenkins-Analyzer",
        "icon_url": "https://jenkins.io/images/logos/jenkins/jenkins.png",
        "text": f"### 🚨 [{target_team}] 빌드 실패 알림\n**프로젝트:** {job_name}\n**빌드 번호:** #{build_num}\n**에러 요약:**\n```\n{summary}\n```\n\n[👉 상세 로그 확인하기]({build_url}console)"
    }

    # 파일로 저장 (Jenkins가 이걸 그대로 curl로 쏩니다)
    with open('mattermost_payload.json', 'w', encoding='utf-8') as f:
        json.dump(payload, f, ensure_ascii=False)

if __name__ == "__main__":
    analyze()