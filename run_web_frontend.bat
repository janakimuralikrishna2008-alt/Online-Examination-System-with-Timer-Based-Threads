@echo off
title Online Examination System - Web Frontend
setlocal enabledelayedexpansion

echo ========================================================
echo Online Examination System with Timer-Based Threads
echo Starting React Vite Frontend (Port 5173)...
echo ========================================================
echo.

cd /d "%~dp0web\frontend"

:: Verify Node.js installation
where node >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Node.js was not found on your system PATH!
    echo Please install Node.js 18+ to run the React Vite frontend.
    echo.
    pause
    exit /b 1
)

:: Verify npm installation
where npm >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [ERROR] npm was not found on your system PATH!
    echo.
    pause
    exit /b 1
)

:: Check if port 5173 is already listening
powershell -NoProfile -Command "$conn = Get-NetTCPConnection -LocalPort 5173 -State Listen -ErrorAction SilentlyContinue; if ($conn) { exit 1 } else { exit 0 }"
if %ERRORLEVEL% equ 1 (
    echo [OK] React Vite Frontend is ALREADY listening on port 5173!
    echo   - Web Application UI: http://localhost:5173
    echo.
    echo No duplicate instance will be launched.
    echo To stop the frontend, close the window running it or press Ctrl+C in that window.
    echo.
    pause
    exit /b 0
)

:: Check if node_modules exists
if not exist node_modules (
    echo [INFO] node_modules not found. Installing frontend dependencies...
    call npm install
    if !ERRORLEVEL! neq 0 (
        echo [ERROR] npm install failed.
        pause
        exit /b !ERRORLEVEL!
    )
)

echo [INFO] Launching Vite development server...
echo [INFO] Web UI Address: http://localhost:5173
echo.

call npm run dev
set EXIT_CODE=%ERRORLEVEL%

if %EXIT_CODE% neq 0 (
    echo.
    echo [ERROR] Frontend server stopped with exit code %EXIT_CODE%.
)

echo.
pause
