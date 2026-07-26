FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests package \
    && cp target/arohan-api-*.jar /workspace/app.jar

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN groupadd --system spring \
    && useradd --system --gid spring spring

COPY --from=build --chown=spring:spring /workspace/app.jar /app/app.jar

USER spring:spring
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

