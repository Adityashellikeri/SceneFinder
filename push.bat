@echo off
:: SceneFinder Auto-Push to GitHub
:: Usage: push.bat "optional commit message"
:: If no message provided, uses timestamp

cd /d "%~dp0"

set MSG=%~1
if "%MSG%"=="" set MSG=Auto-update: %DATE% %TIME%

echo [Push] Staging all changes...
git add -A

echo [Push] Committing: %MSG%
git commit -m "%MSG%" 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [Push] Nothing new to commit.
    goto :done
)

echo [Push] Pushing to GitHub...
git push origin main

if %ERRORLEVEL% EQU 0 (
    echo [Push] SUCCESS - Changes pushed to GitHub!
) else (
    echo [Push] ERROR - Push failed. Check your git credentials.
)

:done
pause
