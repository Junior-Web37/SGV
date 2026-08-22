; ==============================================================================
; INNO SETUP SCRIPT — SGV DESKTOP 1.0.0 (MOÇAMBIQUE)
; Compilador oficial para gerar SGV-Setup-1.0.0.exe instalador com assistente gráfico
; ==============================================================================

#define MyAppName "SGV Desktop"
#define MyAppVersion "1.0.0"
#define MyAppPublisher "SGV Moçambique"
#define MyAppURL "https://github.com/Junior-Web37/SGV"
#define MyAppExeName "SGV.vbs"
#define MyAppIcon "..\SGV.ico"

[Setup]
; Identificador único da aplicação (GUID gerado)
AppId={{D38F2B76-92F3-4B81-968A-8C1B33B9B277}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
AppSupportURL={#MyAppURL}
AppUpdatesURL={#MyAppURL}
DefaultDirName={autopf}\SGV Desktop
DefaultGroupName=SGV Desktop
AllowNoIcons=yes
OutputDir=..\dist
OutputBaseFilename=SGV-Setup-{#MyAppVersion}-Win64
SetupIconFile={#MyAppIcon}
Compression=lzma2/ultra64
SolidCompression=yes
WizardStyle=modern
PrivilegesRequired=lowest
DisableProgramGroupPage=yes

[Languages]
Name: "portuguese"; MessagesFile: "compiler:Languages\Portuguese.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked

[Files]
; Ficheiros de execução e scripts raiz
Source: "..\SGV.vbs"; DestDir: "{app}"; Flags: ignoreversion
Source: "..\SGV-Launcher.bat"; DestDir: "{app}"; Flags: ignoreversion
Source: "..\SGV.ico"; DestDir: "{app}"; Flags: ignoreversion
Source: "..\app-icon.png"; DestDir: "{app}"; Flags: ignoreversion

; Binários da aplicação e recursos
Source: "..\java-sgv\*"; DestDir: "{app}\java-sgv"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\{#MyAppName}"; Filename: "wscript.exe"; Parameters: """{app}\{#MyAppExeName}"""; WorkingDir: "{app}"; IconFilename: "{app}\SGV.ico"
Name: "{group}\SGV Launcher (Diagnóstico)"; Filename: "{app}\SGV-Launcher.bat"; WorkingDir: "{app}"; IconFilename: "{app}\SGV.ico"
Name: "{group}\{cm:UninstallProgram,{#MyAppName}}"; Filename: "{uninstallexe}"
Name: "{autodesktop}\{#MyAppName}"; Filename: "wscript.exe"; Parameters: """{app}\{#MyAppExeName}"""; WorkingDir: "{app}"; IconFilename: "{app}\SGV.ico"; Tasks: desktopicon

[Run]
Description: "{cm:LaunchProgram,{#StringChange(MyAppName, '&', '&&')}}"; Filename: "wscript.exe"; Parameters: """{app}\{#MyAppExeName}"""; Flags: nowait postinstall skipifsilent
