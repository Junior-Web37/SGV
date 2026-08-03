@echo off
setlocal
set "SCRIPT_DIR=%~dp0"
if not defined JAVA_HOME (
  if exist "C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot"
  )
)
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" (
  set "PATH=%JAVA_HOME%\bin;%PATH%"
) else (
  echo JDK 25 nao encontrado. Instale o JDK 25 ou defina JAVA_HOME.
  pause
  exit /b 1
)
cd /d "%SCRIPT_DIR%"
call run-sgv.bat
endlocal
