@echo off
setlocal

set "DEVICE=%~1"
set "ADB_TARGET_ARGS="
set "PACKAGE_NAME=com.kuaigeglobal.epnova"

if defined DEVICE set ADB_TARGET_ARGS=-s "%DEVICE%"

where adb >nul 2>&1
if errorlevel 1 (
    echo [ERROR] adb was not found. Please add Android SDK platform-tools to PATH.
    pause
    exit /b 1
)

echo [1/3] Enabling Firebase Analytics debug mode...
adb %ADB_TARGET_ARGS% shell setprop debug.firebase.analytics.app "%PACKAGE_NAME%"
if errorlevel 1 goto :command_failed

echo [2/3] Current Firebase Analytics debug package:
adb %ADB_TARGET_ARGS% shell getprop debug.firebase.analytics.app
if errorlevel 1 goto :command_failed

echo [3/3] Force-stopping %PACKAGE_NAME%...
adb %ADB_TARGET_ARGS% shell am force-stop "%PACKAGE_NAME%"
if errorlevel 1 goto :command_failed

echo Done. Please launch the app again.
pause
exit /b 0

:command_failed
if defined DEVICE (
    echo [ERROR] ADB command failed. Make sure %DEVICE% is connected.
) else (
    echo [ERROR] ADB command failed. Make sure a default ADB device is available.
)
pause
exit /b 1
