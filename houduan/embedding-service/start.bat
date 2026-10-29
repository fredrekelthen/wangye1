@echo off
setlocal

rem ============================================================
rem  本地中文 Embedding 服务启动脚本
rem  默认监听 8000，供 javahouduan 后端做 RAG 向量化
rem  首次运行会自动下载模型（约 100MB，走 hf-mirror 镜像）
rem ============================================================

cd /d "%~dp0"

where python >nul 2>nul
if errorlevel 1 (
    echo [ERROR] 未找到 python，请先安装 Python 3.9+ 并加入 PATH。
    pause
    exit /b 1
)

if not exist ".venv" (
    echo [INFO] 首次运行，创建虚拟环境并安装依赖，请稍候...
    python -m venv .venv
    if errorlevel 1 (
        echo [ERROR] 创建虚拟环境失败。
        pause
        exit /b 1
    )
    call ".venv\Scripts\activate.bat"
    python -m pip install --upgrade pip
    python -m pip install -r requirements.txt
) else (
    call ".venv\Scripts\activate.bat"
)

echo.
echo ================================================
echo   本地 Embedding 服务
echo   http://localhost:8000
echo   模型: BAAI/bge-small-zh-v1.5 (512 维)
echo   按 Ctrl+C 或关闭窗口停止服务
echo ================================================
echo.

python embedding_server.py --port 8000
pause
endlocal
