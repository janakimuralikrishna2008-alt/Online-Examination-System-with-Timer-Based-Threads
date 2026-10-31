@echo off
title Online Examination System - Web Platform Launcher
setlocal enabledelayedexpansion

echo ========================================================
echo Online Examination System with Timer-Based Threads
echo Launching Full Web Platform (Frontend + Backend)
echo ========================================================
echo.

:: Set default DB_PASSWORD if not already set in environment
if "%DB_PASSWORD%"=="" (
    set DB_PASSWORD=2005
)

:: 1. Start Backend in separate window
echo [STEP 1/2] Starting Spring Boot REST Backend on port 8080...
start "Exam System Web Backend [Port 8080]" cmd /c "%~dp0run_web_backend.bat"

:: 2. Wait up to 10 seconds for backend to become healthy
echo Waiting for Spring Boot backend to become ready...
powershell -NoProfile -Command "for ($i=0; $i -lt 15; $i++) { try { $res = Invoke-RestMethod -Uri 'http://127.0.0.1:8080/api/health' -TimeoutSec 2; if ($res.status -eq 'UP') { exit 0 } } catch {}; Start-Sleep -Seconds 1 }; exit 1"
if %ERRORLEVEL% equ 0 (
    echo [OK] Backend is UP and operational on http://127.0.0.1:8080/api/health
) else (
    echo [NOTE] Backend is still initializing in its window. Continuing to launch frontend...
)
echo.

:: 3. Start Frontend in separate window
echo [STEP 2/2] Starting React Vite Frontend on port 5173...
start "Exam System Web Frontend [Port 5173]" cmd /c "%~dp0run_web_frontend.bat"

echo.
echo ========================================================
echo Online Examination System Web Platform launched!
echo ========================================================
echo - Web Application UI: http://localhost:5173
echo - REST Backend Base:  http://127.0.0.1:8080/api
echo - Backend Health URL: http://127.0.0.1:8080/api/health
echo.
echo Verified Test Credentials:
echo - Administrator: admin    / admin123  (Admin Console)
echo - Student:       student1 / student123 (Student Portal)
echo.
echo How to Stop Services:
echo - To stop either service, close its respective console window
echo   or press Ctrl+C in that window.
echo ========================================================
echo.
pause
