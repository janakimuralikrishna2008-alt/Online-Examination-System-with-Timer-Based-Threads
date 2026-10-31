@echo off
title Online Examination System - Web Backend
setlocal enabledelayedexpansion

echo ========================================================
echo Online Examination System with Timer-Based Threads
echo Starting Spring Boot Web Backend (Port 8080)...
echo ========================================================
echo.

cd /d "%~dp0web\backend"

:: Verify Java installation
where java >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Java 21+ was not found on your system PATH!
    echo Please install Java 21 or ensure JAVA_HOME is configured.
    echo.
    pause
    exit /b 1
)

:: Prefer LTS JDK 21 if JAVA_HOME points to an incompatible early-access version
if exist "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot" (
    set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot"
)

:: Set controlled memory options for Maven execution
if "%MAVEN_OPTS%"=="" (
    set "MAVEN_OPTS=-Xmx384m -XX:+UseSerialGC"
)

:: Database password can be supplied via environment variable; defaults to empty for local MySQL
if not defined DB_PASSWORD (
    set "DB_PASSWORD="
)

:: Check if port 8080 is already listening
powershell -NoProfile -Command "$conn = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue; if ($conn) { exit 1 } else { exit 0 }"
if %ERRORLEVEL% equ 1 (
    echo [CHECK] Port 8080 is already active. Checking if this backend is healthy...
    powershell -NoProfile -Command "try { $res = Invoke-RestMethod -Uri 'http://127.0.0.1:8080/api/health' -TimeoutSec 3; if ($res.status -eq 'UP') { exit 0 } else { exit 2 } } catch { exit 2 }"
    if !ERRORLEVEL! equ 0 (
        echo [OK] Spring Boot Web Backend is ALREADY running and healthy!
        echo   - Health Endpoint: http://127.0.0.1:8080/api/health
        echo   - REST Base URL:   http://127.0.0.1:8080/api
        echo.
        echo No duplicate instance will be launched.
        echo To stop the backend, close the window running it or terminate its process.
        echo.
        pause
        exit /b 0
    ) else (
        echo [WARNING] Port 8080 is occupied by another process that is not responding to /api/health.
        for /f "tokens=5" %%a in ('netstat -ano -p tcp ^| findstr /R ":8080 .*LISTENING"') do (
            echo   Occupying Process ID (PID): %%a
        )
        echo Please stop the conflicting process before starting the Spring Boot backend.
        echo.
        pause
        exit /b 1
    )
)

echo [INFO] Launching Spring Boot Web Backend...
echo [INFO] Working Directory: %CD%
echo [INFO] Database User:     root
echo [INFO] Target Port:       8080
echo.

:: Ensure core module artifact is installed in local repo
pushd "%~dp0"
call mvnw.cmd -o install -DskipTests -q
popd

call ..\..\mvnw.cmd spring-boot:run
set EXIT_CODE=%ERRORLEVEL%

if %EXIT_CODE% neq 0 (
    echo.
    echo [ERROR] Spring Boot Web Backend stopped with exit code %EXIT_CODE%.
    echo Review the error log above for details.
)

echo.
pause
