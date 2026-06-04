# syntax=docker/dockerfile:1.6
# ---------- Build stage ----------
FROM maven:3.9-eclipse-temurin-8 AS build
WORKDIR /workspace

# Copy POM first so the dependency layer can be cached separately from sources.
COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline || true

COPY src ./src
COPY checkstyle.xml ./
RUN mvn -B -ntp -DskipTests clean package

# ---------- Runtime stage ----------
FROM eclipse-temurin:8-jre
WORKDIR /app

# curl is needed for the HEALTHCHECK below.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /workspace/target/backend-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8080
ENV JAVA_OPTS=""

HEALTHCHECK --interval=10s --timeout=3s --retries=20 --start-period=30s \
    CMD curl -fsS http://localhost:8080/api/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
