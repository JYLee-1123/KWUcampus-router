# 1. 빌드 단계 (Maven)
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# 2. 실행 단계 (수정됨: openjdk -> eclipse-temurin)
# 'openjdk' 이미지가 사라져서, 더 안정적인 'eclipse-temurin'으로 교체했습니다.
FROM eclipse-temurin:17-jre
WORKDIR /app
# 빌드 단계에서 만든 .jar 파일을 복사해옴
COPY --from=build /app/target/*.jar app.jar

# 8080 포트 열기
EXPOSE 8080

# 실행 명령어
ENTRYPOINT ["java", "-jar", "app.jar"]