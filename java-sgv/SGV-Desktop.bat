@echo off
setlocal
set "SCRIPT_DIR=%~dp0"
if not defined JAVA_HOME (
  if exist "C:\Users\DELL\.jdk\jdk-25.0.2\bin\java.exe" (
    set "JAVA_HOME=C:\Users\DELL\.jdk\jdk-25.0.2"
  )
)
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" (
  set "PATH=%JAVA_HOME%\bin;%PATH%"
  set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
) else (
  set "JAVA_EXE=java"
)
echo ========================================================
echo SGV Desktop - Sistema de Gestao de Vendas
echo ========================================================
echo.
echo A iniciar o sistema...
echo.
"%JAVA_EXE%" -jar "%SCRIPT_DIR%target\java-sgv-0.1.0.jar"
if %ERRORLEVEL% neq 0 (
    echo.
    echo Ocorreu um erro ao iniciar o SGV.
    echo Certifique-se de que tem o Java 25 instalado (java -version).
    echo E que a base de dados XAMPP / MariaDB esta a correr.
    echo.
    pause
)
endlocal
