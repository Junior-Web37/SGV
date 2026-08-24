@echo off
setlocal EnableDelayedExpansion
title Instalador e Assistente de Configuracao - SGV Desktop 1.0.0

:: Definir cores e codificacao UTF-8
chcp 65001 >nul 2>&1
color 0F

set "CURRENT_DIR=%~dp0"
if "%CURRENT_DIR:~-1%"=="\" set "CURRENT_DIR=%CURRENT_DIR:~0,-1%"

cls
echo ==============================================================================
echo       SGV DESKTOP 2026 — SISTEMA DE GESTÃO DE VENDAS & FACTURAÇÃO (MZ)
echo                     ASSISTENTE DE INSTALAÇÃO RÁPIDA
echo ==============================================================================
echo.
echo  Bem-vindo ao instalador oficial do SGV Desktop para Pequenas e Médias Empresas!
echo  Este assistente irá configurar o SGV, atalhos e ambiente no seu computador.
echo.
echo ------------------------------------------------------------------------------
echo  [1/4] Verificação de Pré-requisitos...
echo ------------------------------------------------------------------------------

:: 1. Verificar Java 21 ou 25
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
            goto :java_detectado
        )
    )
)

:java_detectado
if not defined JAVA_EXE (
    where java >nul 2>&1
    if !ERRORLEVEL! equ 0 set "JAVA_EXE=java"
)

if defined JAVA_EXE (
    echo  [OK] Java Runtime detectado com sucesso: %JAVA_EXE%
) else (
    echo  [AVISO] Java 21 ou 25 não foi detectado automaticamente.
    echo         Recomendamos instalar o Eclipse Adoptium Temurin JDK 21: https://adoptium.net/
)

echo.
echo ------------------------------------------------------------------------------
echo  [2/4] Verificação da Base de Dados (MariaDB / MySQL XAMPP)...
echo ------------------------------------------------------------------------------

:: Testar porta 3306 (MySQL padrão)
netstat -ano | findstr /R ":3306[ ]" >nul 2>&1
if !ERRORLEVEL! equ 0 (
    echo  [OK] Serviço MySQL / MariaDB activo na porta 3306!
) else (
    echo  [AVISO] O serviço MySQL/MariaDB na porta 3306 não está a responder no momento.
    echo         Antes de abrir o SGV, abra o XAMPP Control Panel e clique em "Start" no MySQL.
)

echo.
echo ------------------------------------------------------------------------------
echo  [3/4] Criação de Pastas e Atalhos no Ambiente de Trabalho...
echo ------------------------------------------------------------------------------

:: Criar diretórios de trabalho
if not exist "%CURRENT_DIR%\data" mkdir "%CURRENT_DIR%\data" >nul 2>&1
if not exist "%CURRENT_DIR%\backups" mkdir "%CURRENT_DIR%\backups" >nul 2>&1
if not exist "%USERPROFILE%\Documents\SGV\recibos" mkdir "%USERPROFILE%\Documents\SGV\recibos" >nul 2>&1
if not exist "%USERPROFILE%\Documents\SGV\caixa" mkdir "%USERPROFILE%\Documents\SGV\caixa" >nul 2>&1
if not exist "%USERPROFILE%\Documents\SGV\backups" mkdir "%USERPROFILE%\Documents\SGV\backups" >nul 2>&1

:: Criar Atalho no Desktop via VBScript
set "VBS_SHORTCUT=%TEMP%\sgv_desktop_shortcut.vbs"
set "ICON_FILE=%CURRENT_DIR%\SGV.ico"
if not exist "%ICON_FILE%" set "ICON_FILE=%CURRENT_DIR%\icons\SGV.ico"
if not exist "%ICON_FILE%" set "ICON_FILE=%CURRENT_DIR%\java-sgv\src\main\resources\icons\app-icon.ico"

(
echo Set ws = CreateObject("WScript.Shell"^)
echo Set lk = ws.CreateShortcut(ws.SpecialFolders("Desktop"^) ^& "\SGV - Sistema de Vendas.lnk"^)
echo lk.TargetPath = "wscript.exe"
echo lk.Arguments = Chr(34^) ^& "%CURRENT_DIR%\SGV.vbs" ^& Chr(34^)
echo lk.WorkingDirectory = "%CURRENT_DIR%"
if exist "%ICON_FILE%" (
    echo lk.IconLocation = "%ICON_FILE%,0"
)
echo lk.Description = "SGV Desktop - Sistema de Gestão Comercial e Facturação Moçambique"
echo lk.Save
) > "%VBS_SHORTCUT%"

cscript //nologo "%VBS_SHORTCUT%" >nul 2>&1
del "%VBS_SHORTCUT%" 2>nul

:: Criar Atalho no Menu Iniciar
set "START_MENU_DIR=%APPDATA%\Microsoft\Windows\Start Menu\Programs\SGV"
if not exist "%START_MENU_DIR%" mkdir "%START_MENU_DIR%" >nul 2>&1

set "VBS_STARTMENU=%TEMP%\sgv_startmenu_shortcut.vbs"
(
echo Set ws = CreateObject("WScript.Shell"^)
echo Set lk = ws.CreateShortcut("%START_MENU_DIR%\SGV Desktop.lnk"^)
echo lk.TargetPath = "wscript.exe"
echo lk.Arguments = Chr(34^) ^& "%CURRENT_DIR%\SGV.vbs" ^& Chr(34^)
echo lk.WorkingDirectory = "%CURRENT_DIR%"
if exist "%ICON_FILE%" (
    echo lk.IconLocation = "%ICON_FILE%,0"
)
echo lk.Description = "SGV Desktop - Sistema de Gestão Comercial e Facturação"
echo lk.Save
) > "%VBS_STARTMENU%"

cscript //nologo "%VBS_STARTMENU%" >nul 2>&1
del "%VBS_STARTMENU%" 2>nul

echo  [OK] Atalho criado no Ambiente de Trabalho (Desktop)!
echo  [OK] Atalho adicionado ao Menu Iniciar!

echo.
echo ------------------------------------------------------------------------------
echo  [4/4] Instalação Concluída com Sucesso!
echo ------------------------------------------------------------------------------
echo.
echo  ============================================================================
echo   O SGV Desktop está pronto a utilizar no seu computador!
echo   Pode abrir o sistema através do atalho no Ambiente de Trabalho.
echo.
echo   Utilizadores padrão para login:
echo   - Administrador:  admin         / Senha: admin
echo   - Gerente:        gerente.maputo / Senha: admin
echo   - Operador Caixa: caixa1.maputo  / Senha: admin
echo  ============================================================================
echo.

set /p INICIAR="Deseja iniciar o SGV Desktop agora? (S/N): "
if /i "%INICIAR%"=="S" (
    echo.
    echo A iniciar SGV Desktop...
    start "" wscript.exe "%CURRENT_DIR%\SGV.vbs"
)

echo.
echo Obrigado por escolher o SGV Desktop!
timeout /t 3 >nul
endlocal
exit /b 0
