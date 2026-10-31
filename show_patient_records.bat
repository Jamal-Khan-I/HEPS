@echo off
title Hospital Emergency Priority System - MySQL Patient Records
color 0b
chcp 65001 >nul
cls

echo =========================================================================================
echo                HOSPITAL EMERGENCY PRIORITY SYSTEM (HEPS) - MYSQL RECORDS
echo =========================================================================================
echo Database: hospital_emergency_db  ^|  Host: localhost:3306  ^|  User: root
echo.

:menu
echo Choose an option:
echo   [1] View All Patient Records (Full Details)
echo   [2] View Active Queue (Waiting Patients)
echo   [3] View Patients In Treatment
echo   [4] Open Interactive MySQL Prompt
echo   [5] Exit
echo.
set /p choice="Enter choice [1-5]: "

if "%choice%"=="1" goto all_patients
if "%choice%"=="2" goto waiting_patients
if "%choice%"=="3" goto treat_patients
if "%choice%"=="4" goto mysql_shell
if "%choice%"=="5" goto exit_script

cls
echo Invalid choice. Please select 1 to 5.
goto menu

:all_patients
cls
echo =========================================================================================
echo                                   ALL PATIENT RECORDS
echo =========================================================================================
mysql -u root -t -e "USE hospital_emergency_db; SELECT token_no AS 'TOKEN', full_name AS 'NAME', age AS 'AGE', gender AS 'GENDER', emergency_level AS 'PRIORITY', assigned_bay AS 'ASSIGNED BAY', status AS 'STATUS', CONCAT(COALESCE(spo2, '-'), '%%') AS 'SpO2', CONCAT(COALESCE(systolic_bp, '-'), '/', COALESCE(diastolic_bp, '-')) AS 'BP', COALESCE(gcs, 15) AS 'GCS', COALESCE(pain_score, 0) AS 'PAIN' FROM patients ORDER BY arrival_time DESC;"
echo.
pause
cls
goto menu

:waiting_patients
cls
echo =========================================================================================
echo                                 ACTIVE WAITING QUEUE
echo =========================================================================================
mysql -u root -t -e "USE hospital_emergency_db; SELECT token_no AS 'TOKEN', full_name AS 'NAME', emergency_level AS 'PRIORITY', assigned_bay AS 'BAY', news2_score AS 'NEWS2', arrival_time AS 'ARRIVAL' FROM patients WHERE status = 'WAITING' ORDER BY FIELD(emergency_level, 'CRITICAL', 'HIGH', 'MEDIUM', 'LOW'), arrival_time ASC;"
echo.
pause
cls
goto menu

:treat_patients
cls
echo =========================================================================================
echo                                PATIENTS IN TREATMENT
echo =========================================================================================
mysql -u root -t -e "USE hospital_emergency_db; SELECT p.token_no AS 'TOKEN', p.full_name AS 'NAME', p.assigned_bay AS 'BAY', p.assigned_doctor_name AS 'DOCTOR', p.status AS 'STATUS' FROM patients p WHERE p.status = 'IN_TREATMENT';"
echo.
pause
cls
goto menu

:mysql_shell
cls
echo Starting MySQL Interactive Shell for hospital_emergency_db...
echo Type exit to return.
echo.
mysql -u root hospital_emergency_db
cls
goto menu

:exit_script
exit

