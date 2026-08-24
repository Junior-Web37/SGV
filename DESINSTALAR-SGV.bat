@echo off
setlocal EnableDelayedExpansion
title Desinstalador - SGV Desktop

chcp 65001 >nul 2>&1
color 0C

cls
echo ==============================================================================
echo                      DESINSTALADOR DO SGV DESKTOP
echo ==============================================================================
echo.
echo  Aviso: Esta acção irá remover os atalhos do SGV Desktop do seu computador.
echo  Os seus dados da base de dados (MySQL/MariaDB) NÃO serão apagados.
echo.

set /p CONFIRMAR="Tem certeza que deseja desinstalar os atalhos do SGV? (S/N): "
if /i not "%CONFIRMAR%"=="S" (
    echo.
    echo Desinstalação cancelada pelo utilizador.
    pause
    exit /b 0
)

echo.
echo A remover atalhos...

:: Remover do Desktop
if exist "%USERPROFILE%\Desktop\SGV - Sistema de Vendas.lnk" (
    del "%USERPROFILE%\Desktop\SGV - Sistema de Vendas.lnk" >nul 2>&1
    echo [OK] Atalho do Ambiente de Trabalho removido.
)

:: Remover do Menu Iniciar
set "START_MENU_DIR=%APPDATA%\Microsoft\Windows\Start Menu\Programs\SGV"
if exist "%START_MENU_DIR%" (
    rmdir /S /Q "%START_MENU_DIR%" >nul 2>&1
    echo [OK] Atalhos do Menu Iniciar removidos.
)

echo.
echo ==============================================================================
echo   Desinstalação de atalhos concluída com sucesso!
echo ==============================================================================
echo.
pause
endlocal
