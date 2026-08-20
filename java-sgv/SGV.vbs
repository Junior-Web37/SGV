' ========================================================
' SGV Desktop - Inicializador de Producao (Mocambique)
' ========================================================
Set WshShell = CreateObject("WScript.Shell")
Set FSO = CreateObject("Scripting.FileSystemObject")

strScriptDir = FSO.GetParentFolderName(WScript.ScriptFullName)

Dim javaExe
javaExe = "javaw.exe"

Dim candidates
candidates = Array(_
    "C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot\bin\javaw.exe", _
    "C:\Program Files\Eclipse Adoptium\jdk-21.0.2.13-hotspot\bin\javaw.exe", _
    "C:\Program Files\Eclipse Adoptium\jdk-21\bin\javaw.exe", _
    "C:\Program Files\Eclipse Adoptium\jdk-25\bin\javaw.exe", _
    "C:\Program Files\Java\jdk-21\bin\javaw.exe", _
    "C:\Program Files\Java\jdk-25\bin\javaw.exe" _
)

For Each path In candidates
    If FSO.FileExists(path) Then
        javaExe = """" & path & """"
        Exit For
    End If
Next

Dim jarPath, workDir
If FSO.FileExists(strScriptDir & "\target\java-sgv-0.1.0.jar") Then
    jarPath = strScriptDir & "\target\java-sgv-0.1.0.jar"
    workDir = strScriptDir
ElseIf FSO.FileExists(strScriptDir & "\java-sgv\target\java-sgv-0.1.0.jar") Then
    jarPath = strScriptDir & "\java-sgv\target\java-sgv-0.1.0.jar"
    workDir = strScriptDir & "\java-sgv"
Else
    If FSO.FileExists(strScriptDir & "\SGV-Launcher.bat") Then
        WshShell.CurrentDirectory = strScriptDir
        WshShell.Run """" & strScriptDir & "\SGV-Launcher.bat""", 1, False
        WScript.Quit 0
    End If
End If

WshShell.CurrentDirectory = workDir
strCmd = javaExe & " -jar """ & jarPath & """ --spring.profiles.active=mysql"
WshShell.Run strCmd, 0, False
