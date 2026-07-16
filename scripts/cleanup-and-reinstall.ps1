# Cleanup and Reinstall Script for Java, Maven, and Chocolatey

Write-Host "=== CLEANUP E REINSTALAÇÃO ===" -ForegroundColor Cyan

# Verify admin
$isAdmin = [Security.Principal.WindowsIdentity]::GetCurrent() | ForEach-Object { (New-Object Security.Principal.WindowsPrincipal($_)).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator) }
if (-not $isAdmin) {
    Write-Host "Este script requer privilégios de administrador!" -ForegroundColor Red
    Write-Host "Por favor, execute PowerShell como Administrador e tente novamente." -ForegroundColor Yellow
    exit 1
}

Write-Host "Executando como administrador" -ForegroundColor Green

# ========== PHASE 1: UNINSTALL CHOCOLATEY ==========
Write-Host "`n=== FASE 1: Desinstalando Chocolatey ===" -ForegroundColor Yellow

if (Test-Path 'C:\ProgramData\chocolatey') {
    Write-Host "Removendo C:\ProgramData\chocolatey..."
    try {
        Remove-Item -Path 'C:\ProgramData\chocolatey' -Recurse -Force -ErrorAction Stop
        Write-Host "Chocolatey removido com sucesso" -ForegroundColor Green
    } catch {
        Write-Host "Erro ao remover Chocolatey: $_" -ForegroundColor Red
    }
}

# ========== PHASE 2: UNINSTALL JDK VERSIONS ==========
Write-Host "`n=== FASE 2: Desinstalando JDK ===" -ForegroundColor Yellow

$jdkPaths = @(
    'C:\Program Files\Eclipse Adoptium',
    'C:\Program Files\Java',
    'C:\Program Files (x86)\Java'
)

foreach ($path in $jdkPaths) {
    if (Test-Path $path) {
        Write-Host "Removendo $path..."
        try {
            Remove-Item -Path $path -Recurse -Force -ErrorAction Stop
            Write-Host "Removido: $path" -ForegroundColor Green
        } catch {
            Write-Host "Erro ao remover $path : $_" -ForegroundColor Red
        }
    }
}

# ========== PHASE 3: REMOVE ENVIRONMENT VARIABLES ==========
Write-Host "`n=== FASE 3: Removendo variáveis de ambiente ===" -ForegroundColor Yellow

$envVars = @('JAVA_HOME', 'M2_HOME', 'MAVEN_HOME')
foreach ($var in $envVars) {
    $userValue = [Environment]::GetEnvironmentVariable($var, 'User')
    $machineValue = [Environment]::GetEnvironmentVariable($var, 'Machine')
    
    if ($userValue) {
        Write-Host "Removendo $var do escopo User..."
        [Environment]::SetEnvironmentVariable($var, $null, 'User')
        Write-Host "Removido: $var (User)" -ForegroundColor Green
    }
    
    if ($machineValue) {
        Write-Host "Removendo $var do escopo Machine..."
        [Environment]::SetEnvironmentVariable($var, $null, 'Machine')
        Write-Host "Removido: $var (Machine)" -ForegroundColor Green
    }
}

# Clear PATH references to removed tools
$pathValue = [Environment]::GetEnvironmentVariable('PATH', 'Machine')
$oldPath = $pathValue
$pathValue = $pathValue -replace 'C:\\ProgramData\\chocolatey\\bin;?', ''
$pathValue = $pathValue -replace 'C:\\ProgramData\\chocolatey\\lib\\maven\\[^;]*\\bin;?', ''
$pathValue = $pathValue -replace 'C:\\Program Files\\Eclipse Adoptium\\[^;]*\\bin;?', ''
$pathValue = $pathValue -replace '%MAVEN_HOME%\\bin;?', ''

if ($oldPath -ne $pathValue) {
    Write-Host "Limpando PATH..."
    [Environment]::SetEnvironmentVariable('PATH', $pathValue, 'Machine')
    Write-Host "PATH atualizado" -ForegroundColor Green
}

# ========== PHASE 4: REINSTALL CHOCOLATEY ==========
Write-Host "`n=== FASE 4: Instalando Chocolatey ===" -ForegroundColor Yellow

