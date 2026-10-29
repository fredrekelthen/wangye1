@echo off
setlocal enabledelayedexpansion

rem ============================================================
rem  Smart Course Platform - One-click Launcher
rem  Backend : houduan\backend  (Spring Boot, port 8080, needs JDK 17+)
rem  Frontend: qianduan         (Vue CLI, port 3000)
rem  Double-click to run. Backend and frontend each open a window;
rem  close a window to stop that service.
rem ============================================================

cd /d "%~dp0"

rem Put JAVA_HOME's JDK at the front of PATH so an old Java on the
rem system PATH (e.g. Oracle Java 8) cannot shadow JDK 17.
if defined JAVA_HOME (
    set "PATH=%JAVA_HOME%\bin;%PATH%"
)

echo ================================================
echo   Smart Course Platform - Starting
echo ================================================
echo.

rem ---------- Check Java ----------
where java >nul 2>nul
if errorlevel 1 (
    echo [ERROR] Java not found. Install JDK 17 and set JAVA_HOME.
    pause
    exit /b 1
)

rem Extract Java major version (handles "1.8.0" and "17.0.1" formats)
for /f "tokens=3" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do set "JAVA_VER=%%v"
set "JAVA_VER=!JAVA_VER:"=!"
if "!JAVA_VER:~0,2!"=="1." (
    set "JAVA_MAJOR=!JAVA_VER:~2,1!"
) else (
    for /f "delims=." %%m in ("!JAVA_VER!") do set "JAVA_MAJOR=%%m"
)

if !JAVA_MAJOR! LSS 17 (
    echo [ERROR] Detected JDK !JAVA_MAJOR!, but the project requires JDK 17+.
    echo         Current java path:
    where java
    echo         Set JAVA_HOME to a JDK 17 installation and retry.
    pause
    exit /b 1
)
echo [OK] JDK !JAVA_MAJOR! detected

where npm >nul 2>nul
if errorlevel 1 (
    echo [ERROR] npm not found. Install Node.js first.
    pause
    exit /b 1
)

echo [INFO] Make sure MongoDB is running (default localhost:27017).
echo.
echo [INFO] AI features need these env vars (optional):
echo        set GLM_API_KEY=your_api_key
echo        set ADMIN_INIT_PASSWORD=your_admin_password
echo.

rem ---------- Install frontend deps if missing ----------
if not exist "qianduan\node_modules" (
    echo [INFO] Running npm install, first run may be slow...
    pushd qianduan
    call npm install
    popd
    echo.
)

rem ---------- Load local API keys (DeepSeek / GLM) ----------
rem Secrets live in houduan\backend\secrets.bat, which is gitignored.
set "SECRETS_BAT=%~dp0houduan\backend\secrets.bat"
if not exist "%SECRETS_BAT%" echo [WARN] secrets.bat not found - AI features will be unavailable.
if exist "%SECRETS_BAT%" call "%SECRETS_BAT%"
if exist "%SECRETS_BAT%" echo [INFO] API keys loaded from secrets.bat
echo.

rem ---------- Start backend ----------
echo [1/2] Starting backend Spring Boot (port 8080)...
start "Backend_8080" /d "%~dp0houduan\backend" cmd /k .\mvnw.cmd spring-boot:run

rem ---------- Start frontend ----------
echo [2/2] Starting frontend Vue (port 3000)...
start "Frontend_3000" /d "%~dp0qianduan" cmd /k npm run serve

echo.
echo ================================================
echo   Starting up, please wait...
echo     Backend : http://localhost:8080
echo     Frontend: http://localhost:3000
echo   Close the matching window to stop a service.
echo ================================================
echo.
echo After the frontend finishes compiling, open http://localhost:3000
echo.
pause
endlocal
