# 1. 빌드 단계 (Maven을 이용해 .jar 파일 생성)
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# 2. 실행 단계 (가벼운 Java 환경에서 실행)
FROM openjdk:17-jdk-slim
WORKDIR /app
# 빌드 단계에서 만든 .jar 파일을 복사해옴 (이름에 상관없이 target 폴더의 jar를 가져옴)
COPY --from=build /app/target/*.jar app.jar

# 8080 포트 열기
EXPOSE 8080

# 실행 명령어
ENTRYPOINT ["java", "-jar", "app.jar"]