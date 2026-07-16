$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$javaSgvDir = Join-Path $projectRoot "java-sgv"

if (-not (Test-Path (Join-Path $javaSgvDir "pom.xml"))) {
    Write-Error "Maven project not found at $javaSgvDir"
}

if (-not $env:MAVEN_OPTS) {
    $env:MAVEN_OPTS = "-Xmx256m -Xms128m -XX:+UseSerialGC"
}

Set-Location $javaSgvDir
mvn clean compile javafx:run @args
