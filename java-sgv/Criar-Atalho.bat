@echo off
setlocal
set "SCRIPT_DIR=%~dp0"
set "ICON_PATH=%SCRIPT_DIR%src\main\resources\icons\app-icon.ico"
set "TARGET_VBS=%SCRIPT_DIR%SGV.vbs"

echo ========================================================
echo SGV - Criar Atalho no Ambiente de Trabalho
echo ========================================================
echo.

set "VBS_HELPER=%TEMP%\make_sgv_shortcut_sub2.vbs"

(
echo Set oWS = CreateObject("WScript.Shell"^)
echo sDesktop = oWS.SpecialFolders("Desktop"^)
echo Set oLink = oWS.CreateShortcut(sDesktop ^& "\SGV - Sistema de Vendas.lnk"^)
echo oLink.TargetPath = "wscript.exe"
echo oLink.Arguments = Chr(34^) ^& "%TARGET_VBS%" ^& Chr(34^)
echo oLink.WorkingDirectory = "%SCRIPT_DIR%"
echo oLink.IconLocation = "%ICON_PATH%" ^& ",0"
echo oLink.Description = "SGV - Sistema de Gestao de Vendas e Facturacao"
echo oLink.Save
) > "%VBS_HELPER%"

cscript //nologo "%VBS_HELPER%"
del "%VBS_HELPER%" 2>nul

echo [OK] Atalho criado no seu Ambiente de Trabalho!
echo.
pause
endlocal
