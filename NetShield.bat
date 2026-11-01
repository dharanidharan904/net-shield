@echo off
title NetShield - Offline-First Internet Failure Detection & Recovery Tool
cd /d "%~dp0"
echo =====================================================================
echo               NETSHIELD 2.0 - WINDOWS NETWORK MONITOR
echo =====================================================================
echo.
echo Launching NetShield Desktop Application...
call .\mvnw.cmd javafx:run
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Application exited with error code %ERRORLEVEL%.
    pause
)
