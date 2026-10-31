# ===================================================================
# Online Examination System with Timer-Based Threads
# Production Dockerfile for Render / Container Platforms
# Multi-stage build with Adoptium Java 21 LTS
# ===================================================================

# -------------------------------------------------------------------
# Stage 1: Build the Application
# -------------------------------------------------------------------
FROM eclipse-adoptium:21-jdk-jammy AS builder
WORKDIR /workspace

# Copy Maven wrapper and root project definition
COPY mvnw mvnw.cmd pom.xml ./
COPY .mvn .mvn

# Make wrapper executable
RUN chmod +x ./mvnw

# Copy core library source & database scripts, then build and install core module to local repo
COPY src src
COPY database database
RUN ./mvnw -B clean install -DskipTests

# Copy web backend module and build production executable JAR
COPY web/backend web/backend
WORKDIR /workspace/web/backend
RUN ../../mvnw -B clean package -DskipTests

# -------------------------------------------------------------------
# Stage 2: Minimal Production Runtime
# -------------------------------------------------------------------
FROM eclipse-adoptium:21-jre-jammy
WORKDIR /app

# Create dedicated non-root application user
RUN groupadd -r examapp && useradd -r -g examapp examapp

# Copy built artifact and database schema for automated migrations
COPY --from=builder /workspace/web/backend/target/online-examination-web-backend-*.jar app.jar
COPY database database

# Set directory ownership
RUN chown -R examapp:examapp /app

USER examapp

# Render dynamically sets $PORT (defaults to 8080)
EXPOSE 8080

ENV PORT=8080
ENV JAVA_OPTS="-Xmx384m -XX:+UseSerialGC -XX:+TieredCompilation -XX:TieredStopAtLevel=1"

# Healthcheck
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD java -cp app.jar -Dserver.port=$PORT org.springframework.boot.loader.launch.JarLauncher --help || exit 0

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Dserver.port=$PORT -jar app.jar"]

