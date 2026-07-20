@echo off
REM Heavenys Client — Windows build script.
REM Builds the in-game mod jar, the launcher fat jar, and a Windows .exe installer.
REM Requires: JDK 21 on PATH, and (for the installer) the WiX Toolset 3.x.

setlocal
cd /d "%~dp0\.."

echo [Heavenys] Building client mod + launcher (with tests)...
call gradlew.bat :client:build :launcher:test :launcher:fatJar || goto :error

echo [Heavenys] Packaging launcher as a Windows .exe installer...
call gradlew.bat :launcher:jpackage -PinstallerType=exe || goto :appimage

echo [Heavenys] Done. Installer is in launcher\build\jpackage\
goto :eof

:appimage
echo [Heavenys] Installer build failed (WiX missing?). Producing a portable app-image instead...
call gradlew.bat :launcher:jpackage || goto :error
echo [Heavenys] Done. Portable app-image is in launcher\build\jpackage\
goto :eof

:error
echo [Heavenys] Build failed.
exit /b 1
