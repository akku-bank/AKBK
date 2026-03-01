FROM jenkins/jenkins:lts

USER root

# 1. 필수 도구 및 Python3, Docker Compose 설치
# Jenkins 컨테이너 안에서 docker-compose 명령어를 쓰기 위해 설치가 필요합니다.
RUN apt-get update && apt-get install -y \
    curl \
    python3 \
    python3-pip \
    docker-compose

# 2. 미러 사이트 CA 인증서 미리 다운로드
RUN mkdir -p /var/jenkins_home/update-center-rootCAs && \
    curl -L https://cdn.jsdelivr.net/gh/lework/jenkins-update-center/rootCA/update-center.crt -o /var/jenkins_home/update-center-rootCAs/update-center.crt && \
    chown -R jenkins:jenkins /var/jenkins_home/update-center-rootCAs

# 3. 미러 사이트 설정 파일 생성
RUN echo '<?xml version="1.0" encoding="UTF-8"?><sites><site><id>default</id><url>https://raw.githubusercontent.com/lework/jenkins-update-center/master/updates/tencent/update-center.json</url></site></sites>' > /usr/share/jenkins/ref/hudson.model.UpdateCenter.xml

USER jenkins