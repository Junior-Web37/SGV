@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
set "JAVA_SGV_DIR=%SCRIPT_DIR%java-sgv"

if not exist "%JAVA_SGV_DIR%\pom.xml" (
    echo Maven project not found at %JAVA_SGV_DIR%
    exit /b 1
)

if not defined MAVEN_OPTS (
    set "MAVEN_OPTS=-Xmx512m -Xms256m -XX:+UseSerialGC"
)

cd /d "%JAVA_SGV_DIR%"
mvn clean compile javafx:run %*
