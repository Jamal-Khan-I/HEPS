@echo off
echo ========================================================
echo Building Standalone Windows Executable (HEPS.exe)
echo ========================================================

echo [1/3] Packaging Java application with Maven...
call mvn clean package -DskipTests
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Maven build failed.
    pause
    exit /b %ERRORLEVEL%
)

echo [2/3] Preparing packaging directory...
if not exist "target\package-input" mkdir "target\package-input"
copy /Y "target\hospital-emergency-priority-system-1.0.0.jar" "target\package-input\" > NUL
if exist "release_staging" rmdir /s /q "release_staging"
jpackage --type app-image ^
  --name HEPS ^
  --input target\package-input ^
  --main-jar hospital-emergency-priority-system-1.0.0.jar ^
  --main-class com.hospital.priority.Launcher ^
  --dest release_staging ^
  --app-version 1.0.0 ^
  --description "Hospital Emergency Priority System" ^
  --vendor "CSE203 Java Project" ^
  --add-modules java.base,java.desktop,java.scripting,java.xml,java.logging,java.sql,java.naming,java.management,jdk.unsupported

if %ERRORLEVEL% equ 0 (
    if not exist "release\HEPS" mkdir "release\HEPS"
    if not exist "release\HEPS\app" mkdir "release\HEPS\app"
    copy /Y "release_staging\HEPS\HEPS.exe" "release\HEPS\HEPS.exe" > NUL
    copy /Y "release_staging\HEPS\app\*" "release\HEPS\app\" > NUL
    if not exist "release\HEPS\runtime" (
        xcopy /E /I /Y "release_staging\HEPS\runtime" "release\HEPS\runtime" > NUL
    )
    if exist "release_staging" rmdir /s /q "release_staging" 2> NUL
    echo ========================================================
    echo [SUCCESS] Standalone HEPS.exe generated successfully!
    echo Location: release\HEPS\HEPS.exe
    echo ========================================================
) else (
    echo [ERROR] jpackage failed.
)
