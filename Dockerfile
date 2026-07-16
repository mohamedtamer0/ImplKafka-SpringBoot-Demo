FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace
COPY pom.xml ./
COPY src ./src
RUN mvn --batch-mode --no-transfer-progress clean package -DskipTests

FROM eclipse-temurin:25-jre

RUN groupadd --system --gid 1001 spring \
    && useradd --system --uid 1001 --gid spring spring
WORKDIR /app
COPY --from=build --chown=spring:spring /workspace/target/*.jar app.jar

USER spring:spring
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
