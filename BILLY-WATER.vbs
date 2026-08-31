' ========================================================
' BILLY WATER - SGF  (inicializador sem janela de consola)
' ========================================================
Set WshShell = CreateObject("WScript.Shell")
Set FSO = CreateObject("Scripting.FileSystemObject")

strScriptDir = FSO.GetParentFolderName(WScript.ScriptFullName)

If FSO.FileExists(strScriptDir & "\BILLY-WATER.bat") Then
    WshShell.CurrentDirectory = strScriptDir
    WshShell.Run """" & strScriptDir & "\BILLY-WATER.bat""", 0, False
Else
    MsgBox "Nao encontrei BILLY-WATER.bat nesta pasta.", 16, "BILLY WATER"
End If
