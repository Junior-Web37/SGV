# AUDITORIA COMPLETA DO SISTEMA SGV

## 1. Mapa completo do projecto

### 1.1 Visão geral
- Nome do projecto: SGV Desktop
- Tipo: Sistema de gestão comercial desktop
- Stack principal: Java 21 + JavaFX 21 + Spring Boot 3.5 + JPA/Hibernate + MariaDB/MySQL + Flyway
- Padrão arquitetural dominante: Layered Architecture com MVC/Controller-driven desktop UI
- Execução: JavaFX desktop bootstrapping Spring em thread de background

### 1.2 Estrutura de pastas
- `src/main/java/com/sgv/desktop` — controladores JavaFX, telas, navegação, UI
- `src/main/java/com/sgv/service` — regras de negócio, serviços, orquestração
- `src/main/java/com/sgv/repository` — repositories Spring Data JPA
- `src/main/java/com/sgv/entity` — entidades JPA / modelos de domínio
- `src/main/java/com/sgv/config` — configuração Spring, cache, segurança, bootstrap
- `src/main/resources/fxml` — telas do frontend
- `src/main/resources/css` / `styles` — estilos da interface
- `src/main/resources/db/migration` — migrações Flyway
- `src/test/java` — testes unitários/integrados

### 1.3 Lista de ficheiros-chave (mapa funcional)

#### Java desktop UI
- `MainApp.java`
- `LoginController.java`
- `DashboardController.java`
- `DashboardCrudManager.java`
- `DashboardKpiManager.java`
- `DashboardNavigationManager.java`
- `SaleFormController.java`
- `PurchaseFormController.java`
- `ProductFormController.java`
- `CustomerFormController.java`
- `SupplierFormController.java`
- `ExpenseFormController.java`
- `CashSessionController.java`
- `CashMovementFormController.java`
- `CloseSessionFormController.java`
- `OpenSessionFormController.java`
- `BranchFormController.java`
- `CategoryFormController.java`
- `MetricUnitFormController.java`
- `WarehouseFormController.java`
- `WarehouseTransferFormController.java`
- `ProductionOrderFormController.java`
- `ReportsController.java`
- `SistemaModuleController.java`
- `PaymentFormController.java`
- `StockAdjustFormController.java`
- `StockNewFormController.java`
- `UserFormController.java`
- `BaseFormController.java`
- `DetailDialog.java`
- `DocumentPreviewDialog.java`
- `UiUtils.java`

#### Configuração e bootstrap
- `SgvApplication.java`
- `SecurityConfig.java`
- `CacheConfig.java`
- `DataInitializer.java`
- `DbTriggerConfig.java`

#### Serviços
- `AppConfigService.java`
- `AtSubmissionService.java`
- `AuditLogService.java`
- `BackupService.java`
- `CashSessionService.java`
- `ComplianceDashboardService.java`
- `DailyReconciliationService.java`
- `DashboardService.java`
- `DesktopAuthService.java`
- `FilterPresetService.java`
- `FiscalService.java`
- `LicenseService.java`
- `MetricUnitService.java`
- `PrintHtmlService.java`
- `ReceiptDeliveryService.java`
- `ReportService.java`
- `SafTExportService.java`
- `SaleDocumentService.java`
- `SaleNumberingService.java`
- `SaleService.java`
- `SeriesManagementService.java`
- `StockBranchService.java`
- `SystemLogService.java`
- `ThermalPrintService.java`
- `TrainingModeService.java`
- `TransferService.java`
- `WarehouseService.java`
- `WarehouseTransferService.java`

