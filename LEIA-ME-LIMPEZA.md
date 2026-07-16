# LEIA-ME: Instruções para Limpeza e Reinstalação Completa

## Passos para executar com sucesso:

### 1. Abra PowerShell como Administrador
- Clique em Iniciar
- Digite "PowerShell"
- Clique com botão direito em "Windows PowerShell"
- Selecione "Executar como administrador"
- Aceite a confirmação

### 2. Execute o script de limpeza e reinstalação
```powershell
Set-ExecutionPolicy -ExecutionPolicy Bypass -Scope Process -Force
& 'C:\xampp\htdocs\SGV\scripts\cleanup-and-reinstall.ps1'
```

### 3. Aguarde a conclusão
O script irá:
- ✓ Desinstalar Chocolatey completamente
- ✓ Remover todas as versões do JDK
- ✓ Limpar variáveis de ambiente (JAVA_HOME, M2_HOME, MAVEN_HOME)
- ✓ Reinstalar Chocolatey
- ✓ Instalar Java (Temurin/OpenJDK)
- ✓ Instalar Maven
- ✓ Configurar variáveis de ambiente automaticamente

### 4. Feche e abra um novo PowerShell
Após a conclusão, feche o PowerShell e abra um novo terminal para que as variáveis de ambiente sejam carregadas.

### 5. Verifique a instalação
```powershell
java -version
mvn -v
choco -v
```

---

## Alternativa Manual (Se o script não funcionar):

### 1. Desinstalar via Painel de Controle:
- Painel de Controle → Programas e Recursos
- Procure e desinstale:
  - "Temurin"
  - "Apache Maven"
  - "Chocolatey"

### 2. Remover pastas manualmente (Admin):
```powershell
Remove-Item -Path 'C:\ProgramData\chocolatey' -Recurse -Force
Remove-Item -Path 'C:\Program Files\Eclipse Adoptium' -Recurse -Force
Remove-Item -Path 'C:\Program Files\Java' -Recurse -Force
```

### 3. Limpar variáveis de ambiente:
```powershell
[Environment]::SetEnvironmentVariable('JAVA_HOME', $null, 'Machine')
[Environment]::SetEnvironmentVariable('M2_HOME', $null, 'Machine')
[Environment]::SetEnvironmentVariable('MAVEN_HOME', $null, 'Machine')
```

### 4. Instalar Chocolatey:
```powershell
Set-ExecutionPolicy Bypass -Scope Process -Force; [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072; iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))
```

### 5. Instalar Java e Maven:
```powershell
choco install temurin maven -y
```

### 6. Configurar JAVA_HOME:
```powershell
$javaPath = (Get-Command java).Source
$javaHome = Split-Path -Parent (Split-Path -Parent $javaPath)
[Environment]::SetEnvironmentVariable('JAVA_HOME', $javaHome, 'Machine')
```

---

## Notas importantes:
- ⚠️ Privilégios de administrador são OBRIGATÓRIOS
- ⚠️ Feche VS Code antes de executar se tiver terminais PowerShell abertos
- ⚠️ Abra um novo PowerShell após a conclusão para recarregar as variáveis

Se encontrar problemas, verifique se está realmente executando como Administrador (procure pelo indicador "Administrator" na barra de título do PowerShell).
