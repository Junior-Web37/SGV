Write-Host "SGV setup script - installing Java (Temurin) and Maven via Chocolatey"

if (-not (Get-Command choco -ErrorAction SilentlyContinue)) {
    Write-Host "Chocolatey not found on system." -ForegroundColor Yellow
    Write-Host "Please install Chocolatey first: https://community.chocolatey.org/install" -ForegroundColor Cyan
    exit 1
}

Write-Host "Installing Temurin (OpenJDK) and Maven..."
choco install temurin maven -y

if (Get-Command refreshenv -ErrorAction SilentlyContinue) {
    Write-Host "Refreshing environment variables..."
    refreshenv
} else {
    Write-Host "refreshenv not available - please open a new terminal to pick up PATH changes." -ForegroundColor Yellow
}

function Get-IsElevated {
    $identity = [Security.Principal.WindowsIdentity]::GetCurrent()
    $principal = New-Object Security.Principal.WindowsPrincipal($identity)
    return $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
}

function Get-JavaHomeFromJava {
    $javaPath = Get-Command java -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Source -ErrorAction SilentlyContinue
    if (-not $javaPath) {
        return $null
    }
    $javaHome = Split-Path -Parent (Split-Path -Parent $javaPath)
    if (Test-Path $javaHome) {
        return $javaHome
    }
    return $null
}

function Get-MavenHomeFromMvn {
    $mvnPath = Get-Command mvn -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Source -ErrorAction SilentlyContinue
    if (-not $mvnPath) {
        return $null
    }
    $mavenHome = Split-Path -Parent (Split-Path -Parent $mvnPath)
    if (Test-Path $mavenHome) {
        return $mavenHome
    }
    return $null
}

function Set-EnvVar($name, $value) {
    if ([string]::IsNullOrEmpty($value)) {
        return
    }
    $scope = if (Get-IsElevated) { 'Machine' } else { 'User' }
    [Environment]::SetEnvironmentVariable($name, $value, $scope)
    Write-Host "$name set to $value ($scope scope)" -ForegroundColor Green
}

$javaHome = Get-JavaHomeFromJava
if ($javaHome) {
    Set-EnvVar 'JAVA_HOME' $javaHome
} else {
    Write-Host "Unable to locate java.exe after installation. Please verify Java was installed successfully." -ForegroundColor Yellow
}

$mavenHome = Get-MavenHomeFromMvn
if ($mavenHome) {
    Set-EnvVar 'M2_HOME' $mavenHome
} else {
    Write-Host "Unable to locate mvn after installation. Please verify Maven was installed successfully." -ForegroundColor Yellow
}

Write-Host "Installation complete. Open a new terminal or run refreshenv to pick up updated PATH and JAVA_HOME." -ForegroundColor Cyan