#### Repositories
- `AppConfigRepository.java`
- `AuditLogRepository.java`
- `BranchRepository.java`
- `CashMovementRepository.java`
- `CashSessionRepository.java`
- `CategoryRepository.java`
- `CustomerRepository.java`
- `ExpenseRepository.java`
- `FilterPresetRepository.java`
- `MetricUnitRepository.java`
- `PaymentRepository.java`
- `ProductBarcodeRepository.java`
- `ProductRepository.java`
- `ProductionOrderRepository.java`
- `PurchaseItemRepository.java`
- `PurchaseRepository.java`
- `RoleRepository.java`
- `SaleItemRepository.java`
- `SaleRepository.java`
- `StockBranchRepository.java`
- `StockMovementRepository.java`
- `StockWarehouseRepository.java`
- `SupplierRepository.java`
- `SystemBackupRepository.java`
- `TransferItemRepository.java`
- `TransferRepository.java`
- `UserRepository.java`
- `WarehouseRepository.java`
- `WarehouseTransferRepository.java`

#### Entidades / modelos de domínio
- `AppConfig`
- `AuditLog`
- `Branch`
- `CashMovement`
- `CashSession`
- `Category`
- `Customer`
- `Expense`
- `FilterPreset`
- `MetricUnit`
- `Payment`
- `Product`
- `ProductBarcode`
- `ProductionOrder`
- `Purchase`
- `PurchaseItem`
- `Role`
- `Sale`
- `SaleItem`
- `StockBranch`
- `StockMovement`
- `StockWarehouse`
- `Supplier`
- `SystemBackup`
- `Transfer`
- `TransferItem`
- `User`
- `Warehouse`
- `WarehouseTransfer`
- `WarehouseTransferItem`

### 1.4 Telas e módulos de UI mapeados
- `login.fxml`
- `dashboard.fxml`
- `sale_form.fxml`
- `purchase_form.fxml`
- `product_form.fxml`
- `customer_form.fxml`
- `supplier_form.fxml`
- `expense_form.fxml`
- `cash_session.fxml`
- `cash_movement_form.fxml`
- `close_session_form.fxml`
- `open_session_form.fxml`
- `branch_form.fxml`
- `category_form.fxml`
- `metric_unit_form.fxml`
- `warehouse_form.fxml`
- `warehouse_transfer_form.fxml`
- `stock_adjust_form.fxml`
- `stock_new_form.fxml`
- `production_order_form.fxml`
- `payment_form.fxml`
- `reports.fxml`
- `sistema_module.fxml`
- `user_form.fxml`

### 1.5 Dependências de build
Ficheiro principal: `pom.xml`.

Principais dependências:
- `spring-boot-starter`
- `spring-boot-starter-data-jpa`
- `spring-security-crypto`
- `jackson-databind`
- `mariadb-java-client`
- `poi` / `poi-ooxml`
- `pdfbox`
- `javafx-controls`
- `javafx-fxml`
- `javafx-swing`
- `flyway-core`
- `flyway-mysql`
- `spring-boot-starter-cache`
- `spring-boot-starter-test`
- `h2` (scope test)

### 1.6 Banco de dados e esquema
Base de dados: MariaDB/MySQL local, database `sgv`.

Esquema principal já representado por migrações:
- `V1__baseline.sql`
- `V2__at_compliance.sql`
- `V3__warehouses.sql`
- `V4__system_logs.sql`
- `V5__product_enhancements.sql`
- `V6__system_backups.sql`
- `V7__decimal_monetary_columns.sql`
- `V8__missing_schema_objects.sql`
- `V9__revert_decimal_to_double.sql`
- `V10__stock_movements_missing_columns.sql`
- `V11__app_config_new_columns.sql`
- `V12__license_columns.sql`
- `V13__decimal_monetary_columns_fix.sql`
- `V14__product_flow_integrity.sql`

