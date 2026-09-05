@echo off
rem ============================================================
rem  Snap Capsule - Android quick start (double-click entry)
rem  Wraps android-quick-start.ps1, bypassing the PS execution
rem  policy so this can be run by double-click.
rem  Pass-through args example: -SkipBuild -NoRestart
rem  To run unattended from a terminal, call the .ps1 directly
rem  and remove the "pause" below if you do not want it.
rem ============================================================
setlocal
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0android-quick-start.ps1" %*
if errorlevel 1 (
  echo.
  echo [quick-start] Script failed - see messages above.
  pause
)
endlocal
