FROM jenkins/jenkins:lts

USER root

RUN apt-get update && apt-get install -y \
    curl \
    python3 \
    python3-pip \
    docker.io

# 미러 사이트 설정
RUN mkdir -p /var/jenkins_home/update-center-rootCAs && \
    curl -L https://cdn.jsdelivr.net/gh/lework/jenkins-update-center/rootCA/update-center.crt -o /var/jenkins_home/update-center-rootCAs/update-center.crt
