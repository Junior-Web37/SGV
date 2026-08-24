@echo off
setlocal
set "SCRIPT_DIR=%~dp0"
if not defined JAVA_HOME (
  for %%d in (
    "C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot"
    "C:\Program Files\Eclipse Adoptium\jdk-21.0.2.13-hotspot"
    "C:\Program Files\Eclipse Adoptium\jdk-21"
    "C:\Program Files\Java\jdk-21"
    "C:\Program Files\Java\jdk-25"
  ) do (
    if exist "%%~d\bin\java.exe" (
      set "JAVA_HOME=%%~d"
      goto :found_java
    )
  )
)
:found_java
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" (
  set "PATH=%JAVA_HOME%\bin;%PATH%"
  set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
) else (
  set "JAVA_EXE=java"
)
echo ========================================================
echo SGV Desktop - Sistema de Gestao de Vendas & Facturacao
echo ========================================================
echo.
echo A iniciar o sistema...
echo.
"%JAVA_EXE%" -jar "%SCRIPT_DIR%target\java-sgv-0.1.0.jar"
if %ERRORLEVEL% neq 0 (
    echo.
    echo Ocorreu um erro ao iniciar o SGV.
    echo Certifique-se de que tem o Java 21 ou 25 instalado (java -version).
    echo E que a base de dados XAMPP / MariaDB esta a correr na porta 3306.
    echo.
    pause
)
endlocal
