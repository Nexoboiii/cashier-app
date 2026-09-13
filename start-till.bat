@echo off
setlocal
cd /d "%~dp0"

set "JAR=cashier-app-0.0.1-SNAPSHOT.jar"
if not exist "%JAR%" set "JAR=target\cashier-app-0.0.1-SNAPSHOT.jar"
if not exist "%JAR%" (
  echo Could not find the jar next to this file or in target\.
  pause
  exit /b 1
)

echo Starting the till...
start "" javaw -jar "%JAR%"

rem give spring time to bind the port before chrome asks for it
timeout /t 8 /nobreak >nul

set "CHROME=%ProgramFiles%\Google\Chrome\Application\chrome.exe"
if not exist "%CHROME%" set "CHROME=%ProgramFiles(x86)%\Google\Chrome\Application\chrome.exe"

if exist "%CHROME%" (
  start "" "%CHROME%" --app=http://localhost:8080
) else (
  start "" http://localhost:8080
)