@echo off
setlocal
title Compilador do Instalador - SGV Desktop

echo ==============================================================================
echo                 COMPILAÇÃO DO INSTALADOR OFICIAL (INNO SETUP)
echo ==============================================================================
echo.

set "INNO_EXE=C:\Program Files (x86)\Inno Setup 6\ISCC.exe"
if not exist "%INNO_EXE%" (
    set "INNO_EXE=C:\Program Files\Inno Setup 6\ISCC.exe"
)

if not exist "%INNO_EXE%" (
    echo [AVISO] Inno Setup 6 (ISCC.exe) nao encontrado nos caminhos padrao.
    echo Pode compilar manualmente abrindo o ficheiro "installer\sgv-setup.iss" no Inno Setup GUI.
    echo.
    echo Para instalacao automatica direta sem compilar o .EXE, utilize "INSTALAR-SGV.bat".
    echo.
    pause
    exit /b 1
)

echo [INFO] A compilar instalador executavel...
"%INNO_EXE%" "%~dp0sgv-setup.iss"

if %ERRORLEVEL% equ 0 (
    echo.
    echo ==============================================================================
    echo [SUCESSO] Instalador gerado com sucesso na pasta "dist\SGV-Setup-1.0.0-Win64.exe"!
    echo ==============================================================================
) else (
    echo [ERRO] Ocorreu uma falha durante a compilacao do instalador.
)

pause
endlocal
