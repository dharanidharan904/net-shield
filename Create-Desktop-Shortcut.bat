@echo off
set SCRIPT_DIR=%~dp0
set SHORTCUT_PATH=%USERPROFILE%\Desktop\NetShield.lnk
set TARGET_PATH=%SCRIPT_DIR%NetShield.bat

powershell -Command "$ws = New-Object -ComObject WScript.Shell; $s = $ws.CreateShortcut('%SHORTCUT_PATH%'); $s.TargetPath = '%TARGET_PATH%'; $s.WorkingDirectory = '%SCRIPT_DIR%'; $s.Description = 'NetShield Network Diagnostics'; $s.Save()"

if exist "%SHORTCUT_PATH%" (
    echo [SUCCESS] NetShield desktop shortcut created at: "%SHORTCUT_PATH%"
) else (
    echo [ERROR] Failed to create desktop shortcut.
)
pause