Tabelas-base mapeadas:
- `branches`
- `users`
- `roles`
- `user_roles`
- `role_permissions`
- `categories`
- `metric_units`
- `suppliers`
- `customers`
- `products`
- `product_barcodes`
- `stock_branch`
- `stock_movements`
- `purchases`
- `purchase_items`
- `sales`
- `sale_items`
- `payments`
- `expenses`
- `production_orders`
- `cash_sessions`
- `cash_movements`
- `transfers`
- `transfer_items`
- `audit_logs`
- `filter_presets`
- `app_config`
- `warehouses`
- `stock_warehouse`
- `warehouse_transfers`
- `warehouse_transfer_items`
- `system_backups`

## 2. Arquitectura usada

### 2.1 Padrão identificável
O sistema usa arquitetura em camadas, com forte componente de MVC desktop por JavaFX:
- `Controller` -> `Service` -> `Repository` -> `DB`
- UI e regras de negócio separadas.
- Não é um sistema REST; o “endpoint” é a tela JavaFX e o controlador associado.

### 2.2 Conclusão técnica
- Sim, há separação de responsabilidades.
- O padrão é funcional, mas não é “Clean Architecture”.
- Há muita lógica em controllers e services.
- Faltam DTOs bem estabelecidos para desacoplar view-model e entidade.
- Não existe camada web REST; não há `@RestController` nem API formal.

## 3. Diagnóstico funcional por módulo

### 3.1 Relatório de completude

| Módulo | Estado | % Completo | O que falta |
|--------|--------|------------|-------------|
| Login e autenticação | ✅ | 85% | faltam session timeout, recuperação de password, logs de auditoria mais fortes |
| Gestão de utilizadores | ⚠️ | 75% | faltam regras de recuperação de password e workflow completo de segurança |
| Gestão de produtos | ⚠️ | 78% | falta histórico de preços, imagens, SKU/variações robustas |
| Gestão de clientes | ⚠️ | 72% | faltam múltiplos contactos, classificação, maior normalização de NUIT |
| Gestão de vendas | ✅ | 83% | falta fluxo PDV 3 cliques, anulação mais rígida, devoluções e notas fiscais completas |
| Gestão de stock | ✅ | 82% | faltam alertas e histórico detalhado mais formal |
| Gestão de compras | ⚠️ | 70% | faltam ordens de compra e reconciliations formais |
| Gestão de caixa | ✅ | 80% | falta processamento formal de sangria/reforço e reconciliations audiáveis |
| Gestão de despesas | ✅ | 78% | falta rastreio completo de aprovações/contas e regras fiscais |
| Relatórios/dashboard | ✅ | 76% | falta exportação mais formal e gráficos mais robustos |
| Fiscalidade MZ | ⚠️ | 60% | ainda não está com a conformidade tributária completa para produção |
| Backup/restore | ⚠️ | 65% | backup manual com pouco automatismo e recuperação validada |

## 4. Análise de negócio - Sistema de vendas

### 4.1 Módulos obrigatórios identificados

#### ✅ Existentes
- Gestão de produtos e preços
- Gestão de clientes
- Vendas com itens
- Pagamentos por vários métodos
- Stocks por filial
- Compras/fornecedores
- Caixa e sessões
- Dashboard e KPIs
- Usuários e permissões
- Log e auditoria

#### ⚠️ Incompletos ou parcialmente implementados
1. Produtos/serviços
- Existe cadastro básico de produtos.
- Há `priceCost`, `priceSale`, `priceSaleBulk`, `stockMin`, `stockMax`.
- Falta полноценamente:
  - imagens de produtos
  - SKU com regras fortes
  - histórico de preços
  - variações e subcategorias
  - validações de unicidade e integridade mais rígidas

2. Clientes
- Existe nome, NUIT, endereço, contacto, saldo, crédito.
- Falta:
  - classificação por tipo e comportamento
  - contactos múltiplos
  - histórico detalhado e cálculo de risco
  - validação formal para Moçambique

3. Vendas
- Sistema tem venda com itens, pagamentos, stock, documentos.
- Falta:
  - PDV ultra rápido
  - desconto global consistente
  - crédito parcelado com regras claras
  - devolução formal com reposição
  - anulação fiscal obrigatória e log irreversível

