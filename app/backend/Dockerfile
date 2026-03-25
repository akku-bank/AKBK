# --- 1. Builder Stage (빌드 단계) ---
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /build
# 전체 소스코드를 복사 (docker-compose context 기준)
COPY . .

# 권한 부여 및 빌드 (테스트 제외)
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew
RUN ./gradlew clean bootJar -x test

# --- 2. Runtime Stage (실행 단계) ---
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 보안: root 권한 대신 애플리케이션 전용 비루트 사용자(spring) 생성
RUN addgroup -S spring && adduser -S spring -G spring

# Builder 단계에서 생성된 jar 파일만 가져와서 spring 사용자에게 소유권 부여
COPY --from=builder --chown=spring:spring /build/build/libs/*SNAPSHOT.jar app.jar

# 컨테이너 실행 권한을 spring 사용자로 변경
USER spring:spring

ENTRYPOINT ["java", "-jar", "app.jar"]