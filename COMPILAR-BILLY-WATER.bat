@echo off
setlocal EnableDelayedExpansion
title BILLY WATER - Compilador / Instalador Windows

set "ROOT_DIR=%~dp0"
if "%ROOT_DIR:~-1%"=="\" set "ROOT_DIR=%ROOT_DIR:~0,-1%"
set "APP=%ROOT_DIR%\billywater"

chcp 65001 >nul 2>&1
color 0F

echo ========================================================
echo   BILLY WATER - Compilacao e Instalador Windows (MSI/ZIP)
echo ========================================================
echo  Requisitos: JDK 17+ e Maven.
echo.

where mvn >nul 2>&1
if not !ERRORLEVEL! equ 0 (
    echo [ERRO] Maven nao encontrado no PATH.
    echo        Instale o Apache Maven : https://maven.apache.org/
    echo        e o JDK 17+            : https://adoptium.net/
    pause
    exit /b 1
)

cd /d "%APP%"
if not exist "%APP%\pom.xml" (
    echo [ERRO] Modulo billywater nao encontrado em: %APP%
    pause
    exit /b 1
)

echo [1/2] A compilar fat-jar...
echo.
call mvn -B -q clean package
if not !ERRORLEVEL! equ 0 (
    echo.
    echo [ERRO] A compilacao falhou. Reveja as mensagens acima.
    pause
    exit /b 1
)
echo.
echo [OK] Fat-jar gerado em: %APP%\target\billywater-5.0.0.jar

echo.
echo [2/2] A gerar instalador/portatil Windows (MSI + ZIP)...
echo         (pode demorar; requer JavaFX/Launcher no Windows)
echo.
call mvn -B -Pdist clean package -DskipTests
if not !ERRORLEVEL! equ 0 (
    echo.
    echo [AVISO] O passo -Pdist falhou (comum fora de Windows ou sem JRE).
    echo         O fat-jar ja esta pronto em: target\billywater-5.0.0.jar
    echo         Pode iniciar o sistema clicando em BILLY-WATER.bat
    pause
    exit /b 0
)

echo.
echo [OK] Instalador/portatil gerado em: %APP%\target\
dir /b "%APP%\target\*.msi" "%APP%\target\*.zip" 2>nul
echo.
echo Para instalar: execute o ficheiro .msi (Windows) ou use o .zip portatil.
echo.
pause
endlocal
