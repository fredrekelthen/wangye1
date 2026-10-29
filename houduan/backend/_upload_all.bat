@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

set "SRC=D:\javahouduan\javahouduan\盛东亮 106637-01 电磁场与电磁波（第4版）(1)"
set "API=http://localhost:8080/api/materials/upload"

echo ============================================
echo   批量上传电磁场教材到 RAG 知识库 (UTF-8)
echo ============================================
echo.

call :up "第一章.docx"  1  "第1章 矢量分析"
call :up "第二章.docx"  2  "第2章 静电场"
call :up "第三章.docx"  3  "第3章 静电场边值问题"
call :up "第四章.docx"  4  "第4章 恒定电流与恒定磁场"
call :up "第五章.docx"  5  "第5章 时变电磁场"
call :up "第六章.docx"  6  "第6章 平面电磁波"
call :up "第七章.docx"  7  "第7章 导行电磁波"
call :up "第八章.docx"  8  "第8章 电磁辐射"
call :up "第九章.docx"  9  "第9章 电磁场数值方法"
call :up "附件.docx"    0  "附录"

echo.
echo ============================================
echo   全部上传完成
echo ============================================
pause
exit /b 0

:up
set "FILE=%SRC%\%~1"
set "CH=%~2"
set "TITLE=%~3"
echo [%CH%] %TITLE%
curl -s -X POST %API% ^
  -F "file=@%FILE%" ^
  -F "title=%TITLE%" ^
  -F "description=电磁场与电磁波（第4版）盛东亮 106637-01" ^
  -F "category=电磁场与电磁波" ^
  -F "chapterNumber=%CH%" ^
  -F "uploadedBy=admin"
echo.
echo.
exit /b 0
