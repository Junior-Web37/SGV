# =============================================
# SGV — Multi-stage Dockerfile
# Build context: ./java-sgv  (from docker-compose)
# Copies pom.xml + src from context root
# =============================================

# ---- Stage 1: Build ----
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy dependency manifests first (layer caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn package -DskipTests -B

# ---- Stage 2: Runtime ----
FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache mariadb-client curl && rm -rf /var/cache/apk/*

WORKDIR /app

# Security: run as non-root
RUN addgroup -S sgvgroup && adduser -S sgvuser -G sgvgroup
USER sgvuser

COPY --from=builder /app/target/*.jar app.jar

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

# Profile set via SPRING_PROFILES_ACTIVE env var from docker-compose
ENTRYPOINT ["java", "-jar", "app.jar"]