try {
    $chocolateyInstallScript = 'https://community.chocolatey.org/install.ps1'
    Write-Host "Baixando instalador do Chocolatey..."
    [Net.ServicePointManager]::SecurityProtocol = [Net.ServicePointManager]::SecurityProtocol -bor [Net.SecurityProtocolType]::Tls12
    
    $chocolateyDownload = Invoke-WebRequest -Uri $chocolateyInstallScript -UseBasicParsing
    $installScript = $chocolateyDownload.Content
    
    Write-Host "Executando instalador..."
    Invoke-Expression $installScript
    
    if (Test-Path 'C:\ProgramData\chocolatey\bin\choco.exe') {
        Write-Host "Chocolatey instalado com sucesso!" -ForegroundColor Green
        & choco --version
    } else {
        Write-Host "Falha na instalação do Chocolatey" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "Erro ao instalar Chocolatey: $_" -ForegroundColor Red
    exit 1
}

# ========== PHASE 5: REINSTALL JAVA AND MAVEN ==========
Write-Host "`n=== FASE 5: Instalando Java (Temurin) e Maven ===" -ForegroundColor Yellow

try {
    Write-Host "Instalando Temurin (OpenJDK)..."
    & choco install temurin -y
    
    Write-Host "Instalando Maven..."
    & choco install maven -y
    
    Write-Host "Atualizando variáveis de ambiente..."
    if (Test-Path 'C:\ProgramData\chocolatey\bin\RefreshEnv.cmd') {
        & cmd /c "C:\ProgramData\chocolatey\bin\RefreshEnv.cmd"
    }
} catch {
    Write-Host "Erro ao instalar Java/Maven: $_" -ForegroundColor Red
    exit 1
}

# ========== PHASE 6: SET ENVIRONMENT VARIABLES ==========
Write-Host "`n=== FASE 6: Configurando variáveis de ambiente ===" -ForegroundColor Yellow

function Get-JavaHomeFromInstall {
    $javaPath = Get-Command java -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Source -ErrorAction SilentlyContinue
    if ($javaPath) {
        $javaHome = Split-Path -Parent (Split-Path -Parent $javaPath)
        if (Test-Path $javaHome) {
            return $javaHome
        }
    }
    return $null
}

function Get-MavenHomeFromInstall {
    $mvnPath = Get-Command mvn -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Source -ErrorAction SilentlyContinue
    if ($mvnPath) {
        $mvnHome = Split-Path -Parent (Split-Path -Parent $mvnPath)
        if (Test-Path $mvnHome) {
            return $mvnHome
        }
    }
    return $null
}

$javaHome = Get-JavaHomeFromInstall
if ($javaHome) {
    Write-Host "Configurando JAVA_HOME = $javaHome"
    [Environment]::SetEnvironmentVariable('JAVA_HOME', $javaHome, 'Machine')
    Write-Host "JAVA_HOME configurado!" -ForegroundColor Green
} else {
    Write-Host "Aviso: Não foi possível localizar JAVA_HOME automaticamente" -ForegroundColor Yellow
}

$mavenHome = Get-MavenHomeFromInstall
if ($mavenHome) {
    Write-Host "Configurando M2_HOME = $mavenHome"
    [Environment]::SetEnvironmentVariable('M2_HOME', $mavenHome, 'Machine')
    Write-Host "M2_HOME configurado!" -ForegroundColor Green
} else {
    Write-Host "Aviso: Não foi possível localizar M2_HOME automaticamente" -ForegroundColor Yellow
}

# ========== PHASE 7: VERIFY INSTALLATION ==========
Write-Host "`n=== FASE 7: Verificando instalação ===" -ForegroundColor Yellow

$env:JAVA_HOME = Get-JavaHomeFromInstall
$env:M2_HOME = Get-MavenHomeFromInstall

Write-Host "`nJava:" -ForegroundColor Cyan
java -version

Write-Host "`nMaven:" -ForegroundColor Cyan
mvn -v

Write-Host "`nChocolatey:" -ForegroundColor Cyan
choco -v

Write-Host "`n=== INSTALAÇÃO COMPLETA ===" -ForegroundColor Green
Write-Host "Por favor, abra um novo PowerShell para que as variáveis de ambiente sejam recarregadas." -ForegroundColor Yellow
