# 🇲🇿 DIAGNÓSTICO ESTRUTURAL E ARQUITETURAL COMPLETO — SGV (SISTEMA DE GESTÃO DE VENDAS)

**Data:** 11 de Agosto de 2026  
**Responsável Técnico:** Principal Staff Engineer & Auditor de Sistemas de Alta Disponibilidade  
**Padrão Regulatório:** Legislação Fiscal de Moçambique (AT / CIVA - Decreto 7/2024 / SAF-T MZ)

---

## 1. INVENTÁRIO TOTAL DO REPOSITÓRIO

### 1.1 Visão Geral dos Artefactos
* **Linguagem & Plataforma:** Java 21 (LTS) com JavaFX 21 + Spring Boot 3.1.4 (Modo Desktop / Standalone Offline).
* **Motor de Persistência:** Spring Data JPA / Hibernate 6 + MariaDB / MySQL local (XAMPP porta 3306) + Pool HikariCP.
* **Controlo de Versão de Base de Dados:** Flyway com **25 Migrações SQL** (`V1__baseline.sql` a `V25__create_series_counters_and_optimistic_locks.sql`).
* **Segurança Criptográfica:** BCrypt para palavras-passe e SHA-256 encadeado (*Previous Hash Chaining*) para certificação fiscal.
* **Geração de Documentos:** Apache PDFBox (A4 e Talão Térmico 80mm/58mm com código QR) + Apache POI (Mapas Excel `.xlsx`).

---

## 2. DIAGNÓSTICO DOS SISTEMAS CRÍTICOS & DECISÕES TOMADAS

### 2.1 Facturação, Recebimentos e a Eliminação do "Pagamento Duplo Fantasma"
* **Diagnóstico da Falha:** A consulta `SaleRepository.findRecentNonCancelled` retornava todas as vendas não canceladas (inclusive as já pagas a 100% no balcão). Ao registar pagamento, o sistema somava o valor cegamente em `paidAmount`, duplicando a receita contabilística e emitindo recibos fictícios sobre documentos já liquidados.
* **Decisão & Resolução:**
  1. Criada a consulta estrita `SaleRepository.findPendingSales()` que filtra apenas documentos onde `(total - paidAmount) > 0.01 MT`.
  2. Implementado bloqueio visual e transacional: se o operador digitar um valor maior que o saldo devedor, a gravação é rejeitada.
  3. Ao liquidar, o valor abate imediatamente o saldo devedor na ficha do cliente (`Customer.balanceAmount`), lança entrada na sessão de caixa (`CashSession`) e gera registo na tabela `payment_allocation`.

### 2.2 Concorrência Atómica e Encadeamento Fiscal (Padrão AT Moçambique)
* **Diagnóstico da Falha:** O sequenciamento numérico de facturas usava `SELECT MAX(document_number)` sem bloqueio de concorrência. Sob múltiplos terminais simultâneos, isso gerava colisões de numeração e quebrava a assinatura digital SHA-256.
* **Decisão & Resolução:**
  1. Criada a tabela `series_counters` com chave composta `(branch_id, series, document_type)`.
  2. Criado `SeriesCounterRepository` com lock pessimista `@Lock(LockModeType.PESSIMISTIC_WRITE)`.
  3. O `SaleNumberingService` garante alocação sequencial rigorosa e atómica.
  4. O `FiscalService` implementa o encadeamento formal (*Previous Hash Chaining*): `Data;DataSistema;NumeroDoc;Total;HashAnterior`.

### 2.3 Bloqueio Otimista de Inventário (`@Version`)
* **Diagnóstico da Falha:** Se dois caixas vendessem o mesmo artigo com 1 unidade restante ao mesmo tempo, ambos liam `stock = 1`, subtraíam 1 e gravavam, gerando estoque físico negativo sem alerta.
* **Decisão & Resolução:**
  * Adicionada a coluna `version` em `stock_branch` e `stock_warehouse` (Migração `V25`) e mapeado `@Version private Long version = 0L;` nas entidades JPA.

