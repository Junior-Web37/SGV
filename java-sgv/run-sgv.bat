@echo off
setlocal
set "SCRIPT_DIR=%~dp0"
if not defined JAVA_HOME (
  for %%d in (
    "C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot"
    "C:\Program Files\Eclipse Adoptium\jdk-21.0.2.13-hotspot"
    "C:\Program Files\Eclipse Adoptium\jdk-21"
    "C:\Program Files\Java\jdk-21"
    "C:\Program Files\Java\jdk-25"
  ) do (
    if exist "%%~d\bin\java.exe" (
      set "JAVA_HOME=%%~d"
      goto :found_java
    )
  )
)
:found_java
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" (
  set "PATH=%JAVA_HOME%\bin;%PATH%"
  set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
) else (
  set "JAVA_EXE=java"
)
set "JAR=%SCRIPT_DIR%target\java-sgv-0.1.0.jar"
if not exist "%JAR%" (
  echo Ficheiro JAR nao encontrado em: %JAR%
  echo A compilar o projecto com Maven...
  call mvn -f "%SCRIPT_DIR%pom.xml" -DskipTests clean package
)
cd /d "%SCRIPT_DIR%"
if not exist "data" mkdir "data"
"%JAVA_EXE%" -jar "%JAR%"
if errorlevel 1 pause
endlocal
