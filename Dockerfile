# ===================================================================
# Online Examination System with Timer-Based Threads
# All-in-One Production Dockerfile for Render Free Tier (₹0 Demo)
# Runs MySQL 8.4 Server + Spring Boot 3 on Adoptium Java 21 LTS
# Specially optimized for 512 MB RAM limit
# ===================================================================

# -------------------------------------------------------------------
# Stage 1: Build Core Module and Web Backend JAR
# -------------------------------------------------------------------
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /workspace

# Copy Maven wrapper & root pom
COPY mvnw mvnw.cmd pom.xml ./
COPY .mvn .mvn
RUN chmod +x ./mvnw

# Copy core engine source & compile/install core module to local repo
COPY src src
COPY database database
RUN ./mvnw -B clean install -DskipTests

# Copy web backend module and package executable Spring Boot JAR
COPY web/backend web/backend
WORKDIR /workspace/web/backend
RUN ../../mvnw -B clean package -DskipTests

# -------------------------------------------------------------------
# Stage 2: Combined Minimal Runtime (Adoptium JRE 21 + MySQL Server)
# -------------------------------------------------------------------
FROM eclipse-temurin:21-jre-jammy

# Install MySQL Server 8 and utilities non-interactively
ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get update && \
    apt-get install -y --no-install-recommends mysql-server mysql-client curl && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy application JAR and database scripts
COPY --from=builder /workspace/web/backend/target/online-examination-web-backend-*.jar /app/app.jar
COPY database /app/database

# Copy custom low-memory MySQL configuration
COPY database/low-memory-my.cnf /etc/mysql/conf.d/low-memory.cnf

# Copy and set permissions for entrypoint script
COPY docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod +x /app/docker-entrypoint.sh && \
    sed -i 's/\r$//' /app/docker-entrypoint.sh

# Render sets $PORT dynamically (defaults to 8080)
EXPOSE 8080

ENV PORT=8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
  CMD curl -f http://127.0.0.1:${PORT}/api/health || exit 1

ENTRYPOINT ["/app/docker-entrypoint.sh"]
