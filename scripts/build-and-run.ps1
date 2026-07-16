<#
.SYNOPSIS
Builds and runs the java-sgv Spring Boot application on Windows.

.DESCRIPTION
This script checks for Maven and Java, ensures JAVA_HOME is available, runs a package build, and then starts the generated JAR.

.EXAMPLE
.
  .\scripts\build-and-run.ps1

.EXAMPLE
  .\scripts\build-and-run.ps1 -PackageOnly
#>
param(
    [switch]$PackageOnly
)

$root = Split-Path -Parent $PSScriptRoot
$projectDir = Join-Path $root 'java-sgv'

Write-Host "Project root: $projectDir"

function Get-CommandPath($name) {
    $cmd = Get-Command $name -ErrorAction SilentlyContinue
    if ($null -ne $cmd) {
        return $cmd.Path
    }
    return $null
}

function Find-JavaExecutable() {
    $javaPath = Get-CommandPath 'java'
    if ($javaPath) {
        return $javaPath
    }

    $jdkRoots = @(
        'C:\Program Files\Eclipse Adoptium',
        'C:\Program Files\Java'
    )

    foreach ($root in $jdkRoots) {
        if (-not (Test-Path $root)) {
            continue
        }

        $candidateJdks = Get-ChildItem -Path $root -Directory -Filter 'jdk*' -ErrorAction SilentlyContinue | Sort-Object Name -Descending
        foreach ($jdk in $candidateJdks) {
            $candidate = Join-Path $jdk.FullName 'bin\java.exe'
            if (Test-Path $candidate) {
                return $candidate
            }
        }
    }

    return $null
}

function Get-MavenExecutable() {
    $mvnPath = Get-CommandPath 'mvn'
    if ($mvnPath) {
        return $mvnPath
    }

    $mavenLib = 'C:\ProgramData\chocolatey\lib\maven'
    if (Test-Path $mavenLib) {
        $mavenInstall = Get-ChildItem -Path $mavenLib -Directory -Filter 'apache-maven-*' -ErrorAction SilentlyContinue | Sort-Object Name -Descending | Select-Object -First 1
        if ($mavenInstall) {
            $candidate = Join-Path $mavenInstall.FullName 'bin\mvn.cmd'
            if (Test-Path $candidate) {
                return $candidate
            }
        }
    }

    $chocoBin = 'C:\ProgramData\chocolatey\bin\mvn.cmd'
    if (Test-Path $chocoBin) {
        return $chocoBin
    }

    return $null
}

function Set-JavaHomeIfMissing() {
    if ($env:JAVA_HOME -and (Test-Path $env:JAVA_HOME)) {
        return
    }

    $javaPath = Find-JavaExecutable
    if ($null -ne $javaPath) {
        $javaHome = Split-Path -Parent (Split-Path -Parent $javaPath)
        if (Test-Path $javaHome) {
            $env:JAVA_HOME = $javaHome
            Write-Host "Derived JAVA_HOME from java.exe: $env:JAVA_HOME" -ForegroundColor Yellow
        }
    }
}

$mvnExecutable = Get-MavenExecutable
if (-not $mvnExecutable) {
    Write-Host "Maven not found." -ForegroundColor Red
    Write-Host "Install Maven and retry. If you have Chocolatey, use scripts/setup-maven-java.ps1." -ForegroundColor Yellow
    exit 1
}

$javaExecutable = Find-JavaExecutable
if (-not $javaExecutable) {
    Write-Host "Java not found." -ForegroundColor Red
    Write-Host "Install a JDK and retry. If you have Chocolatey, use scripts/setup-maven-java.ps1." -ForegroundColor Yellow
    exit 1
}

Set-JavaHomeIfMissing
if (-not $env:JAVA_HOME) {
    Write-Host "JAVA_HOME was not defined and could not be derived automatically." -ForegroundColor Red
    Write-Host "Please set JAVA_HOME to your JDK root, e.g. C:\\Program Files\\Eclipse Adoptium\\jdk-21.0.8.9-hotspot." -ForegroundColor Yellow
    exit 1
}

Set-Location $projectDir

Write-Host "Running Maven package..." -ForegroundColor Cyan
$mvnArgs = '-DskipTests','package'
$packageResult = & $mvnExecutable @mvnArgs
if ($LASTEXITCODE -ne 0) {
    Write-Host "Maven build failed with exit code $LASTEXITCODE." -ForegroundColor Red
    exit $LASTEXITCODE
}

if ($PackageOnly) {
    Write-Host "Package completed. Artifact is target\java-sgv-0.1.0.jar" -ForegroundColor Green
    exit 0
}

$jarPath = Join-Path $projectDir 'target\java-sgv-0.1.0.jar'
if (-not (Test-Path $jarPath)) {
    Write-Host "Expected JAR not found: $jarPath" -ForegroundColor Red
    exit 1
}

Write-Host "Starting Spring Boot application..." -ForegroundColor Cyan
& $javaExecutable -jar $jarPath
