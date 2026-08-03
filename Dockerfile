# AI 恋爱大师 - Serverless 部署用 Dockerfile
# 方式一：运行时打包（推荐，自动化程度高）
FROM maven:3.9-eclipse-temurin-17
WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

EXPOSE 8080

CMD ["java", "-jar", "/app/target/ai-love-master-1.0.0-SNAPSHOT.jar", "--spring.profiles.active=prod"]
