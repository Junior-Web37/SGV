# 🗺️ PLANO DE MIGRAÇÃO TÉCNICA E MODERNIZAÇÃO ARQUITETURAL

Este documento estabelece a ordem de execução das refatorações estruturais aplicadas ao sistema SGV, com a respetiva estimativa de impacto e análise de quebra de compatibilidade.

---

## 1. MATRIZ DE IMPACTO E RISCO

```
┌─────────┬───────────────────────────────────────────┬──────────────┬────────────────────────┐
│ FASE    │ COMPONENTE / REFATORAÇÃO                  │ IMPACTO      │ COMPATIBILIDADE        │
├─────────┼───────────────────────────────────────────┼──────────────┼────────────────────────┤
│ FASE 1  │ Schema BD & Migrações Flyway (V23..V25)   │ Médio-Alto   │ 100% Retrocompatível   │
│ FASE 2  │ Sequenciador Atómico & Concorrência       │ Alto         │ Sem Quebra de Schema   │
│ FASE 3  │ Sanitização de Logs & Zero-Trust (OWASP)  │ Baixo        │ Transparente           │
│ FASE 4  │ Desacoplamento Transacional de I/O        │ Alto         │ Mesma Assinatura API   │
│ FASE 5  │ Pool Gerenciado de Threads (AsyncConfig)  │ Médio        │ Elimina Memory Leaks   │
│ FASE 6  │ Ficha Técnica (BOM) & Produção            │ Médio        │ Adição Não Disruptiva  │
│ FASE 7  │ Blindagem Financeira & Recebimentos       │ Alto         │ Corrige Saldo Devedor  │
│ FASE 8  │ UI Touchscreen, Numpad & Padrão MZ        │ Médio        │ Melhoria Ergonómica    │
└─────────┴───────────────────────────────────────────┴──────────────┴────────────────────────┘
```

---

## 2. SEQUÊNCIA DE EXECUÇÃO DETALHADA

### FASE 1: Integridade Estrutural da Base de Dados
1. **Migração `V23`:** Criação da tabela `payment_allocation` e inclusão da coluna `last_movement_at` na tabela `products`.
2. **Migração `V24`:** Criação da tabela `product_recipes` com chaves estrangeiras para produto pai e matérias-primas.
3. **Migração `V25`:** Criação da tabela `series_counters` e inclusão da coluna `version` em `stock_branch` e `stock_warehouse`.
4. **Criação de Índices Fiscais:** Indexação de `sales(customer_id)`, `sales(created_at)`, `stock_movements(created_at)` e `products(category_id)`.

### FASE 2: Concorrência e Sequenciação Atómica
1. **Criação da Entidade `SeriesCounter` e Repositório com Lock Pessimista:**
   * Anotação `@Lock(LockModeType.PESSIMISTIC_WRITE)` em `SeriesCounterRepository.findForUpdate(...)`.
2. **Refatoração do `SaleNumberingService`:**
   * Garantia de alocação atómica e thread-safe para o número do documento fiscal e `hashControl`.

### FASE 3: Desacoplamento Transacional e Performance de I/O
1. **Divisão de `SaleService.processAndSave`:**
   * `persistSaleTransaction`: Validação, hashes e gravação no banco dentro de `@Transactional` (< 30ms).
   * Geração de PDF e impressão térmica executadas fora do lock de banco de dados.

### FASE 4: Blindagem Financeira e Eliminação de Pagamentos Duplicados
1. **Refatoração de `PaymentService` & `PaymentFormController`:**
   * Inclusão de filtro de faturas pendentes (`total - paidAmount > 0.01 MT`).
   * Validação em tempo real: bloqueio de valores que excedam o saldo devedor.
   * Abate automático na conta corrente do cliente (`Customer.balanceAmount`) e lançamento no caixa.
2. **Refatoração de `SupplierPaymentService`:**
   * Listagem restrita a compras não quitadas e bloqueio de pagamentos excedentes.

### FASE 5: Logística de Armazéns e Ficha Técnica
1. **Ficha Técnica na Panificação (`ProductRecipe`):**
   * Abate automático de ingredientes proporcionais à quantidade produzida.
2. **Painel de Guias de Transferência:**
   * Tabela com filtros de armazém, filial e estado (Pendente, Em Trânsito, Concluído) e confirmação de receção.

### FASE 6: Interface Gráfica, Numpad Touch e Padrão Moçambicano
1. **Teclado Numérico Touchscreen Permanente:**
   * Botões de tamanho mínimo 48x48dp com contraste WCAG AA.
2. **Reorganização dos Módulos do Sistema:**
   * Painel Geral, Vendas & Facturação, Stock & Artigos, Caixa & Tesouraria, Armazém & Compras, Mapas & Relatórios e Configurações.
