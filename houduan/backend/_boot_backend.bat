@echo off
set "JAVA_HOME=D:\jdk21\jdk-17.0.12"
set "PATH=D:\jdk21\jdk-17.0.12\bin;%PATH%"

rem load local secrets (ignored by git)
if exist "%~dp0secrets.bat" call "%~dp0secrets.bat"

rem record credential status at startup (no secret content written)
if defined DEEPSEEK_API_KEY (echo [BOOT] DEEPSEEK_API_KEY = SET > "%~dp0_boot_env.log") else (echo [BOOT] DEEPSEEK_API_KEY = NOT-SET > "%~dp0_boot_env.log")

cd /d D:\javahouduan\javahouduan\houduan\backend
call .\mvnw.cmd -B -DskipTests spring-boot:run > _boot.log 2>&1
