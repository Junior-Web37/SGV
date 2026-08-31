@echo off
setlocal
set "DIR=%~dp0"
if "%DIR:~-1%"=="\" set "DIR=%DIR:~0,-1%"

set "VBS=%TEMP%\bw_shortcut_maker.vbs"

(
echo Set ws = CreateObject("WScript.Shell"^)
echo Set lk = ws.CreateShortcut(ws.SpecialFolders("Desktop"^) ^& "\BILLY WATER - SGF.lnk"^)
echo lk.TargetPath = "wscript.exe"
echo lk.Arguments = Chr(34^) ^& "%DIR%\BILLY-WATER.vbs" ^& Chr(34^)
echo lk.WorkingDirectory = "%DIR%"
echo lk.IconLocation = "%DIR%\icons\SGV.ico,0"
echo lk.Description = "BILLY WATER - Sistema de Gestao de Facturacao de Agua"
echo lk.Save
) > "%VBS%"

cscript //nologo "%VBS%"
del "%VBS%" 2>nul

echo ========================================================
echo [OK] Atalho "BILLY WATER - SGF" criado no seu Ambiente de Trabalho!
echo ========================================================
echo.
pause
endlocal
