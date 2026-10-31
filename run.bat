@echo off
setlocal

set M2=C:\Users\nande\.m2\repository
set MYSQL_JAR=%M2%\com\mysql\mysql-connector-j\8.3.0\mysql-connector-j-8.3.0.jar
set FLATLAF_JAR=%M2%\com\formdev\flatlaf\3.4.1\flatlaf-3.4.1.jar

set CP=bin
if exist "%MYSQL_JAR%" set CP=%CP%;%MYSQL_JAR%
if exist "%FLATLAF_JAR%" set CP=%CP%;%FLATLAF_JAR%

echo ===================================================================
echo   Starting Online Examination System with Timer-Based Threads
echo ===================================================================
java -cp "%CP%" com.onlineexam.Main %*
