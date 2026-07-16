@echo off
REM Run PowerShell script with admin privileges
powershell -NoProfile -ExecutionPolicy Bypass -Command "Start-Process powershell -ArgumentList '-NoProfile -ExecutionPolicy Bypass -File \"C:\xampp\htdocs\SGV\scripts\cleanup-and-reinstall.ps1\"' -Verb RunAs -Wait"
