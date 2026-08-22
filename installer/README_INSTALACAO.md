# Guia de Instalação e Implantação — SGV Desktop 1.0.0

## 🇲🇿 Sistema de Gestão de Vendas & ERP para PMEs em Moçambique

---

### 1. Requisitos do Sistema
- **Sistema Operativo:** Windows 10 / 11 (64-bit) ou Windows Server.
- **Java:** Eclipse Adoptium Temurin JDK 21 (LTS) ou superior.
- **Base de Dados:** MariaDB / MySQL (XAMPP ou serviço nativo na porta `3306`).
- **Memória RAM:** Mínimo 4 GB (Recomendado 8 GB).

---

### 2. Opções de Instalação

#### Opção A: Instalação Rápida em 1-Clique (Recomendada)
1. Certifique-se de que o **XAMPP MySQL** está ligado (`Start` na porta `3306`).
2. Dê duplo clique no ficheiro **`INSTALAR-SGV.bat`** na raiz do projeto.
3. O instalador:
   - Verifica o Java e a Base de Dados.
   - Cria as pastas necessárias (`recibos`, `caixa`, `backups`).
   - Cria o atalho no **Ambiente de Trabalho (Desktop)** com o ícone oficial.
   - Adiciona o SGV ao **Menu Iniciar**.

#### Opção B: Instalador Executável Gráfico (`.EXE`)
1. Abra o ficheiro **`installer/sgv-setup.iss`** com o **Inno Setup Compiler**.
2. Clique em **Compile** (ou execute `installer/build-installer.bat`).
3. O ficheiro `dist/SGV-Setup-1.0.0-Win64.exe` será gerado, permitindo instalar o SGV através de um assistente gráfico estilo Windows padrão.

---

### 3. Credenciais Padrão de Acesso

| Perfil / Função | Nome de Utilizador | Palavra-passe | Nível de Acesso |
| :--- | :--- | :--- | :--- |
| **Administrador Geral** | `admin` | `admin` | Acesso total a todas as páginas e configurações |
| **Gerente de Loja** | `gerente.maputo` | `admin` | Gestão de stock, preços, vendas e relatórios |
| **Operador de Caixa** | `caixa1.maputo` | `admin` | Abertura/Fecho de turno, emissão de facturas e recibos |
| **Fiel de Armazém** | `fiel.machava` | `admin` | Recepção de compras, contagem de inventário e TWA |
| **Contabilidade** | `contabilidade` | `admin` | Apuramento de IVA (16%), SAF-T MZ e mapas fiscais |

---

### 4. Suporte e Resolução de Problemas
- **Erro ao Iniciar?** Execute `SGV-Launcher.bat` para ver o diagnóstico completo na consola.
- **Modo Silencioso:** Use `SGV.vbs` para abrir a aplicação sem a janela preta do terminal.
- **Desinstalação:** Execute `DESINSTALAR-SGV.bat` para remover os atalhos.
