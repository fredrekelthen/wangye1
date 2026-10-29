@echo off
setlocal

rem ============================================================
rem  Smart Course Platform - One-click Stopper
rem  Kills whatever is listening on port 8080 (backend) and
rem  3000 (frontend). Double-click to run.
rem ============================================================

echo ================================================
echo   Smart Course Platform - Stopping
echo ================================================
echo.

call :killPort 8080 Backend
call :killPort 3000 Frontend

echo.
echo Done.
pause
endlocal
exit /b 0

rem ---------- Kill the process listening on a port ----------
:killPort
set "PORT=%~1"
set "NAME=%~2"
set "FOUND="
for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":%PORT% " ^| findstr LISTENING') do (
    set "FOUND=1"
    echo [%NAME%] port %PORT% used by PID %%p, killing...
    taskkill /F /PID %%p >nul 2>nul
    if errorlevel 1 (
        echo    [FAIL] could not kill PID %%p (already gone or needs admin).
    ) else (
        echo    [OK] killed PID %%p.
    )
)
if not defined FOUND echo [%NAME%] port %PORT% not in use, nothing to stop.
exit /b 0
