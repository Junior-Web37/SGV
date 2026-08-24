@echo off
setlocal
set "DIR=%~dp0"
if "%DIR:~-1%"=="\" set "DIR=%DIR:~0,-1%"

set "VBS=%TEMP%\sgv_shortcut_maker.vbs"

(
echo Set ws = CreateObject("WScript.Shell"^)
echo Set lk = ws.CreateShortcut(ws.SpecialFolders("Desktop"^) ^& "\SGV - Sistema de Vendas.lnk"^)
echo lk.TargetPath = "wscript.exe"
echo lk.Arguments = Chr(34^) ^& "%DIR%\SGV.vbs" ^& Chr(34^)
echo lk.WorkingDirectory = "%DIR%"
echo lk.IconLocation = "%DIR%\SGV.ico,0"
echo lk.Description = "SGV - Sistema de Gestao de Vendas e Facturacao"
echo lk.Save
) > "%VBS%"

cscript //nologo "%VBS%"
del "%VBS%" 2>nul

echo ========================================================
echo [OK] Atalho criado no seu Ambiente de Trabalho com sucesso!
echo ========================================================
echo.
pause
endlocal
