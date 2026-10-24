@echo off
echo Starting MySQL Server for Hospital Emergency Priority System...
tasklist /FI "IMAGENAME eq mysqld.exe" 2>NUL | find /I /N "mysqld.exe">NUL
if "%ERRORLEVEL%"=="0" (
    echo [OK] MySQL Server is already running.
    exit /b 0
)

start "" /B "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysqld.exe" --defaults-file="C:\ProgramData\MySQL\MySQL Server 8.4\my.ini"
timeout /t 3 /nobreak > NUL
echo [OK] MySQL Server started successfully.
