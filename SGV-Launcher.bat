@echo off
setlocal EnableDelayedExpansion
title SGV - Sistema de Gestao de Vendas e Facturacao

set "ROOT_DIR=%~dp0"
if "%ROOT_DIR:~-1%"=="\" set "ROOT_DIR=%ROOT_DIR:~0,-1%"

echo ========================================================
echo   SGV Desktop - Sistema de Gestao de Vendas (Mocambique)
echo ========================================================
echo.

:: 1. Procurar Java (JDK 21 ou 25)
set "JAVA_EXE="
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
)

if not defined JAVA_EXE (
    for %%d in (
        "C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot"
        "C:\Program Files\Eclipse Adoptium\jdk-21.0.2.13-hotspot"
        "C:\Program Files\Eclipse Adoptium\jdk-21"
        "C:\Program Files\Eclipse Adoptium\jdk-25"
        "C:\Program Files\Java\jdk-21"
        "C:\Program Files\Java\jdk-25"
        "C:\Program Files\BellSoft\LibericaJDK-21"
        "C:\Program Files\Amazon Corretto\jdk21"
    ) do (
        if exist "%%~d\bin\java.exe" (
            set "JAVA_HOME=%%~d"
            set "JAVA_EXE=%%~d\bin\java.exe"
            goto :java_found
        )
    )
)

:java_found
if not defined JAVA_EXE (
    where java >nul 2>&1
    if !ERRORLEVEL! equ 0 (
        set "JAVA_EXE=java"
    )
)

if not defined JAVA_EXE (
    echo [ERRO] Java 21 ou Java 25 nao encontrado no seu computador.
    echo Por favor, instale o Eclipse Adoptium Temurin JDK 21: https://adoptium.net/
    echo.
    pause
    exit /b 1
)

:: 2. Localizar pasta java-sgv e o JAR
if exist "%ROOT_DIR%\java-sgv\pom.xml" (
    set "APP_DIR=%ROOT_DIR%\java-sgv"
) else (
    set "APP_DIR=%ROOT_DIR%"
)

set "JAR_FILE=%APP_DIR%\target\java-sgv-0.1.0.jar"

cd /d "%APP_DIR%"
if not exist "data" mkdir "data"

:: 3. Verificar se o JAR existe; se nao, tentar compilar ou executar com Maven
if not exist "%JAR_FILE%" (
    where mvn >nul 2>&1
    if !ERRORLEVEL! equ 0 (
        echo [INFO] A preparar e iniciar SGV em modo directo...
        call mvn -DskipTests javafx:run
        if !ERRORLEVEL! equ 0 goto :done
    )
)

if exist "%JAR_FILE%" (
    echo [INFO] A iniciar SGV Desktop...
    "%JAVA_EXE%" -jar "%JAR_FILE%" --spring.profiles.active=mysql
) else (
    echo [INFO] A executar com Maven...
    call mvn -DskipTests javafx:run
)

:done
if %ERRORLEVEL% neq 0 (
    echo.
    echo [AVISO] Certifique-se de que o modulo MySQL do XAMPP esta ligado (Start na porta 3306).
    echo.
    pause
)
endlocal
