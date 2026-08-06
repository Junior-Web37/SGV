# APPLY_MIGRATIONS.ps1
# Safe helper to backup DB and apply migrations manually using mysql CLI.
# Usage: .\APPLY_MIGRATIONS.ps1 -Host 127.0.0.1 -Port 3306 -User root -Password "" -Database sgv
param(
    [string]$Host = '127.0.0.1',
    [int]$Port = 3306,
    [string]$User = 'root',
    [string]$Password = '',
    [string]$Database = 'sgv',
    [string]$MigrationsDir = "src\main\resources\db\migration"
)

function Exec-Command($cmd) {
    Write-Host "EXEC: $cmd"
    & cmd /c $cmd
}

# 1) Backup
$timestamp = (Get-Date).ToString('yyyyMMdd_HHmmss')
$backupFile = "$PWD\sgv_backup_$timestamp.sql"
$dumpCmd = "mysqldump -h $Host -P $Port -u $User --password=$Password $Database > \"$backupFile\""
Write-Host "Backing up DB to $backupFile"
Exec-Command $dumpCmd

# 2) Apply migrations (run each SQL in migration folder in natural sort order)
$files = Get-ChildItem -Path $MigrationsDir -Filter 'V*.sql' | Sort-Object Name
foreach ($f in $files) {
    Write-Host "Applying migration: $($f.Name)"
    $applyCmd = "mysql -h $Host -P $Port -u $User --password=$Password $Database < \"$($f.FullName)\""
    Exec-Command $applyCmd
}

Write-Host "Migrations applied. Check the DB and application logs."