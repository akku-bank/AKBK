FROM jenkins/jenkins:lts

USER root

# 1. 필수 도구 설치
RUN apt-get update && apt-get install -y curl

# 2. 가이드에 있던 CA 인증서 미리 다운로드 (수동 작업 대체)
RUN mkdir -p /var/jenkins_home/update-center-rootCAs && \
    curl -L https://cdn.jsdelivr.net/gh/lework/jenkins-update-center/rootCA/update-center.crt -o /var/jenkins_home/update-center-rootCAs/update-center.crt && \
    chown -R jenkins:jenkins /var/jenkins_home/update-center-rootCAs

# 3. 미러 사이트 설정 파일을 미리 생성 (수동 수정 대체)
RUN echo '<?xml version="1.0" encoding="UTF-8"?><sites><site><id>default</id><url>https://raw.githubusercontent.com/lework/jenkins-update-center/master/updates/tencent/update-center.json</url></site></sites>' > /usr/share/jenkins/ref/hudson.model.UpdateCenter.xml

USER jenkins
