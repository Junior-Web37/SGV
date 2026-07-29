@echo off
setlocal
set "SCRIPT_DIR=%~dp0"
if not defined JAVA_HOME (
  if exist "C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot" (
    set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot"
  )
)
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" (
  set "PATH=%JAVA_HOME%\bin;%PATH%"
  set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
) else (
  set "JAVA_EXE=java"
)
set "JAR=%SCRIPT_DIR%target\java-sgv-0.1.0.jar"
if not exist "%JAR%" (
  echo Arquivo nao encontrado: %JAR%
  echo Execute primeiro: mvn -f "%SCRIPT_DIR%pom.xml" -DskipTests package
  pause
  exit /b 1
)
cd /d "%SCRIPT_DIR%"
if not exist "data" mkdir "data"
"%JAVA_EXE%" -jar "%JAR%"
if errorlevel 1 pause
endlocal
