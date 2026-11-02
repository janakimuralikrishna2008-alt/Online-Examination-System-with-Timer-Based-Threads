#!/bin/bash
set -e

# ===================================================================
# Online Examination System with Timer-Based Threads
# Production All-in-One Container Entrypoint (MySQL 8.4 + Spring Boot 3)
# ===================================================================

echo "=================================================================="
echo " Starting Online Examination System - Production All-in-One Service"
echo "=================================================================="

# Ensure MySQL runtime directory exists
mkdir -p /var/run/mysqld /var/log/mysql /var/lib/mysql
chown -R mysql:mysql /var/run/mysqld /var/log/mysql /var/lib/mysql

# 1. Initialize MySQL Data Directory if not yet initialized
if [ ! -d "/var/lib/mysql/mysql" ]; then
    echo "[1/4] Initializing MySQL data directory..."
    mysqld --defaults-file=/etc/mysql/conf.d/low-memory.cnf --initialize-insecure --user=mysql
fi

# 2. Start MySQL Daemon in background
echo "[2/4] Starting MySQL daemon (Low-Memory profile)..."
mysqld --defaults-file=/etc/mysql/conf.d/low-memory.cnf --user=mysql &
MYSQL_PID=$!

# Wait for MySQL to become fully ready
echo "      Waiting for MySQL to accept connections on 127.0.0.1:3306..."
until mysqladmin ping --silent >/dev/null 2>&1; do
    sleep 1
done
echo "      MySQL is up and accepting connections."

# 3. Setup Database, Dynamic Local Credentials, and Schema
echo "[3/4] Initializing database schema and security..."
DB_NAME="${DB_NAME:-online_exam_db}"
APP_DB_USER="examapp"

# Generate cryptographically random local password for internal container communication
# if DB_PASSWORD is not explicitly provided in environment
if [ -z "$DB_PASSWORD" ]; then
    APP_DB_PASS=$(head -c 24 /dev/urandom | base64 | tr -dc 'a-zA-Z0-9' | head -c 20)
else
    APP_DB_PASS="$DB_PASSWORD"
fi

# Configure database & internal application user (never exposed externally)
mysql -u root <<EOSQL
CREATE DATABASE IF NOT EXISTS \`${DB_NAME}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS '${APP_DB_USER}'@'127.0.0.1' IDENTIFIED BY '${APP_DB_PASS}';
CREATE USER IF NOT EXISTS '${APP_DB_USER}'@'localhost' IDENTIFIED BY '${APP_DB_PASS}';
ALTER USER '${APP_DB_USER}'@'127.0.0.1' IDENTIFIED BY '${APP_DB_PASS}';
ALTER USER '${APP_DB_USER}'@'localhost' IDENTIFIED BY '${APP_DB_PASS}';
GRANT ALL PRIVILEGES ON \`${DB_NAME}\`.* TO '${APP_DB_USER}'@'127.0.0.1';
GRANT ALL PRIVILEGES ON \`${DB_NAME}\`.* TO '${APP_DB_USER}'@'localhost';
FLUSH PRIVILEGES;
EOSQL

# Execute production schema and educational materials (no hardcoded credentials)
if [ -f "/app/database/production_schema.sql" ]; then
    mysql -u root "${DB_NAME}" < /app/database/production_schema.sql
elif [ -f "/app/database/init_tidb_cloud.sql" ]; then
    mysql -u root "${DB_NAME}" < /app/database/init_tidb_cloud.sql
fi

# Securely configure default admin account if ADMIN_INITIAL_PASSWORD or ADMIN_INITIAL_HASH is provided
if [ -n "$ADMIN_INITIAL_HASH" ]; then
    mysql -u root "${DB_NAME}" <<EOSQL
INSERT INTO users (id, username, password_hash, full_name, email, role)
VALUES (1, 'admin', '${ADMIN_INITIAL_HASH}', 'System Administrator', 'admin@onlineexam.com', 'ADMIN')
ON DUPLICATE KEY UPDATE password_hash = VALUES(password_hash);
EOSQL
fi

echo "      Database initialization verified successfully."

# 4. Launch Spring Boot Application on Render dynamic $PORT
PORT="${PORT:-8080}"
echo "[4/4] Starting Spring Boot web application on port ${PORT}..."

# Export Spring Boot DataSource environment variables for localhost MySQL
export DB_URL="jdbc:mysql://127.0.0.1:3306/${DB_NAME}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
export DB_USER="${APP_DB_USER}"
export DB_PASSWORD="${APP_DB_PASS}"

# JVM memory configuration specifically tuned for 512 MB Render Free tier:
# - Max heap: 220 MB
# - Metaspace limit: 96 MB
# - Thread stack: 512 KB
# - Serial GC for minimum RAM overhead
# - TieredStopAtLevel=1 for fast startup & lower code cache footprint
JVM_FLAGS="-Xmx220m -Xms128m -XX:MaxMetaspaceSize=96m -Xss512k -XX:+UseSerialGC -XX:TieredStopAtLevel=1"

# Handle graceful shutdown of both Spring Boot and MySQL
cleanup() {
    echo "Stopping container processes gracefully..."
    if [ -n "$SPRING_PID" ]; then
        kill -TERM "$SPRING_PID" 2>/dev/null || true
    fi
    if [ -n "$MYSQL_PID" ]; then
        kill -TERM "$MYSQL_PID" 2>/dev/null || true
    fi
    wait 2>/dev/null || true
    echo "Container shutdown complete."
    exit 0
}

trap cleanup SIGINT SIGTERM

java $JVM_FLAGS -Dserver.port="$PORT" -jar /app/app.jar &
SPRING_PID=$!

wait $SPRING_PID
