@echo off
cd /d "%~dp0"
echo Compiling...
javac *.java
if errorlevel 1 (
  echo.
  echo COMPILE FAILED. Install JDK and ensure javac is in PATH.
  pause
  exit /b 1
)
echo.
echo Starting Disaster Response Logistics demo...
echo.
java MainSystem
echo.
pause
