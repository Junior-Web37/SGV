# SGV Desktop 1.0.2 — Notas de Lançamento

**Data:** 22 de Agosto de 2026  
**Tag:** `v1.0.2`

## Pacote de download

- **ZIP oficial:** [SGV-Desktop 1.0.2.zip](https://github.com/Junior-Web37/SGV/archive/refs/tags/v1.0.2.zip)
- **Página da release:** https://github.com/Junior-Web37/SGV/releases/tag/v1.0.2

## Correcção desta versão

- `DashboardNavigationManager.showStockDetails` usava `stockMovementRepository` sem o campo existir — agora obtém o repositório via Spring (`applicationContext.getBean`).

## Correcção crítica desta versão

A v1.0.0 **não compilava** (`36 erros` no `mvn javafx:run`). Esta versão corrige todos os erros:

- imports em falta (`VBox`, `List`, `LinkedHashMap`)
- métodos de caixa (`getInitialValueAmount`, `getAmountValue`) alinhados com as entidades
- `CategoryRepository.findByName` e `DocumentPreviewDialog.atDefaultFormat` públicos
- `valorField` usado antes de ser declarado
- `ordersTable` / `getOrdersTable()` no gestor de navegação
- `stockMovementRepository` no Kardex de stock
- variável `ex` duplicada na abertura de caixa
- `AuditLog.getEntity()` / `getEntityId()`

## O que está incluído

- Código-fonte completo do SGV Desktop (Java 21 / JavaFX 21 / Spring Boot 3.1.4)
- Instalador em 1 clique (`INSTALAR-SGV.bat`) e desinstalador
- Launchers Windows (`SGV.vbs`, `SGV-Launcher.bat`) com atalho no Ambiente de Trabalho
- Script Inno Setup (`installer/sgv-setup.iss`) para gerar o assistente gráfico `.exe`
- 25+ migrações Flyway (schema `sgv` criado automaticamente no arranque)
- Conformidade fiscal Moçambique: CIVA 16%, NUIT Módulo 11, SAF-T MZ 1.01

## Correcções incluídas nesta publicação

- Ligações `fx:id` dos controladores FXML corrigidas (evita falhas ao abrir ecrãs)
- Duplicação de sintaxe no gestor de navegação eliminada
- Instalador, desinstalador e guia de implantação adicionados
- Validação NUIT (Módulo 11) nas filiais e parâmetros da empresa
- Gestão de lotes FEFO, diálogos de auditoria e resiliência da impressão térmica
- Sincronização de caixa nas despesas, pagamentos a fornecedores e reconciliação a crédito

## Requisitos

| Componente | Versão |
| :--- | :--- |
| Windows | 10 / 11 (64-bit) |
| Java JDK | 21 LTS ou 25 (Adoptium Temurin) |
| Base de dados | MariaDB / MySQL 8 via XAMPP, porta 3306 |

## Credenciais iniciais

| Perfil | Utilizador | Palavra-passe |
| :--- | :--- | :--- |
| Administrador | `admin` | `admin` |
| Gerente | `gerente.maputo` | `admin` |
| Operador de caixa | `caixa1.maputo` | `admin` |

Altere a palavra-passe do administrador no primeiro login (Configurações → Alterar Palavra-passe).
