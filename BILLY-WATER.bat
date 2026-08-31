@echo off
setlocal EnableDelayedExpansion
title BILLY WATER - Sistema de Gestao de Facturacao de Agua

set "ROOT_DIR=%~dp0"
if "%ROOT_DIR:~-1%"=="\" set "ROOT_DIR=%ROOT_DIR:~0,-1%"
set "APP=%ROOT_DIR%\billywater"

chcp 65001 >nul 2>&1
color 0F

echo ========================================================
echo   BILLY WATER - SGF (Sistema de Gestao de Facturacao)
echo ========================================================
echo.

:: ---- 1. Procurar Java (17 / 21 / 25) ----
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
        "C:\Program Files\Java\jdk-17"
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
    where java >nul 2>&1 && set "JAVA_EXE=java"
)
if not defined JAVA_EXE (
    echo [ERRO] Java 17 ou superior nao encontrado.
    echo        Instale o Eclipse Adoptium Temurin JDK 21: https://adoptium.net/
    echo.
    pause
    exit /b 1
)
echo  [OK] Java: %JAVA_EXE%

:: ---- 2. Localizar onde esta o modulo billywater ----
cd /d "%APP%"
if not exist "%APP%\pom.xml" (
    echo [ERRO] Modulo billywater no encontrado em: %APP%
    pause
    exit /b 1
)

:: ---- 3. Preparar pastas de dados ----
if not exist "%APP%\data" mkdir "%APP%\data" >nul 2>&1
if not exist "%APP%\backups" mkdir "%APP%\backups" >nul 2>&1
if not exist "%APP%\config" mkdir "%APP%\config" >nul 2>&1

:: ---- 4. Verificar jar de producao ----
set "JAR=%APP%\target\billywater-5.0.0.jar"
if exist "%JAR%" (
    echo  [OK] A iniciar BILLY WATER (jar de producao)...
    start "" "%JAVA_EXE%" -Dprism.order=sw -jar "%JAR%"
    endlocal
    exit /b 0
)

:: ---- 5. Senao, tentar Maven (modo directo) ----
where mvn >nul 2>&1
if !ERRORLEVEL! equ 0 (
    echo  [INFO] Jar de producao nao encontrado. A compilar/iniciar com Maven...
    echo         (primeira vez demora alguns minutos a descarregar dependencias)
    call mvn -DskipTests javafx:run
    if !ERRORLEVEL! equ 0 (
        endlocal
        exit /b 0
    )
)

echo.
echo [AVISO] Nao foi possivel iniciar automaticamente.
echo        1) Instale o JDK 17+  : https://adoptium.net/
echo        2) Instale o Maven   : https://maven.apache.org/
echo        Depois execute:  COMPILAR-BILLY-WATER.bat
echo.
pause
endlocal