### 2.4 Desacoplamento Transacional de I/O
* **Diagnóstico da Falha:** A geração de PDFs e a comunicação de impressão ocorriam dentro do bloco `@Transactional`, retendo conexões do pool de banco de dados e bloqueando linhas durante a escrita em disco.
* **Decisão & Resolução:**
  * Separado em `SaleService.persistSaleTransaction` (banco de dados rápido, < 30ms) e `SaleService.processAndSave` (renderização e PDF fora da transação).

### 2.5 Gestão de Armazéns, Ficha Técnica (BOM) e Guias de Transporte
* **Diagnóstico da Falha:** As transferências entre armazém e lojas não tinham painel visual de acompanhamento com filtros. Na padaria/produção, as ordens de fabrico não abatiam matérias-primas.
* **Decisão & Resolução:**
  1. Criada a tabela `product_recipes` (Migração `V24`) para fichas técnicas/BOM.
  2. Implementado abate automático de ingredientes em `StockBranchService.consumeIngredientsForProduction`.
  3. Criado o painel visual de **Guias de Transferência** no Dashboard, com filtros por armazém, filial, estado (Pendente, Em Trânsito, Concluído) e botão de **Confirmar Receção** na loja.

---

## 3. ARQUITETURA DE MÓDULOS (PADRÃO COMERCIAL MOÇAMBICANO)

| Módulo Principal | Sub-Módulos & Telas | Finalidade Operacional |
| :--- | :--- | :--- |
| **⌂ Painel Geral** | Resumo de KPIs, Gráficos de Vendas, Estado do Caixa | Visão em tempo real da saúde da empresa |
| **Vendas & Facturação** | ⚡ PDV (Venda Rápida), 📄 Facturas Emitidas, 👥 Clientes & NUIT | Emissão fiscal com troco, atalhos F2-F10 e contas correntes |
| **Stock & Artigos** | 🏷️ Artigos & Preços, 📊 Inventário em Loja, 📑 Famílias, 🥖 Fabrico & Padaria | Gestão de catálogo, margens de lucro e produção com receitas |
| **Caixa & Tesouraria** | 💼 Sessão de Caixa (Fundo de Maneio, Fita Z), 💳 Recebimentos, 📉 Despesas | Controlo de dinheiro, sangrias, reforços e fecho cego térmico |
| **Armazém & Compras** | 🏢 Armazéns Centrais, 📦 Stock Armazém, 📥 Facturas Compra, 🚛 Fornecedores, 🔁 Guias de Transferência | Abastecimento central, logística entre lojas e custo médio ponderado |
| **Mapas & Relatórios** | 📊 Mapas de Vendas, 🏛️ Apuramento IVA (16%), 📋 SAF-T MZ, 📦 Kardex Stock, 🛡️ Auditoria | Prestação de contas fiscal e relatórios contabilísticos |
| **Configurações** | ⚙️ Dados da Empresa & NUIT, 👤 Utilizadores & Acessos (RBAC), 💾 Backups, 🔑 Licença, 🎓 Modo Treino | Parametrização, segurança, cópias de segurança e formação |

---

## 4. CONFORMIDADE FISCAL & MERCADO DE MOÇAMBIQUE

1. **NUIT:** Validação algorítmica estrita com algoritmo de ponderação por pesos de 9 dígitos.
2. **IVA Moçambique (16%):** Alinhado ao Decreto 7/2024 e catálogo oficial de motivos de isenção CIVA (M01 a M18).
3. **Moeda:** Metical moçambicano (**MT / MZN**), formatado com duas casas decimais e separadores padrão.
4. **Métodos de Pagamento:** Suporte nativo a **M-Pesa**, **e-Mola**, **mKesh**, Numerário (Dinheiro), Cartão POS, Transferência Bancária e Cheque.
5. **Fita Z:** Impressão de fecho de turno com detalhamento de numerário, quebras/sobras e assinaturas.
