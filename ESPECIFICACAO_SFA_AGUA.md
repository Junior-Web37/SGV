# 💧 SFA — Sistema de Facturação de Água (Moçambique)

**Especificação completa** — espelhada 1:1 no **SGV — Sistema de Gestão de Vendas & Facturação**

> **Objectivo:** desenvolver um sistema de facturação de água que seja, em **tudo**, igual ao SGV
> (descrição, visual, layout, design, regras de negócio, fluxo de operações e de dados, páginas/janelas,
> fluxo de interacções, totais, componentes, ícones e dimensões) **com uma única excepção**:
> o **menu superior** é adaptado ao domínio da água. O tamanho das coisas, os ícones, o design e o layout
> **permanecem idênticos**.

| Campo | Valor |
|---|---|
| Nome do produto | **SFA** — Sistema de Facturação de Água |
| Mercado | Moçambique (pt-MZ) · 100% offline · Conformidade fiscal AT / CIVA |
| Base de referência | `Junior-Web37/SGV` — commit `45fa142` |
| Alvo visual | **"like the same"** — réplica do Design System SGV |
| Única diferença pedida | Menu superior (navegação de topo) adaptado à água |

---

## Índice

1. [Visão Geral](#1-visão-geral)
2. [Stack Tecnológica](#2-stack-tecnológica)
3. [Design System (idêntico ao SGV)](#3-design-system)
4. [A única mudança: Menu Superior](#4-a-única-mudança-menu-superior)
5. [Mapa de Correspondência SGV → SFA](#5-mapa-de-correspondência-sgv--sfa)
6. [Modelo de Domínio / Entidades](#6-modelo-de-domínio--entidades)
7. [Regras de Negócio](#7-regras-de-negócio)
8. [Fluxo de Operações e de Dados](#8-fluxo-de-operações-e-de-dados)
9. [Páginas / Janelas](#9-páginas--janelas)
10. [Fluxo de Interacções](#10-fluxo-de-interacções)
11. [Mapas & Relatórios](#11-mapas--relatórios)
12. [Segurança / RBAC / Configuração](#12-segurança--rbac--configuração)
13. [Componentes Reutilizáveis (kit de UI)](#13-componentes-reutilizáveis)
14. [Reutilização do Design System SGV](#14-reutilização-do-design-system-sgv)
15. [Glossário](#15-glossário)

---

## 1. Visão Geral

O **SFA** é um sistema completo de **facturação de água e saneamento** para concessionárias e serviços
municipais de águas, desenvolvido em **Java 21 (LTS) / JavaFX 21** e **Spring Boot 3.1.4**, com
persistência sobre **MariaDB / MySQL**. Opera **100% offline** e está em conformidade fiscal com
Moçambique (**Autoridade Tributária - AT / CIVA / SAF-T MZ**).

O SFA é a **tradução de domínio do SGV**: onde o SGV vende **produtos** e emite **facturas de venda**,
o SFA **lê contadores**, **calcula consumos por escalão**, **emite facturas de água** e **cobra** esses
valores. Toda a mecânica de transacções, tesouraria, contas correntes, numeração fiscal, auditoria,
backup e licenciamento é herdada do SGV.

**Ciclo de valor (tradução do SGV):**

| Ciclo SGV | Ciclo SFA |
|---|---|
| Cadastrar artigo → precificar | Cadastrar **contador** → definir **tarifa/escalões** |
| Vender (PDV) → emissão de factura | **Efectuar leitura** → cálculo de consumo → emissão de **factura de água** |
| Receber pagamento | **Cobrar** a factura (Numerário, M-Pesa, e-Mola, mKesh, POS, transferência) |
| Gestão de stock / reclamações | Gestão de **consumos não lidos/estimados**, **reclamações** e rectificações |
| Mapa de vendas / IVA | Mapa de **facturação** por zona / **apuramento de IVA** / água não facturada |

---

## 2. Stack Tecnológica

**Idêntica ao SGV.**

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 21 (LTS) — também compila em Java 25 |
| UI | JavaFX 21 (FXML + panes programáticas) |
| Backend | Spring Boot 3.1.4 (perfil `web-application-type=none`) |
| Persistência | Spring Data JPA / Hibernate 6 sobre MariaDB / MySQL 8 (XAMPP, porta 3306, user `root`) |
| Migrações | Flyway (V1 → V29 no SGV; o SFA herda e renomeia) |
| Segurança | `spring-security-crypto` (BCrypt) · RBAC por permissões `PAGE:ACTION` |
| Cache / Agendamento | `@EnableCaching` (ConcurrentMap) · `@EnableScheduling` |
| Documentos | PDFBox 3.0.2 (A4 / A5 / 80mm / 58mm) · POI (Excel) |
| Ficheiros | `app-icon.ico` / `app-icon.png` · mysqldump via subprocesso |
| Base de dados default | Esquema `sfa` (o Flyway cria automaticamente todas as migrações no arranque) |

> Se o SFA for desenvolvido como **fork** do SGV, basta:
> 1. Recriar o esquema com nome `sfa`;
> 2. Renomear o pacote `com.sgv` → `com.sfa`;
> 3. Manter 100% das classes de UI (controller, diálogos, CSS, FXML) **inalteradas**;
> 4. Alterar apenas o **conteúdo textual e o menu superior**.

**Credenciais padrão:** `admin` / `admin` (recomendado alterar no primeiro login em Configurações → Segurança).

---

## 3. Design System

> ⚠️ **Esta secção não altera nada relativamente ao SGV.** Descrevemos os mesmos tokens, classes e
> componentes para que o SFA seja visualmente indistinguível (mesmo tamanho de coisas, mesmos ícones,
> mesmo layout). Apenas **os textos e o domínio** mudam.

### 3.1 Paleta de cores (regra 60/30/10)

| Papel | Cor | Uso |
|---|---|---|
| Superfície (60%) | `#F8FAFC` | Fundo geral da aplicação / painéis |
| Cartões + contornos (30%) | `#FFFFFF` / `#E2E8F0` | Cards, tabelas, inputs |
| Accent (10%) | `#2563EB` / `#10B981` | Acções primárias, foco, destaque |

| Token | Valor | Aplicação |
|---|---|---|
| `--primary` | `#2563EB` | Botão primário, foco de input |
| `--primary-dark` | `#1D4ED8` | Hover do primário |
| `--primary-deep` | `#1E40AF` | Gradiente cabeçalho/borda |
| `--nav-gradient` | `#1E3A8A → #1D4ED8 → #1E40AF` | Barra de navegação superior |
| `--success` | `#10B981` | Confirmações, troco, valores pagos |
| `--success-dark` | `#059669` | Hover success |
| `--danger` | `#EF4444` | Eliminar, anular, stock 0 |
| `--danger-soft` | `#FEE2E2` | Fundo de estados de perigo |
| `--danger-text` | `#DC2626` / `#991B1B` | Texto de perigo |
| `--warning` | `#F59E0B` | Alertas, pendências, stock baixo |
| `--warning-soft` | `#FEF3C7` | Fundo de aviso |
| `--warning-text` | `#92400E` | Texto de aviso |
| `--accent-purple` | `#7C3AED` | Destaques secundários / unidade |
| `--purple-soft` | `#EDE9FE` | KPI purple |
| `--text-strong` | `#0F172A` | Títulos, números grandes |
| `--text-mid` | `#475569` / `#334155` | Rótulos, subtítulos |
| `--text-soft` | `#64748B` / `#94A3B8` | Placeholders, metadados |
| `--border` | `#E2E8F0` / `#CBD5E1` / `#93C5FD` | Contornos |

### 3.2 Tipografia

```css
.root {
    -fx-font-family: "Segoe UI", -apple-system, BlinkMacSystemFont, "Helvetica Neue", Arial, sans-serif;
    -fx-background-color: #F8FAFC;
    -fx-text-fill: #0F172A;
}
```

| Elemento | Tamanho | Peso | Cor |
|---|---|---|---|
| Número KPI grande | 26–28px | 900 | `#0F172A` |
| Título de página (sub-bar) | 13px | 800 | `#0F172A` |
| Subtítulo de página | 12px | 600 | `#475569` |
| Cabeçalho de tabela | 11px | 800/900 | `#475569` / `#1E40AF` |
| Célula de tabela | 13px | 500 | `#0F172A` |
| Botão | 12–13px | 700–800 | variável |
| Input | 12–13px | 600 | `#0F172A` |
| Prompt / placeholder | – | – | `#94A3B8` |

### 3.3 Raio, sombras e espaçamento

| Elemento | Valor |
|---|---|
| Raio de cartão / painel | `10` (CSS) · `12` (diálogos e janelas) |
| Raio de botão | `6` |
| Raio de KPI-icon | `12` |
| Sombra de cartão | `dropshadow(gaussian, rgba(15,23,42,0.05), 10, 0, 0, 4)` |
| Sombra hover cartão | `rgba(15,23,42,0.08), 12, 0, 0, 6` · `-fx-translate-y: -1` |
| Sombra botão primário | `dropshadow(gaussian, rgba(37,99,235,0.28), 8, 0, 0, 2)` |
| Padding de cartão | `20` |
| Cell height de tabela | `42px` |
| Tooltip | fundo `#0F172A`, texto `#FFFFFF`, raio `6` |

### 3.4 Classes CSS reutilizadas (iguais ao SGV)

**Botões:** `.button`, `.primary-button`, `.primary-button-success`, `.secondary-button`,
`.danger-button`, `.btn-primary-ux`, `.btn-secondary-ux`, `.btn-danger-ux`, `.login-button`,
`.header-action-btn`.

**Cartões:** `.card-box`, `.card-pane`, `.login-box`, `.filter-card`.

**Inputs:** `.text-field`, `.password-field`, `.text-area`, `.combo-box`, `.date-picker`, `.form-input`
(todos com foco azul + sombra `rgba(29,78,216,0.20)`, raio `6`, borda `#CBD5E1`).

**Tabelas:** `.table-view`, `.column-header-background`, `.column-header`, `.table-row-cell`,
`.table-cell`, `.compact-table`, `.cash-table`.

**Badges:** `.badge`, `.badge-success` (`#D1FAE5`/`#065F46`), `.badge-danger` (`#FEE2E2`/`#991B1B`),
`.badge-warning` (`#FEF3C7`/`#92400E`), `.badge-info` (`#DBEAFE`/`#1E40AF`).

**KPIs:** `.kpi-icon`, `.kpi-icon-blue` (`#DBEAFE`), `.kpi-icon-green` (`#D1FAE5`),
`.kpi-icon-purple` (`#EDE9FE`), `.kpi-icon-orange` (`#FEF3C7`).

**Navegação:** `.top-nav-bar`, `.nav-btn`, `.nav-btn-home`, `.nav-btn-back`, `.nav-bell-btn`,
`.logout-btn`, `.nav-btn.active`.

**Cabeçalho de janela/formulário:** `.form-header`, `.page-header`, `.header-title`,
`.header-subtitle`, `.section-header`.

**Tabs:** `.tab-pane`, `.tab`, `.tab-label`, `.tab-content-area`.

---

## 4. A Única Mudança: Menu Superior

> **Regra:** mantém-se o mesmo **layout, dimensões (`58px` de altura), ícones, tipografia, gradiente azul,
> cartão de utilizador, sino de alertas, badge de caixa e botão Sair**. Só **mudam os textos/domínio**.
> A estrutura de Navegação (raiz com 7 botões + `🛡️ Logs` + sub-navegação com `◂ Voltar`) fica idêntica.

### 4.1 Barra superior (top bar) — `dashboard.fxml` (topo)

**Antes (SGV):**
```
[ ⚡ SGV PRO ] | [ ⌂ Painel  Vendas ▾  Caixa ▾  Stock ▾  Armazém ▾  Relatórios ▾  Configurações ▾  🛡️ Logs ] | [○ Caixa Fechado] [🔔 Alertas] [A Administrador / ADMIN] [⏻ Sair]
```

**Depois (SFA) — mesma disposição, textos de domínio:**
```
[ 💧 SFA PRO ] | [ ⌂ Painel  Facturação ▾  Cobrança ▾  Leituras ▾  Rede ▾  Relatórios ▾  Configurações ▾  🛡️ Logs ] | [○ Caixa Fechado] [🔔 Alertas] [A Administrador / ADMIN] [⏻ Sair]
```

> **Nota:** o `SGV` → `SFA`, o relâmpago `⚡` → gota de água `💧`, e o badge `PRO` permanece.
> Tudo o resto (dimensões 58px, raio 8, `rgba(0,0,0,0.25)` por trás do logo, separadores verticais,
> badge de caixa em vermelho `#FCA5A5`, sino `🔔`, avatar circular 26px, botão `⏻ Sair` em `#FCA5A5`)
> fica **byte a byte igual**.

### 4.2 Mapa raiz (sub-nav por item)

| # | SGV (raiz) | SFA (raiz) | Sub-navegação (traduzida) |
|---|---|---|---|
| 1 | `⌂ Painel` | `⌂ Painel` | — (página inicial) |
| 2 | `Vendas ▾` | `Facturação ▾` | `⚡ Emissão / Leitura` · `📄 Facturas & Consumos` · `👥 Clientes` |
| 3 | `Caixa ▾` | `Cobrança ▾` | `💼 Sessão Caixa` · `💳 Recebimentos` · `📉 Despesas` · `📈 Resumo Caixa` |
| 4 | `Stock ▾` | `Leituras ▾` | `📑 Tarifas & Escalões` · `🏷️ Contadores & Tarifas` · `📊 Consumos / Leituras` · `🥖 Zonas de Leitura` |
| 5 | `Armazém ▾` | `Rede ▾` | `🏢 Postos / Zonas` · `🚚 Fornecedores` · `📥 Compras / Materiais` · `📦 Stock de Materiais` · `🔁 Transferências` |
| 6 | `Relatórios ▾` | `Relatórios ▾` | `📊 Mapa de Facturação` · `🔥 Maiores Consumos` · `🏛️ Apuramento IVA` · `👥 Contas Correntes` · `📦 Consumo por Contador` · `🏢 Facturação por Zona` · `🔁 Perdas / Água Não Facturada` · `📑 Pag. Fornecedores` |
| 7 | `Configurações ▾` | `Configurações ▾` | `⚙️ Parâmetros` · `👤 Utilizadores` · `💾 Backups` · `🎓 Treinamento` · `🔑 Licenciamento` · `🔒 Segurança` |
| 8 | `🛡️ Logs` | `🛡️ Logs` | (página de auditoria — igual) |

### 4.3 Sub-bar de título (page title sub-bar) — as dimensões `40px`, fontes e cores mantêm-se

Exemplo na página de Emissão:
```
Facturação / Emissão de Factura de Água            Bem-vindo ao SFA.
```

---

## 5. Mapa de Correspondência SGV → SFA

> Cada módulo/tela do SGV é mapeado 1:1. **O design, layout, colunas de tabelas, cartões KPI e
> componentes são os mesmos**; apenas os *dados* são de água.

| Módulo SGV | Tela SGV | Módulo SFA | Tela SFA |
|---|---|---|---|
| Vendas | Dashboard / Painel | Facturação | Painel Geral (KPIs de facturação, consumos, dívidas, caixa) |
| PDV | `sale_form.fxml` | Emissão | `reading_form.fxml` — **Leitura & Emissão de Factura** |
| Vendas | Histórico `DashboardCrudManager` | Facturação | Histórico de **Facturas & Consumos** |
| Clientes | `customer_form.fxml` | Clientes | `customer_form.fxml` — **Consumidor/Cliente** (com contador, zona, NUIT) |
| Conta corrente | Extrato do Cliente | Conta corrente | Extrato/**estado de dívida** por consumidor |
| Catálogo | Categorias | Tarifário | **Categorias de consumo** (Doméstico, Comercial, Industrial, Institucional, Público) |
| Unidades | `metric_unit_form.fxml` | Unidades | **Unidades** (`m³`, `L`, `I1000`) |
| Produtos | Catálogo de artigos | Contadores | **Catálogo de Contadores & Tarifas** |
| Stock filial | Inventário | Consumos | **Leituras / Consumos registados** por contador |
| Stock armazém | Stock Central | Materiais | **Stock de materiais de rede** (tubos, contadores, válvulas) |
| Compras | `purchase_form.fxml` | Compras | **Compras de materiais** |
| Fornecedores | `supplier_form.fxml` | Fornecedores | **Fornecedores de materiais/equipamento** |
| Transferências TWA | Guia de transferência | Transferências | **Guias de transferência de materiais/contadores** entre zonas/armazém |
| Produção | Ordem de produção | (opcional) | **Tarifação por escalão** (cálculo do consumo em blocos) |
| Caixa | `cash_session.fxml` | Cobrança | **Sessão & Fecho de Caixa** |
| Pagamentos | `payment_form.fxml` | Recebimentos | **Cobrança / Recebimento de facturas** |
| Despesas | `expense_form.fxml` | Despesas | **Despesas operacionais** |
| Resumo Financeiro | `financeiroPane` | Resumo | **Resumo de tesouraria** |
| Relatórios | `reports.fxml` | Relatórios | **Mapas de facturação, IVA, perdas, devedores** |
| Sistema/Config | `sistema_module.fxml` | Config | **Parâmetros da concessionária, utilizadores, backup, licença, treino, segurança** |
| Auditoria | Logs & Auditoria | Auditoria | **Logs & Auditoria** (idêntico) |

---

## 6. Modelo de Domínio / Entidades

> **Convenção:** tabelas do SFA espelham as do SGV com o mesmo kind de estrutura (BigDecimal pós-V17/V22,
> campos `created_at`, `updated_at`, `@Version` nas entidades de stock, `series_counters` para numeração
> atómica, triggers de protecção fiscal). Apenas os nomes e campos de domínio de água mudam.

### 6.1 Entidades principais e tabelas

| Entidade SFA | Tabela | Equivalente SGV | Descrição |
|---|---|---|---|
| `Consumer` (Cliente/Consumidor) | `consumers` | `customers` | Nome, NUIT (9/10 dígitos, módulo 11), contacto, endereço, zona de leitura, tipo de consumo, **contador associado**, limite de crédito, saldo devedor, `fidelityPoints` (opcional), `active`. Código `CLI-####`. |
| `WaterMeter` (Contador) | `water_meters` | `products` | Nº de série do contador, marca, diâmetro (15/20/25/…), **leitura inicial**, estado (instalado, em reparação, retirado), **tarifa associada**, localização (rua, coordenadas), `active`. Código `CTD-####`. |
| `Tariff` (Tarifa) | `tariffs` | `category`/preço | Nome (Doméstico, Comercial, …), **estrutura por escalões**, taxa de disponibilidade (fixa), taxa de saneamento, validade. |
| `TariffTier` (Escalão) | `tariff_tiers` | `sale_items`/preço | Limite inferior/superior (m³), **preço por m³** do escalão, percentagem do IVA aplicável. |
| `MeterReading` (Leitura) | `meter_readings` | `sale` | Leitura actual, leitura anterior, **consumo m³** (actual − anterior, com tratamento de volta/rollover), data da leitura, método (física/estimada/auto), leitor, estado, `isEstimated`, `isBackward`. |
| `WaterInvoice` (Factura) | `water_invoices` | `sales` | Série/número/hash (chain AT + QR), tipo documental (FT/VD/RC/NC/ND), **consumo facturado**, valor da água por escalão, taxa de saneamento, taxa fixa (disponibilidade), **subtotal, IVA, total**, estado (EMITIDA/PAGO/PAGO_PARCIAL/ANULADA), cliente, contador, período (mês competência). |
| `InvoiceItem` (Linha de factura) | `invoice_items` | `sale_items` | Escalão aplicado, m³, preço/m³, base tributável, taxa IVA, subtotal. |
| `Payment` (Cobrança) | `payments` | `payments` | Valor, método (DINHEIRO, MPESA, EMOLA, MKESH, POS, DEBITO, TRANSFERENCIA, CREDITO, CHEQUE), referência (terminal), alocação a facturas, `payment_allocation`. |
| `PaymentAllocation` | `payment_allocations` | `payment_allocation` | Ligar pagamento → factura. |
| `ConsumerAccountEntry` (Ledger) | `consumer_account_entries` | `customer_account_entries` | Eventos imutáveis de conta corrente (venda a crédito, anulação, pagamento, recibo sem factura, reconciliação, ajuste). Invariante `SUM(entries) == consumers.balance`. |
| `ReadingZone` (Zona) | `reading_zones` | `branches` | Zona de leitura / posto de atendimento / filial. |
| `Material` (Artigo de rede) | `materials` | `products` | Tubos, contadores, válvulas, registos. |
| `StockZone` / `StockMaterial` | `stock_zones` / `stock_materials` | `stock_branch` / `stock_warehouse` | Existências por zona/armazém com `@Version`. |
| `Warehouse` (Armazém) | `warehouses` | `warehouses` | Depósito central de materiais. |
| `MaterialTransfer` | `material_transfers` | `warehouse_transfers` / `transfers` | Guia de transferência (PENDING → IN_TRANSIT → COMPLETED / CANCELLED). |
| `Purchase` / `PurchaseItem` | `purchases` / `purchase_items` | `purchases` | Factura de compra de materiais, CMP (custo médio ponderado). |
| `Supplier` | `suppliers` | `suppliers` | Fornecedor de materiais/equipamento. |
| `Expense` | `expenses` | `expenses` | Despesa operacional da concessionária. |
| `CashSession` / `CashMovement` | `cash_sessions` / `cash_movements` | idem | Sessão de caixa, fundo de maneio, sangrias/reforços, fecho cego + **Fita Z**. |
| `AppConfig` | `app_config` | idem | Parâmetros da concessionária (NUIT, série default, taxas, IVA, tarifa default). |
| `User` / `Role` / `Permission` | `users` / `roles` / `role_permissions` | idem | RBAC `PAGE:ACTION` |
| `AuditLog` | `audit_logs` | idem | Logs de auditoria |
| `SystemBackup` | `system_backups` | idem | Cópias (mysqldump + checksum SHA-256) |
| `SeriesCounter` | `series_counters` | idem | Numeração fiscal atómica `SELECT … FOR UPDATE` |
| `FiscalDocumentType`/`OperationKind`/`SaleState` | enums | idem/adaptados | Estados e tipos (ver 6.2) |

### 6.2 Enums / estados (adaptados, mesmos mecanismos)

| Enum SGV | Enum SFA |
|---|---|
| `SaleState` (EMITIDA, PAGO, PAGO_PARCIAL*, ANULADA, COTACAO_ABERTA, COTACAO_PAGA, ENCOMENDA_ABERTA) | `InvoiceState` (EMITIDA, PAGO, PAGO_PARCIAL, ANULADA, ESTIMADA, RECTIFICADA) |
| `DocumentType` (VENDA, COTACAO, ENCOMENDA, FACTURA, RECIBO, NC, ND) | `DocumentType` (CONSUMO, FACTURA, RECIBO, NC, ND, RECTIFICACAO) |
| `PaymentMethod` (DINHEIRO, MPESA, EMOLA, MKESH, POS, DEBITO, TRANSFERENCIA, CREDITO, CHEQUE, MULTICAIXA) | **idêntico** (com `movesCashDrawer()`: só DINHEIRO move a gaveta) |
| `OperationKind` (SALE, QUOTE, REFUND, TRANSFER, OTHER) | `OperationKind` (INVOICE, RECTIFICATION, REFUND, TRANSFER, OTHER) |
| `CashSession` (OPEN, CLOSED) | idem |
| `MaterialTransfer` (PENDING, IN_TRANSIT, COMPLETED, CANCELLED) | idem |
| `ReadingMethod` (novo) | FISICA, ESTIMADA, AUTO, PREVISTA |

> ⚠️ **Herança importante:** o SFA deve resolver-na-nascença os bugs conhecidos do SGV (§3 da
> `AUDITORIA_SGV.md`) — por exemplo, **`PAGO_PARCIAL` deve existir no enum** (não como string solta),
> **venda/factura a crédito não nasce `PAGO`**, e **os meios electrónicos não movem a gaveta física**.

---

## 7. Regras de Negócio

> Todas as regras transaccionais e de segurança do SGV são herdadas. Adicionalmente, o SFA introduz
> **regras de utilidade de água** que substituem as regras de stock/produção.

### 7.1 Tarifação por escalões (regra central)

- Cada contador está associado a uma **tarifa** (`tariffs`).
- Uma tarifa é composta por **N escalões** (`tariff_tiers`), ordenados por limite superior de consumo (m³).
- O **consumo facturado** é a soma do produto de cada fracção de m³ pelo seu preço de escalão:

```
Cobrança da água = Σ ( m³_do_escalão_i × preço_m³_i )
```

- **Tarifa de disponibilidade (fixa):** aplicada sempre, independentemente do consumo (comparticipação
  do serviço). Configurável por tarifa.
- **Taxa de saneamento:** aplicada como **percentagem da água facturada** ou **por m³** (configurável).
- **Consumo mínimo facturável:** se o consumo calculado for inferior ao mínimo da tarifa, cobra-se o mínimo.

**Exemplo — Doméstico (base BIMF-1, Moçambique):**

| Escalão | Limite | Preço/m³ (exemplo) |
|---|---|---|
| 1º | 0–10 m³ | 10,00 MT |
| 2º | 11–20 m³ | 18,00 MT |
| 3º | 21–40 m³ | 28,00 MT |
| 4º | 41+ m³ | 42,00 MT |

Consumo = **25 m³** → `10×10 + 10×18 + 5×28 = 100 + 180 + 140 = 420,00 MT` (água) + fixa + saneamento + IVA.

> O **IVA** é configurável no `app_config`; em Moçambique a água pode estar **isenta ou na taxa reduzida
> conforme o CIVA** — modelar como campo por tarifa/tipo (`tax_rate`), com **isento M00..M18** para SAF-T.

### 7.2 Ciclo de leitura e cálculo do consumo

1. **Leitura anterior** = último valor registado do contador (última leitura física aceite — ou leitura de
   instalação se não houver).
2. **Leitura actual** = valor lido pelo leitor (física, estimada, automática).
3. **Consumo bruto** = `leitura_actual − leitura_anterior`.
   - **`consumo < 0` (contador voltou):** recusar leitura como consumo negativo; lançar alerta e propor
     **leitura estimada** (média histórica do consumidor) ou **auto-satisfação via base de dados** (rollover
     do contador com reinício automático) — conforme configuração.
   - **`consumo` muito elevado (pico/registo suspeito):** comparação com média do consumidor; acima de
     `max_consumption_deviation` (ex.: 5× média) exigir **confirmação manual** ou marcar para **revisão**.
   - **Sem leitura física:** gerar **factura estimada** (`isEstimated=true`) quando a leitura anterior
     tem mais de X dias, sinalizada no relatório de **leituras estimadas**.
4. **Rectificação de leitura:** criar uma **Nota de Rectificação (RECTIFICACAO)** (equivalente à NC do SGV)
   que refaz o cálculo e ajusta a conta corrente e o caixa quando aplicável — **simétrico** à venda original.

### 7.3 Facturação (emissão)

- Documento gerado com **numeração atómica** via `series_counters` (`SELECT … FOR UPDATE`) — igual ao SGV.
- **Cadeia de hashes AT** por `(zona, série, hashControl)` com **QR** no documento (o SGV falhou nisto —
  o SFA deve implementar correctamente: anterior por `WHERE zone=? AND series=? AND hash_control < ?`).
- **Estados:** `EMITIDA` (a crédito) → `PAGO_PARCIAL` (pagamento parcial) → `PAGO` (total);
  `ANULADA` apenas por operador com permissão e com **estorno simétrico** (stock/contador, caixa p/ cash,
  conta corrente p/ crédito).
- **Tipos de doc:** `FACTURA` (consumo debitado), `RECIBO` (comprovativo de pagamento, série própria RB),
  `NC` (anulação), `ND` (débito extra/penalidade), `RECTIFICACAO`.

### 7.4 Contas correntes / dívidas (idêntico ao SGV, corrigido)

- **Ledger** `consumer_account_entries` é o **ponto único** de escrita de `consumers.balance`; invariante
  `SUM(entries) == balance` (estrutural).
- Factura a crédito → entrada **CRED** (+); pagamento → **PAY** (−); NC/rectificação → **CREDIT_NOTE** (−);
  recibo sem factura → **RECEIPT_WITHOUT_SALE** (com movimento de caixa se numerário); reconciliação e
  ajustes manuais registados.
- **Só** facturas (`FACTURA/CONSUMO`) podem ser cobradas; cotações/pre-leituras não.
- Extrato do consumidor = leitura do ledger (não reconstrução a partir das facturas).

### 7.5 Tesouraria / Caixa (idêntico ao SGV, corrigido)

- **Só DINHEIRO move a gaveta** (`movesCashDrawer()`). M-Pesa / e-Mola / mKesh / POS / transferência /
  débito **não** movem o caixa físico (corrige o bug SGV-003).
- **Troco persistido**: `received` e `change` gravados na factura/pagamento, tal como no PDV.
- Fecho cego + **Fita Z** separando **esperado por método** (numerário vs electrónico); diferença
  (quebra/sobra) guardada por sessão.
- Anulações/NCs fazem **estorno simétrico e atómico** (sem `catch` engolido).

### 7.6 Segurança / RBAC

- Perfil `admin` / `gestor` / `leitor` / `caixa`.
- **Service-level** `requirePermission` em Todas as escritas (não só na UI):
  `FACTURACAO:CREATE/DELETE`, `COBRANCA:CREATE`, `LEITURAS:CREATE/APPROVE`, `MATERIAIS:CREATE`,
  `CONFIG:CREATE`, `RELATORIOS:VIEW`.
- `forceChangePassword` **efectivo** no login; `admin` desactivado **não é reactivado** no arranque;
  seeds demo marcados com `demoFlag`/`isTraining`.
- Licença com **binding por machineId** e bloqueio em expirado.

### 7.7 Outras regras de domínio

- **Água não facturada (NRW):** consumo produzido vs facturado → relatório de perdas (traduz a noção de
  "stock quebrado" do SGV para utilidade).
- **Materiais de rede:** stock com `@Version`; compras entram no **armazém**; transferência
  armazém→zona é **etapa visível e obrigatória** para poder instalar um contador (corrige o dual-stock do SGV
  exigindo ponte explícita).
- **Cobrança parcial/antecipada:** validação de limite de crédito com **lock**; sem duplo-pagamento
  (concorrência resolvida com `@Version` + retry).

---

## 8. Fluxo de Operações e de Dados

### 8.1 Fluxo principal — Emissão de Factura (equivalente ao PDV)

```
[Login] → [Abrir Sessão de Caixa] → [Escolher Cliente/Contador] → [Entrar leitura actual]
  → [Sistema calcula consumo (actual − anterior)] → [Valida regras: negativo? pico? estimada?]
  → [Aplica tarifa por escalão + fixa + saneamento] → [Subtotal + IVA]
  → [Valor recebido + troco (se numerário)] → [Nova Factura] (numeração + hash + QR)
  → [Baixa de consumo / registo no ledger] → [Imprimir Factura (A4/80mm/58mm)]
  → [Movimento de caixa IN se DINHEIRO]
```

**Dados afectados (numa única transação):** `water_invoices`, `invoice_items`, `meter_readings`,
`consumer_account_entries`, `consumers.balance`, `cash_movements` (+ `cas_sessions`), `series_counters`
(contador), `audit_logs`. O PDF/impressão fica fora da transação.

### 8.2 Fluxo de cobrança (equivalente ao Pagamento)

```
[Recebimentos] → [Factura pendente (só FACTURA/CONSUMO)] → [Valor]
  → [Valida lock + limite] → [Cria Payment + Allocation + ledger PAY −]
  → [Movimento de caixa IN só se DINHEIRO] → [Estado EMITIDA→PAGO_PARCIAL→PAGO]
  → [Recibo com série RB própria + hash] → [Imprimir]
```

### 8.3 Fluxo de reconciliação / rectificação

```
[Histórico de Facturas] → [Emitir NC/Rectificação]
  → [Reverte a factura original: stock(consumo), caixa OUT p/ cash, ledger CREDIT_NOTE − p/ crédito]
  → [Gera documento RECTIFICACAO com hash + QR] → [Auditoria]
```

### 8.4 Fluxo de transferência de materiais/contadores

```
[Armazém → Zona] → [Nova Guia TWA] → [Estado PENDING]
  → [Expedir → IN_TRANSIT] → [Receber na Zona → COMPLETED (re-valida stock + movimenta)]
  → [Kardex de materiais]
```

### 8.5 Fluxo de fecho de caixa (Fita Z)

```
[Abrir com fundo de maneio] → [Vendas/cobranças/despesas/sangrias/reforços]
  → [Contagem] → [Fecho cego] → [Fita Z por método] → [Diferença (quebra/sobra) persistida]
```

### 8.6 Fluxo de dados no arranque

- Splash futurista (idêntico) → Boot Spring (Flyway cria o esquema `sfa`) → Verificação de **licença**
  → Login → Dashboard.

---

## 9. Páginas / Janelas

> Cada janela mantém o **layout, dimensões, ícones, cores e componentes** do SGV. Descrevemos o **conteúdo**
> de domínio que muda.

### 9.1 Login (`login.fxml`) — idêntico, só textos

- Card gradiente azul, raio 16, `dropshadow(rgba(0,0,0,0.6), 25)`.
- Logo: **`💧 SFA`** + subtítulo **"SISTEMA DE FACTURAÇÃO DE ÁGUA & SANEAMENTO"**.
- Campos `👤 Login` / `🔒 Palavra-passe` (fundo `#1E293B`, texto `#FFFFFF`, prompt `#64748B`).
- Botão `ENTRAR NO SISTEMA` (gradiente `#2563EB→#1D4ED8`).
- Rodapé: `🔒 Certificado Fiscal AT` · `🇲🇿 Moçambique (CIVA)`. (Em vez de `CIVA 16%`, usar a taxa
  aplicável à água configurada, ou manter genérico.)
- Rodapé global: `© 2026 SFA Enterprise • Sistema de Facturação de Água 100% Offline`.

### 9.2 Dashboard / Painel (`dashboard.fxml`) — mesma grelha

- **4 KPI cards** (ícone emoji 24px, `kpi-icon-*-background`):
  | SGV | SFA |
  |---|---|
  | `💰 Vendas de Hoje` (blue) | `💰 Facturação de Hoje` (blue) |
  | `📦 Artigos em Rutura` (purple) | `🧾 Contadores por Ler` (purple) |
  | `👥 Contas a Receber` (green) | `👥 Dívidas por Cobrar` (green) |
  | `💵 Caixa / Gaveta` (orange) | `💵 Caixa / Cobranças de Hoje` (orange) |
- **Gráficos:** `LineChart` "Facturação Diária — Últimos 7 dias" e `PieChart` "Meios de Pagamento — Hoje".
- **Quick actions:** `Fazer Factura` (secundário) · `Nova Leitura (Emissão)` (primário).
- **Sub-bar de título:** `Painel / Visão geral da facturação, consumos e tesouraria`.

### 9.3 Emissão / Leitura (`reading_form.fxml` — clone visual do PDV)

- **Top bar:** `💧 Facturação` + `Ponto de Emissão & Facturação Rápida` + `⏸ Suspender (F6)`,
  `▶ Recuperar (F7)`, Combo `documentTypeCombo` (CONSUMO/FACTURA), `Total: 0.00 MT`.
- **Coluna esquerda:**
  - Linha 1: `Zona` (combo), `Tipo de Tarifa` (toggle `Doméstico/Grosso`), `Consumidor Diverso (F8)`,
    `Cliente` (combo).
  - Linha 2 (pesquisa com debounce 300ms): combo `Catálogo / Tipo de Consumo` + `Código / Nome do
    Contador (F2)` + `Qtd. (F4)` + `+ Adicionar (Enter)`.
  - Tabela `itemsTable`: colunas **`Contador`, `Leitura Anterior`, `Leitura Actual`, `Consumo (m³)`,
    `Total`** + coluna acção.
- **Coluna direita:**
  - `FORMA DE PAGAMENTO` (`Método`, `Moeda`).
  - `RECEBIMENTO & TROCO` (`Valor Entregue`, `Troco`, botões `Exato/+50/+100/+500/+1000`).
  - **Teclado numérico touch 48×48dp** (0–9, `.`, `C`).
  - Resumo totais (`Subtotal`, `IVA`, `TOTAL`) com `#10B981`.
  - Botões: `Limpar / Cancelar (ESC)`, `FINALIZAR FACTURA (F10)`.
- **Footer:** Atalhos Rápidos (F2/F4/F6/F7/F8/F10/ESC).

### 9.4 Histórico de Facturas & Consumos

- Toolbar: `+ Nova` (azul), `Imprimir` (cinza), `Ver Detalhes` (verde), `Rectificar` (amarelo),
  `Nota de Crédito` (amarelo), `Anular` (vermelho), `↻` (verde).
- KPIs: `Facturação Hoje`, `Facturação Semana`, `Facturação Mês`, `Ticket Médio` (= valor médio/factura).
- Tabela: `Documento`, `Série`, `Cliente`, `Total`, `Estado`, `Data`, `Pagamento`.
- Filtros: search, `Data`, `Tipo` (CONSUMO/FACTURA/RECIBO/NC/ND/RECTIFICACAO), `Estado`
  (EMITIDA/PAGO/ANULADA/…), `Limpar`.

### 9.5 Clientes & Consumidores (`customer_form.fxml`)

- Toolbar: `+ Novo Cliente`, `Editar`, `Ver Detalhes`, `Liquidar Dívida`, `Reconciliar Crédito`,
  `Eliminar`, `Actualizar`, `Exportar Excel`. Campos: `Código`, `Nome`, `Tipo` (Doméstico/Comercial/…),
  `Contacto`, `Saldo`.
- KPI: `Total Clientes`, `Com contador`, `Por ler`, `Novos 30d`.
- Detalhes (DetailDialog): identificação + **contador associado** + estado de dívida + histórico de leituras
  e facturas.
- Validações: NUIT (9/10 dígitos módulo 11); código único `CLI-####` (sem colisão após eliminações).

### 9.6 Tarifas & Escalões (catálogo)

- **Painel esquerdo:** `Categorias/Tipos de Consumo` com `+ Nova` / `Eliminar`.
- **Painel direito:** `Tarifas` (Abreviação, Descrição) e sub-editor de **escalões** (limite inferior,
  superior, preço/m³, taxa IVA) — equivalência directa ao editor de unidades do SGV.
- KPI: `Categorias`, `Escalões`, `Tarifas activas`, `Tipos com IVA`.

### 9.7 Contadores & Tarifas (equivalente a Artigos)

- Tabela: `Código`, `Nº Série / Contador`, `Diâmetro`, `Zona`, `Tarifa`, `Última Leitura`, `Estado`.
- KPI: `Total Contadores`, `Instalados`, `Por Ler`, `Em Reparação`.
- Formulário: dados do contador, zona, tarifa, leitura inicial, estado, localização.

### 9.8 Consumos / Leituras (equivalentes a Stock/Inventário)

- Tabela: `Contador`, `Zona`, `Leitura Anterior`, `Leitura Actual`, `Consumo (m³)`, `Estado`
  (`Lida`, `Estimada`, `Suspeita`, `Não lida`).
- Coluna consumo usa coloração (0/mín → vermelho `#FEE2E2`, baixo → amarelo `#FEF3C7`, ok → verde `#10B981`).
- Acções: `Registar Leitura`, `Ver Detalhes`, `Eliminar`, `↻`.

### 9.9 Sessão & Fecho de Caixa (`cash_session.fxml`) — idêntico

- Abertura com **fundo de maneio**; movimentos (sangria/reforço); fecho cego com **Fita Z**, separando
  numerário vs electrónico; diferença persistida; histórico por utilizador/estado.

### 9.10 Recebimentos (`payment_form.fxml`) — idêntico

- Lista de facturas **pendentes** (só FACTURA/CONSUMO, não pre-facturas); alocação; método; referência;
  recibo com série RB; validação de lock/limite; movimento de caixa só DINHEIRO.

### 9.11 Resumo Financeiro (`financeiroPane`)

- `Total Facturado`, `Recebido`, `Pendente`, `Cobranças` — com o **universo reportável** partilhado
  (exclui anuladas, estimadas em modo teste, pré-facturas).

### 9.12 Rede / Materiais / Transferências

- `Rede ▾` → `Armazéns`, `Fornecedores`, `Compras`, `Stock de Materiais`, `Transferências` — todos
  **clone do SGV** (mesmas tabelas e acções, com materiais em vez de produtos).

### 9.13 Relatórios — ver §11.

### 9.14 Configurações & Sistema — idêntico

- `⚙️ Parâmetros` (dados da concessionária + NUIT + tarifa default + séries + taxas), `👤 Utilizadores`,
  `💾 Backups` (dump real + checksum + restauro + agendamento respeitado), `🎓 Treinamento`,
  `🔑 Licenciamento` (binding por machineId + bloqueio), `🔒 Segurança` (alterar password + política).

### 9.15 Auditoria & Segurança — idêntico (Tabs: Todos/Erros/Segurança/Utilizadores/Sistema)

---

## 10. Fluxo de Interacções

**Teclado / atalhos** (iguais ao PDV):

| Tecla | Acção |
|---|---|
| `F2` | Foco na pesquisa de contador |
| `F4` | Foco na quantidade (m³) |
| `F6` | Suspender leitura em curso |
| `F7` | Recuperar leitura suspensa |
| `F8` | Alterar consumidor (diverso/pesquisado) |
| `F10` | Finalizar / emitir factura |
| `ESC` | Cancelar / limpar leitura em curso |
| `Enter` (na pesquisa) | Adicionar leitura à factura |

**Eventos:**

| Evento | Comportamento |
|---|---|
| Digitação na pesquisa (debounce 300ms) | Filtra resultados localmente (catálogo de contadores) |
| Scan de código de barras do contador | Auto-deteção por velocidade → adiciona linha |
| `Valor Entregue` | Recalcula troco em tempo real (não descartado) |
| Botões `Exato/+50/+100/+500/+1000` | Fixam o valor recebido e recalcular troco |
| `FINALIZAR` | Valida → transação única → PDF → baixa consumo/ledger → caixa IN (se DINHEIRO) |
| Duplo-clique numa linha | Abre `DetailDialog` de factura/contador/cliente |
| `Anular` | Diálogo de perigo com motivo obrigatório → estorno simétrico |
| `Rectificar` | Gera RECTIFICACAO → recalcula consumo → ajusta ledger ≥0 |
| `Nota de Crédito` | Gera NC (simétrica) |
| Selecção de estado na Fita Z | Reconcilia por método |

**Estados da factura:** `EMITIDA` → (pagamento) → `PAGO_PARCIAL` → `PAGO`; `ANULADA`; `RECTIFICADA`.

---

## 11. Mapas & Relatórios

| # | SGV | SFA | Conteúdo |
|---|---|---|---|
| 1 | Mapa de Vendas | **Mapa de Facturação** | Consumo e valor facturado por período/zona/tipo |
| 2 | Mais Vendidos | **Maiores Consumos** | Top consumidores/contadores por m³ e valor |
| 3 | Apuramento de IVA | **Apuramento de IVA** | Base tributável/isenta **por linha** (não por documento); CIVA M00..M18; NC abate |
| 4 | Contas Correntes | **Contas Correntes (devedores)** | Aging de dívidas apenas FACTURA/CONSUMO |
| 5 | Kardex | **Consumo por Contador** | Histórico de leituras (anterior/actual/consumo) por contador |
| 6 | Stock por Armazém | **Facturação por Zona** | Agregação por zona de leitura/posto |
| 7 | Transferências | **Perdas / Água Não Facturada (NRW)** | Produção vs facturação (equivalentes a transferências de stock/logística) |
| 8 | Pag. Fornecedores | **Pag. Fornecedores** | idem |
| 9 | — | **Leituras Estimadas** | Contadores sem leitura física a facturar por estimativa |
| 10 | SAF-T MZ | **SAF-T MZ** | Exportação XML com o universo reportável |

**Exportação:** Excel (`ExportUtil`) e PDF (`PDFBox`) em todos os mapas; **paridade** de números entre
dashboard = mapa = apuramento = caixa (usar um único predicado `isReportable`).

---

## 12. Segurança / RBAC / Configuração

- **Modelo idêntico:** `users → user_roles → roles → role_permissions` (`PAGE:ACTION` com wildcards).
- **Perfis default:** `admin`, `gestor`, `leitor` (leituras), `caixa` (cobrança), `fiscal`.
- **Matriz (exemplo):**

| Permissão | ADMIN | GESTOR | LEITOR | CAIXA |
|---|---|---|---|---|
| `FACTURACAO:CREATE` | ✅ | ✅ | ✅ | ✅ |
| `FACTURACAO:DELETE` | ✅ | ✅ | ❌ | ❌ |
| `LEITURAS:CREATE/APPROVE` | ✅ | ✅ | ✅ | ❌ |
| `COBRANCA:CREATE` | ✅ | ✅ | ❌ | ✅ |
| `MATERIAIS:CREATE` | ✅ | ✅ | ❌ | ❌ |
| `CONFIG:CREATE` | ✅ | ❌ | ❌ | ❌ |
| `RELATORIOS:VIEW` | ✅ | ✅ | ❌ | ✅ |

- **Service-level: TODAS as escritas chamam `requirePermission`** (lição do SGV-§17). `VIEW` não executa escrita.
- **Arranque seguro:** seeds apenas em BD vazia; `admin` desactivado permanece; demo com `demoFlag=true`.
- **Configuração:** `app_config` com NUIT, série default, tarifas, taxas (água/ saneamento/ IVA), séries.

---

## 13. Componentes Reutilizáveis

> **Mesmos componentes JavaFX do SGV** — reutilizar sem alterar. Apenas os ícones/textos de domínio mudam.

| Componente | API | Nota |
|---|---|---|
| `SgvDialog` | `info/warning/error/confirm/confirmDanger/confirmAction(toast)/prompt` | Barra accent de 5px, ícone circular 54px, botões estilizados. Igual. |
| `DetailDialog` | `.title().subtitle().statusBadge().section().field().tableSection().show()` | Janela sem decoração, header gradiente azul, ícone 46px, secções com accent bar 4px, tabelas zebra. Igual. |
| `StatusBadge` | `new StatusBadge(text, BadgeType.SUCCESS/DANGER/WARNING/INFO)` | Igual. |
| `SearchBar` / `EmptyState` / `PaginationControl` | Iguals | Iguals. |
| `ToastNotification` | toast de confirmação (auto-fecha ~2.5s) | Igual. |
| `Formatters` | `moneyMT`, `moneyMZN`, `formatNumber`, `applyCurrencyFormatter`, `applyDecimalFormatter` | Igual (moeda MZN/MT). |
| `UiUtils` | `applyHoverElevation`, `applyPressFeedback`, `attachSafe`, `setupDebounce`, `hardenComboBox` | Igual. |
| `FilterPanelBuilder` | critérios de filtro por página | Igual. |
| `ExportUtil` | Excel via POI | Igual. |

**Paleta de estados (coloração de células) — igual:**

```
PAGO / COMPLETED / ACTIVO / CONCLUÍDA   → #D1FAE5 / #065F46
EMITIDA / INFO                          → #DBEAFE / #1E40AF
PENDENTE / EM CURSO / ESTIMADA          → #FEF3C7 / #92400E
ANULADA / CANCELLED / INACTIVO          → #FEE2E2 / #991B1B
ENCOMENDA / RECTIFICACAO                → #EDE9FE / #5B21B6
```

---

## 14. Reutilização do Design System SGV

**Passos para garantir "like the same":**

1. Copiar `src/main/resources/styles/styles.css` e `src/main/resources/css/dashboard.css` **sem alterar**
   (tokens, classes, animações). Adicionar apenas fichas de texto novas se necessário.
2. Copiar os componentes `desktop/*.java` (SgvDialog, DetailDialog, StatusBadge, ToastNotification,
   UiUtils, Formatters, ExportUtil, FilterPanelBuilder, SearchBar, EmptyState, PaginationControl,
   ConfirmDialog, NotificationsCenterDialog, LoadingOverlay, BaseFormController) **sem alterar**.
3. Copiar os FXML **e apenas substituir textos**: `login.fxml`, `dashboard.fxml`, `sale_form.fxml` →
   `reading_form.fxml`, `customer_form.fxml`, `payment_form.fxml`, `cash_session.fxml`, etc.
4. **Único ficheiro com mudança estrutural real:** o **`dashboard.fxml` top bar** — renomear módulos
   (ver §4). **As dimensões (`58px`, `40px`, `42px` cell), as cores, os raios e os ícones não mudam.**
5. Ícones trocados por analogia de domínio (mantendo o **tamanho e a posição**):
   `⚡ → 💧`, `💰 → 💧/💰`, `📦 → 🧾` (contadores), `🥖 → 💧`, `🏗️/Armazém → 🏢`, etc.,
   **sem alterar o emoji size (24px KPI) nem a grelha.**

---

## 15. Glossário

| Termo | Significado |
|---|---|
| **Consumo** | m³ calculado = leitura actual − leitura anterior |
| **Escalão** | Faixa de consumo com um preço por m³ próprio |
| **Tarifa de disponibilidade** | Valor fixo de acesso/serviço, cobrado sempre |
| **Taxa de saneamento** | Cobrança de tratamento de efluentes (percentagem da água ou por m³) |
| **Leitura estimada** | Facturação por estimativa quando não há leitura física |
| **NRW** | Água Não Facturada (perdas físicas + administrativas) |
| **Fita Z** | Fecho de caixa por método de pagamento |
| **Ledger** | Livro de conta corrente (eventos imutáveis + saldo) |
| **SAF-T MZ** | Exportação fiscal oficial em XML |

---

### Checklist de aceitação da "semelhança"

- [ ] Login visualmente idêntico (gradiente azul, raio 16, mesmos campos), mudando só textos/logo.
- [ ] Barra superior com o mesmo layout/altura/gradiente; **só os textos dos módulos mudam**.
- [ ] KPIs com os mesmos emoji, cores de fundo e tamanhos (24px icon / 26–28px número).
- [ ] Tabelas com header cinza `#F8FAFC`/`#F1F5F9`, zebra `#FFFFFF`/`#F8FAFC`, cell 42px.
- [ ] Botões com as mesmas classes (primário/success/secundário/perigo) e sombras.
- [ ] Diálogos (`SgvDialog`, `DetailDialog`) sem alterações de código.
- [ ] PDV/Leitura com numpad 48×48dp, painel de troco, footer de atalhos.
- [ ] Regras de negócio de água (escalões, saneamento, fixa, IVA, estimadas) implementadas.
- [ ] Tesouraria: só DINHEIRO move a gaveta; troco persistido; Fita Z por método.
- [ ] Ledger de conta corrente com invariante `SUM(entries) == balance`.
- [ ] Numeração fiscal atómica + **QR** + cadeia de hashes por (zona, série, hashControl).
- [ ] RBAC por serviço; licença com binding; backup/restauro reais.
- [ ] Universo reportável único (dashboard = mapa = apuramento = caixa).

---
*Especificação elaborada com base na análise do repositório `Junior-Web37/SGV` (design system, regras de
negócio, fluxos, telas e componentes), replicando 1:1 tudo excepto o menu superior (adaptado à água).*
