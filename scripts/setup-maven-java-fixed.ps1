# Setup script for Windows (requires Chocolatey)
# Run as Administrator in PowerShell

Write-Host "SGV setup script - installing Java (Temurin) and Maven via Chocolatey"

if (-not (Get-Command choco -ErrorAction SilentlyContinue)) {
    Write-Host "Chocolatey not found on system." -ForegroundColor Yellow
    Write-Host "Please install Chocolatey first: https://community.chocolatey.org/install" -ForegroundColor Cyan
    exit 1
}

Write-Host "Installing Temurin (OpenJDK)..."
choco install temurin -y

Write-Host "Installing Maven..."
choco install maven -y

if (Get-Command refreshenv -ErrorAction SilentlyContinue) {
    Write-Host "Refreshing environment variables..."
    refreshenv
} else {
    Write-Host "refreshenv not available — please open a new terminal to pick up PATH changes." -ForegroundColor Yellow
}

Write-Host "Installation attempted. Verify with:" -ForegroundColor Green
Write-Host "  java -version" -ForegroundColor Green
Write-Host "  mvn -v" -ForegroundColor Green
