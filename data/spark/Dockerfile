FROM bitnami/spark:3.5.0

USER root

# 필수 패키지 설치 (Redis 접속용 redis 라이브러리 포함)
RUN pip install --no-cache-dir redis

# Spark-Kafka 커넥터와 Postgres JDBC 드라이버 자동 다운로드 설정
# 실행 시 --packages 옵션으로 주입하는 것이 일반적이지만, 
# 미리 받아두고 싶다면 여기에 추가할 수 있습니다.

USER 1001