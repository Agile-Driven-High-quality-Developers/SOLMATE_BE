# 1단계 - 빌드
FROM gradle:8.14 AS builder

WORKDIR /app

# 의존성 캐시 (소스 변경돼도 의존성은 다시 안 받음)
COPY build.gradle settings.gradle ./
COPY gradle ./gradle
RUN gradle dependencies --no-daemon || true

# 소스 복사 + 빌드
COPY src ./src
RUN gradle build -x test --no-daemon

# 2단계 - 실행
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# 빌드 결과물만 복사 (1단계 gradle이나 소스는 버림)
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]