4. Documentos fiscais
- Serviço fiscal existe, mas o nível de conformidade e versão final para Moçambique ainda não está “pronto para produção”.
- Faltam modelos e regras detalhadas para:
  - factura
  - factura-recibo
  - recibo
  - nota de crédito
  - guia de remessa
  - série/numeração estritamente conforme AT

5. Financeiro
- Caixa e despesas existem.
- Falta:
  - contas a receber em contabilidade de crédito
  - contas a pagar com workflow formal
  - conciliação financeira robusta

6. Relatórios
- Dashboard e charting existem.
- Falta:
  - exportação consolidada para PDF/Excel em camadas definidas
  - volatilidade de dados e relatórios fiscais Oficiais

### 4.2 Módulos que não existem concluídos
- ❌ MÓDULO “PROFORMA / ORÇAMENTO COMPLETO” NÃO ENCONTRADO EM FLUXO ROBUSTO
- ❌ MÓDULO “NOTA DE CRÉDITO / DÉBITO COMPLETO” NÃO ENCONTRADO COMO DOCUMENTO EXECUTÁVEL
- ❌ MÓDULO “RECUPERAÇÃO DE PASSWORD” NÃO IMPLEMENTADO COMO FLUXO REAL
- ❌ MÓDULO “AUTENTICAÇÃO COM TIMEOUT DE SESSÃO” NÃO CONFIRMADO EM CAMADA DE SEGURANÇA
- ❌ MÓDULO “BACKUP / RESTAURAÇÃO AUTOMATIZADO” NÃO ENCONTRADO COMO PROCESSO EMPACOTADO

## 5. Análise de modelos e entidades

### 5.1 Conclusão geral
Existem entidades bem desenhadas para operação típica de POS, mas há uma mistura de estilos:
- Algumas entidades usam `BigDecimal` para dinheiro.
- Outras continuam a usar `Double` para valores monetários.
- Isso é um risco relevante em produção, porque valores em meticais devem ser tratados com precisão decimal rígida.

### 5.2 Problemas evidentes

#### 🔴 Critical
1. Uso misto de `Double` e `BigDecimal` em entidades monetárias.
Exemplos:
- `Expense.java` usa `Double` no campo `amount` (antes do ajuste local)
- `Payment.java` usa `Double` em `amount`
- `Product.java` nalguns campos continua com `Double`
- `StockMovement.java` tem campos monetários em `Double`

Impacto:
- imprecisão fiscal e contábil
- erros de arredondamento para Moçambique (MZN)
- risco de inconsistência com o schema DECIMAL(19,4)

2. Ausência de validação Bean Validation em entidades.
Não se encontrou `@NotNull`, `@NotBlank`, `@Min`, `@Pattern`, `@Valid` em força em entidades relevantes.

Impacto:
- entrada inválida pode entrar na base de dados
- dados fiscais pouco robustos

3. Falta de soft delete / anti-delete para documentos fiscais.
No código de negócio, não há garantia de uma política fiscal que impeça eliminações de documentos.

Impacto:
- violação de integridade legal/fiscal

#### 🟠 High
4. Muitos controllers são componentes Spring com alta responsabilidade.
Há um conjunto bastante volumoso de controladores UI e managers a cuidar de UI + regras de negócio.

Impacto:
- acoplamento alto
- manutenção difícil
- risco de regressão maior

5. Falta de DTOs claros e separação de camada de apresentação.

#### 🟡 Medium
6. Ausência de índices explícitos para campos de pesquisa e auditoria.
7. Ausência de campos de auditoria completos (`criadoPor`, `actualizadoPor`, etc.) em entidades de negócio.
8. Falta de um serviço de backup automatizado concluído.

## 6. UX / UI / visual

### 6.1 O que está bom
- Há um dashboard consistente.
- Há diversas telas de gestão com estrutura semelhante.
- Há contrôles de navegação centralizados.
- Há um padrão visual de JavaFX bem organizado.

