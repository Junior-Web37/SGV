@echo off
setlocal
set "SCRIPT_DIR=%~dp0"
set "JAVA_HOME=C:\Users\DELL\.jdk\jdk-25.0.2"
if exist "%JAVA_HOME%\bin\java.exe" (
  set "PATH=%JAVA_HOME%\bin;%PATH%"
  set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
) else (
  echo JDK 25 nao encontrada em %JAVA_HOME%
  pause
  exit /b 1
)
cd /d "%SCRIPT_DIR%"
call run-sgv.bat
endlocal
