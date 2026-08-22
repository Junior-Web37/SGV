# SGV Desktop 1.0.5 — Notas de Lançamento

**Data:** 22 de Agosto de 2026  
**Tag:** `v1.0.5`

## Pacote de download

- **ZIP oficial:** [SGV-Desktop 1.0.5.zip](https://github.com/Junior-Web37/SGV/archive/refs/tags/v1.0.5.zip)
- **Página da release:** https://github.com/Junior-Web37/SGV/releases/tag/v1.0.5

## O que mudou nesta versão

O SGV já arranca, autentica e vende (v1.0.4). Restavam crashes de ComboBox do JavaFX 21
ao abrir Transferência / Compra com lista vazia ou ao apagar texto no editor:

- `IndexOutOfBoundsException: fromIndex 0, toIndex 1, size 0`
- `IllegalArgumentException: The start must be <= the end`

Nesta versão:

- Todos os ComboBox da aplicação (transferência, compra, venda, produção, filtros, relatórios e restantes formulários) foram protegidos
- O popup vazio deixa de ser clicável; o editor corrige o cursor antes do JavaFX rebentar
- Os bugs internos do JavaFX 21 deixam de mostrar o alerta vermelho — a aplicação continua a trabalhar

## Diagnóstico (versões anteriores)

O ecrã vermelho no arranque vinha do Spring a validar queries JPQL.
Na v1.0.2 a query `SaleItemRepository.findTopSellingProductsToday` usava `si.total`
— o campo JPA de `SaleItem` é `lineTotal`. Sem isso o bean `saleItemRepository`
não nasce e o dashboard não inicia.

Auditoria completa de **todas** as `@Query` e métodos derivados dos repositórios:
nenhum outro atributo inexistente. Foram endurecidos os riscos do Hibernate 6
que rebentariam a seguir (mesmo tipo de erro em cadeia):

- `COALESCE(campo BigDecimal, 0)` → `0.0` (Sale, Purchase, Expense, Stock)
- `Warehouse.findByIsActiveTrue` passou a `@Query` explícita (`w.isActive`)
- `!=` JPQL → `<>` nas comparações de estado

A v1.0.0 **não compilava** (`36 erros` no `mvn javafx:run`). Isso já está corrigido
desde a v1.0.1 (imports, métodos de caixa, `findByName`, `valorField`, `ordersTable`,
`stockMovementRepository`, variável `ex` duplicada, `AuditLog`).

## O que está incluído

- Código-fonte completo do SGV Desktop (Java 21 / JavaFX 21 / Spring Boot 3.1.4)
- Instalador em 1 clique (`INSTALAR-SGV.bat`) e desinstalador
- Launchers Windows (`SGV.vbs`, `SGV-Launcher.bat`) com atalho no Ambiente de Trabalho
- Script Inno Setup (`installer/sgv-setup.iss`) para gerar o assistente gráfico `.exe`
- 25+ migrações Flyway (schema `sgv` criado automaticamente no arranque)
- Conformidade fiscal Moçambique: CIVA 16%, NUIT Módulo 11, SAF-T MZ 1.01

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
