FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

COPY pom.xml .
COPY src/ src/

RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:17-jre

WORKDIR /app

RUN useradd --system --uid 10001 appuser

COPY --from=build /app/target/autoscientist-backend-1.0.0-SNAPSHOT.jar app.jar

RUN mkdir -p /app/uploads/datasets \
    && chown -R appuser:appuser /app/uploads

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
