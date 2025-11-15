@echo off
REM ============================================================
REM FRC SHOOTER TUNER - AUTO-START DAEMON
REM Runs in background, drivers do nothing!
REM ============================================================

REM Run silently in background (no window)
start /B pythonw tuner_daemon.py

REM Optional: Show a quick message
echo FRC Tuner daemon started in background
timeout /t 2 /nobreak >nul

