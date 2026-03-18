# /app/backend/Dockerfile
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# app/backend/build/libs 안의 jar 파일을 복사
ARG JAR_FILE=build/libs/*SNAPSHOT.jar
COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]