### 6.2 O que está incompleto
- Não há evidência sólida de shortcuts de teclado em todos os módulos.
- Não há um poderoso fluxo PDV mínimo de 3 cliques.
- Não há feedback de toast/notifications centralizadas.
- Nenhuma confirmação uniforme de acções destrutivas em todos os módulos.
- UX ainda está muito orientada a “cadastro e formulário”, não ao “fluxo rápido de venda”.

## 7. Segurança e performance

### 7.1 Segurança
- Passwords com hash via BCrypt: sim.
- SQL Injection: minimamente protegido via JPA repositories.
- Autorização por roles e permissions: sim.
- Sessão e timeout: não evidenciado com força.
- Recuperação de password: não implementada como fluxo.

### 7.2 Performance
- Cache simples implementado para dashboard e produtos.
- Alguns serviços usam carregamentos massivos e normalizações.
- Ainda existem riscos de N+1 em certos fluxos de UI se o carregamento não for cuidadoso.

## 8. Contexto moçambicano

### 8.1 Boa cobertura
- Moeda e contexto local em texto e nomes de módulos existem.
- Tipos de pagamento locais em UI aparecem parcialmente: `M-Pesa`, `e-Mola`.
- O contexto “desktop offline” é adequado para ambientes instáveis.

### 8.2 Falhas de conformidade
- Taxa de IVA 16% não está formalmente centralizada em config fiscal robusta.
- NUIT não é tratado como regra de negócio forte em toda a cadeia.
- Numeração fiscal sequencial e “não apagar facturas” ainda não estão formalmente enforçados em toda a aplicação.
- Conformidade SAFT-MZ ainda não está validada como peça completa.

## 9. Diagnóstico final

### 9.1 Problemas críticos que impedem prontidão para produção
1. Uso misto de tipos monetários `Double`/`BigDecimal`
2. Falta de validações Bean Validation massivas
3. Sistema funcional, mas não totalmente fiscalmente conformante para Moçambique
4. Sem política de anti-delete para documentos fiscais
5. Fluxo PDV ainda não é “perfeito” para um operador de baixa literacia digital
6. Falta de recuperação de password e gestão de sessão robusta
7. Backup/restauração não está automatizado como processo de produção

### 9.2 Nota final estimada

| Categoria | Nota (0-10) |
|-----------|-------------|
| Completude funcional | 7.5/10 |
| Qualidade do código | 6.5/10 |
| Interface/UX | 7.0/10 |
| Segurança | 6.5/10 |
| Performance | 7.0/10 |
| Conformidade fiscal MZ | 5.5/10 |
| Prontidão para produção | 5.5/10 |
| NOTA FINAL | 6.5/10 |

## 10. Roadmap de implementação

### Ordem oficial
1. Corrigir tipos monetários e centralizar `BigDecimal` em entidades-chave.
2. Adicionar validações Bean Validation e regras fiscais obrigatórias.
3. Definir políticas de anti-delete / anulação / auditoria legal.
4. Formalizar documentos fiscais e séries/document numbers.
5. Melhorar autenticação: timeout, recuperação, logging e sessão.
6. Completar fluxo PDV/PDV rápido e evitar UI acoplada.
7. Implementar backup/restauração e política de deploy.
8. Polir UX, keyboard shortcuts e mensagens.

## 11. Resumo executivo
O sistema tem um núcleo bastante útil e funcional para operação comercial local, com vendas, stock, caixa, compras, produtos e dashboard. No entanto, ele está em uma fase de boa base operacional, mas ainda não está pronto para produção em ambiente fiscal sério e rigoroso em Moçambique.

A maior causa de preocupação é o uso heterogéneo de valores monetários, a falta de validações fortes e a ausência de uma política fiscal totalmente rígida para documentos e anulações.
