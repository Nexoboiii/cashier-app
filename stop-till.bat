@echo off
echo Stopping the till - this runs the shutdown backup.

curl -s -X POST http://localhost:8080/api/system/shutdown
if errorlevel 1 (
  echo Could not reach the till. It may already be stopped.
  pause
  exit /b 1
)

timeout /t 6 /nobreak >nul
echo Done. Check the backups folder for a -shutdown.zip
pause