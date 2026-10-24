@echo off
echo ========================================================
echo Hospital Emergency Priority System (HEPS) - Launcher
echo ========================================================

:: 1. Check and start MySQL if not running
tasklist /FI "IMAGENAME eq mysqld.exe" 2>NUL | find /I /N "mysqld.exe">NUL
if "%ERRORLEVEL%"=="0" (
    echo [OK] MySQL Server is running.
) else (
    echo [INFO] Starting MySQL Server...
    start "" /B "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysqld.exe" --defaults-file="C:\ProgramData\MySQL\MySQL Server 8.4\my.ini"
    timeout /t 3 /nobreak > NUL
    echo [OK] MySQL Server started.
)

:: 2. Launch the JavaFX Application
echo [INFO] Launching Hospital Emergency Priority System UI...
call mvn javafx:run
