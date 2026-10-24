@echo off
setlocal
cd /d "%~dp0"

:: ========================================================
:: Hospital Emergency Priority System (HEPS) - Native EXE Launcher
:: ========================================================

:: 1. Ensure MySQL Server is running
tasklist /FI "IMAGENAME eq mysqld.exe" 2>NUL | find /I /N "mysqld.exe">NUL
if "%ERRORLEVEL%"=="0" (
    echo [OK] MySQL Server is running.
) else (
    echo [INFO] Starting MySQL Server...
    start "" /B "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysqld.exe" --defaults-file="C:\ProgramData\MySQL\MySQL Server 8.4\my.ini"
    timeout /t 3 /nobreak > NUL
    echo [OK] MySQL Server started.
)

:: 2. Launch the Standalone HEPS.exe Application
if not exist "%~dp0release\HEPS\HEPS.exe" (
    echo [INFO] release\HEPS\HEPS.exe not found. Building executable first...
    call "%~dp0build-exe.bat"
)

echo [INFO] Launching HEPS Desktop Application...
start "" "%~dp0release\HEPS\HEPS.exe"
