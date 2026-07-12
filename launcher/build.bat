@echo off
:: Heavenys Client Launcher — Windows build script
:: Requirements: Python 3.11+, pip install -r requirements.txt
:: Output: dist\HeavenysLauncher.exe

echo ============================================
echo  Heavenys Client Launcher — Build Script
echo ============================================
echo.

:: Install / verify dependencies
pip install -r requirements.txt --quiet
if %ERRORLEVEL% neq 0 (
    echo [ERROR] pip install failed. Make sure Python 3.11+ is on PATH.
    pause
    exit /b 1
)

:: Clean previous build
if exist build  rmdir /s /q build
if exist dist   rmdir /s /q dist

:: Build
pyinstaller heavenys.spec --noconfirm
if %ERRORLEVEL% neq 0 (
    echo [ERROR] PyInstaller failed.
    pause
    exit /b 1
)

echo.
echo [OK] Build complete: dist\HeavenysLauncher.exe
pause
