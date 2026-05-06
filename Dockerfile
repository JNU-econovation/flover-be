# ── Stage 1: 빌드 ──────────────────────────────────────────────
# gradle + JDK 21 이미지로 jar 파일 생성
FROM gradle:8-jdk21 AS builder

WORKDIR /app

# 의존성 캐시 레이어 분리 (소스 변경 시 의존성 재다운로드 방지)
COPY build.gradle settings.gradle ./
RUN gradle dependencies --no-daemon || true

COPY src ./src
RUN gradle bootJar --no-daemon -x test

# ── Stage 2: 실행 ──────────────────────────────────────────────
# JRE만 있는 가벼운 이미지로 jar 실행 (빌드 도구 제외)
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
