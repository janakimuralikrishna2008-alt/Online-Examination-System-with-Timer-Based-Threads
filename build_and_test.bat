@echo off
setlocal enabledelayedexpansion

echo ===================================================================
echo   Compiling Main Application Sources...
echo ===================================================================

if not exist bin mkdir bin
if not exist bin-test mkdir bin-test

powershell -NoProfile -Command "(Get-ChildItem -Path src\main\java -Filter *.java -Recurse | Resolve-Path -Relative) | Out-File -FilePath sources.txt -Encoding ascii"
javac -d bin @sources.txt
set MAIN_EXIT=%ERRORLEVEL%
if exist sources.txt del sources.txt

if %MAIN_EXIT% neq 0 (
    echo [ERROR] Main source compilation failed with exit code %MAIN_EXIT%!
    exit /b %MAIN_EXIT%
)

echo Main sources compiled successfully into bin/
copy /Y src\main\resources\* bin\ >nul 2>&1

echo.
echo ===================================================================
echo   Compiling Test Sources...
echo ===================================================================

set M2=C:\Users\nande\.m2\repository
set CP=bin;%M2%\org\junit\jupiter\junit-jupiter-api\5.10.2\junit-jupiter-api-5.10.2.jar;%M2%\org\junit\jupiter\junit-jupiter-params\5.10.2\junit-jupiter-params-5.10.2.jar;%M2%\org\junit\platform\junit-platform-commons\1.10.2\junit-platform-commons-1.10.2.jar;%M2%\org\opentest4j\opentest4j\1.3.0\opentest4j-1.3.0.jar;%M2%\org\apiguardian\apiguardian-api\1.1.2\apiguardian-api-1.1.2.jar;%M2%\com\mysql\mysql-connector-j\8.3.0\mysql-connector-j-8.3.0.jar

powershell -NoProfile -Command "(Get-ChildItem -Path src\test\java -Filter *.java -Recurse | Resolve-Path -Relative) | Out-File -FilePath testsources.txt -Encoding ascii"
javac -cp "%CP%" -d bin-test @testsources.txt
set TEST_EXIT=%ERRORLEVEL%
if exist testsources.txt del testsources.txt

if %TEST_EXIT% neq 0 (
    echo [ERROR] Test source compilation failed with exit code %TEST_EXIT%!
    exit /b %TEST_EXIT%
)

echo Test sources compiled successfully into bin-test/

echo.
echo ===================================================================
echo   Running JUnit 5 Test Suite via TestRunner...
echo ===================================================================

set RUN_CP=bin;bin-test;%M2%\org\junit\jupiter\junit-jupiter-api\5.10.2\junit-jupiter-api-5.10.2.jar;%M2%\org\junit\jupiter\junit-jupiter-params\5.10.2\junit-jupiter-params-5.10.2.jar;%M2%\org\junit\platform\junit-platform-commons\1.10.2\junit-platform-commons-1.10.2.jar;%M2%\org\opentest4j\opentest4j\1.3.0\opentest4j-1.3.0.jar;%M2%\org\apiguardian\apiguardian-api\1.1.2\apiguardian-api-1.1.2.jar;%M2%\com\mysql\mysql-connector-j\8.3.0\mysql-connector-j-8.3.0.jar

java -cp "%RUN_CP%" com.onlineexam.TestRunner
set RUNNER_EXIT=%ERRORLEVEL%

exit /b %RUNNER_EXIT%
