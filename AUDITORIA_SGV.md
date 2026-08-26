# AUDITORIA COMPLETA DO SISTEMA SGV
**Repositório:** `Junior-Web37/SGV` · **Commit auditado:** `4f80aea` (main) · **Data:** 2026-08-25
**Papel:** Auditor Sénior de Software / QA Lead / Arquiteto / Analista de Negócio / UX / BD / Integração E2E

---

## 0. AMBIENTE, METODOLOGIA E LIMITAÇÕES (RELA 42)

| Item | Estado |
|---|---|
| Acesso ao código-fonte completo | **Sim** (202 ficheiros Java, 25 FXML, 27 migrações SQL) |
| Execução da aplicação (JavaFX) | **NÃO TESTADO** — sandbox sem JDK/Maven e **sem rede** (impossível instalar OpenJDK 21 / obter dependências Maven). Nenhum teste de regressão foi executado. |
| Acesso à base de dados real | **NÃO TESTADO** — sem MariaDB no ambiente. |
| Método utilizado | Análise estática profunda da cadeia completa: FXML → Controller → Service → Repository → SQL/Flyway → Efeitos secundários → Auditoria → UI. Cada constatação abaixo é **confirmada por evidência no código** (ficheiro + método). Onde o comportamento em runtime não pôde ser verificado, está indicado "evidência por código; teste em execução necessário". |

**Nota técnica importante:** o enunciado da auditoria assumia JDBC; o sistema real usa **Spring Data JPA / Hibernate** sobre **MariaDB** (Flyway V1–V27) + JavaFX 21 + Spring Boot 3.1.4 (perfil `web-application-type=none`). Os "Repositories/DAO" são interfaces JPA.

---

## 1. INVENTÁRIO TÉCNICO (RESUMO)

### Stack
- **Backend:** Spring Boot 3.1.4, Java 21, JPA/Hibernate 6, Spring Security Crypto (BCrypt), Spring Cache (ConcurrentMap), `@EnableCaching` **ativo**, `@EnableScheduling` **ativo**, Jackson.
- **Base de dados:** MariaDB (perfil `mysql` por omissão em runtime), Flyway V1→V27; testes em H2 in-memory com `ddl-auto=create-drop` (**o esquema de teste não é o esquema de produção — derivação de esquema possível**).
- **UI:** JavaFX 21, 25 FXML, ~35 controllers/panes programáticas, POI (Excel), PDFBox 3.0.2 (PDF A4/A5/80mm/58mm).
- **Integrações (no código):** AT (Autoridade Tributária) via `AtSubmissionService` (HTTP), SAF-T via `SafTExportService`, WhatsApp via `ReceiptDeliveryService`, backup via `mysqldump` (subprocesso).

### Mapa de componentes (principais)
| Componente | FXML | Controller | Services principais | Tabelas |
|---|---|---|---|---|
| Login | login.fxml | LoginController | DesktopAuthService | users, roles, role_permissions |
| Dashboard/Painel | dashboard.fxml | DashboardController + KpiManager + CrudManager + NavigationManager | DashboardService(morta), CashSessionService, StockBranchService | sales, stock_branch, cash_sessions, cash_movements, expenses |
| PDV | sale_form.fxml | SaleFormController | SaleService, CashSessionService, StockBranchService, SaleNumberingService, FiscalService, SaleDocumentService/ThermalPrintService | sales, sale_items, stock_branch, cash_sessions, cash_movements, customers, series_counters |
| Histórico/Anulação/NC | (pane no dashboard) | DashboardCrudManager | SaleService (annulSale, createCreditNote) | sales, sale_items, stock_branch, customers |
| Caixa | cash_session, open_session_form, close_session_form, cash_movement_form | CashSessionController, Open/Close/CashMovementFormController | CashSessionService | cash_sessions, cash_movements |
| Pagamentos | payment_form.fxml | PaymentFormController | PaymentService | payments, payment_allocation, sales, customers, cash_movements |
| Cliente / CC | customer_form.fxml | CustomerFormController, DashboardCrudManager | CustomerService, CustomerAccountService | customers, sales, payments, payment_allocation |
| Compras | purchase_form.fxml | PurchaseFormController | PurchaseService, WarehouseService | purchases, purchase_items, stock_warehouse, stock_movements, products |
| Fornecedores / Pag. Fornecedor | supplier_form, supplier_payment_form | SupplierFormController, SupplierPaymentFormController | SupplierService, SupplierPaymentService | suppliers, supplier_payments, purchases, cash_movements |
| Stock (filial) | stock_new_form, stock_adjust_form | StockNew/StockAdjustFormController | StockBranchService | stock_branch, stock_movements |
| Stock (armazém) | warehouse_form + pane | WarehouseFormController, DashboardCrudManager | WarehouseService | stock_warehouse, warehouses, stock_movements |
| Transferências armazém→filial | warehouse_transfer_form | WarehouseTransferFormController | WarehouseTransferService | warehouse_transfers(+items), stock_warehouse, stock_branch, stock_movements |
| Transferências filial→filial | (pane) | DashboardCrudManager/NavigationManager | TransferService | transfers(+items), stock_branch, stock_movements |
| Produção | production_order_form | ProductionOrderFormController | StockBranchService (consumeIngredientsForProduction) | production_orders, product_recipes, stock_branch, stock_movements |
| Relatórios | reports.fxml | ReportsController | ReportService, SafTExportService | sales, sale_items, stock_movements, payments |
| Sistema | sistema_module.fxml (parcialmente morta) + panes programáticas | SistemaModuleController, SystemSettingsPageManager | AppConfigService, BackupService(não ligada à UI), LicenseService, TrainingModeService, AuditLogService, SystemLogService | app_config, system_backups, users, roles, audit_logs |

### Triggers/procedures na BD
- **Triggers MariaDB** criados em runtime por `DbTriggerConfig`: `trg_prevent_sales_update`, `trg_prevent_sales_delete`, `trg_prevent_sale_items_update/delete` (proteção fiscal de documentos emitidos). **Não existem em H2 (testes) e não existem triggers de integridade de stock** (ex.: impedimento de `stock_current < 0` a nível BD — a proteção é só em Java).
- Sem procedures. `series_counters` com `SELECT … FOR UPDATE` (numeração atómica).

### Enums/estados (resumo — detalhe na secção 15)
- `SaleState`: EMITIDA, PAGO, **PAGO_PARCIAL (string solta, fora do enum!)**, ANULADA, COTACAO_ABERTA, COTACAO_PAGA, ENCOMENDA_ABERTA.
- `DocumentType`: VENDA, COTACAO, ENCOMENDA, FACTURA, RECIBO, NC, ND.
- `PaymentMethod`: DINHEIRO, MPESA, EMOLA, MKESH, POS, DEBITO, TRANSFERENCIA, CREDITO, CHEQUE, MULTICAIXA (UI não oferece MULTICAIXA).
- Compra: RECEIVED, PENDING, CANCELLED, **PAID / PAGO_PARCIAL (strings soltas)**.
- Transfer (filial→filial): PENDING, APPROVED, REJECTED, COMPLETED, CANCELLED.
- WarehouseTransfer (armazém→filial): PENDING, IN_TRANSIT, COMPLETED, CANCELLED.
- CashSession: OPEN, CLOSED. CashMovement.type: IN/OUT (+ OperationKind SALE/QUOTE/REFUND/TRANSFER/OTHER).
- ProductionOrder: PENDING, COMPLETED (PENDING nunca usado — a ordem nasce COMPLETED).
- Expense: PENDING, PAID, CANCELLED.

---

## 2. RESUMO EXECUTIVO (41.A)

| Métrica | Valor |
|---|---|
| Total de funcionalidades auditadas (lista de 62) | **62** (+ ~15 implícitas descobertas) |
| OK | **25** |
| PARCIAL | **26** |
| FALHA | **11** |
| AUSENTE nas 62 listadas | **0** — mas existem **20** funcionalidades implícitas que deveriam existir e não existem (§8, A1–A20) |
| NÃO TESTÁVEL em runtime (só código) | todas as acima (ver §0) |
| Bugs **CRÍTICOS** | **7** |
| Bugs **ALTO** | **21** |
| Bugs **MÉDIO** | **16** |
| Bugs **BAIXO** | **10** |
| **MELHORIAS** (P2/P3, sem classe de severidade de bug) | **42** (§8: 20 + §16 Performance: 8 + §17 UX: 14; algumas sobrepõem-se a bugs) |
| Serviços órfãos (implementados, zero chamadores) | **8** (AT, WhatsApp, Apuramento diário, Compliance, Lotes, Encomendas de Compra, PrintHtml, DashboardService) + `BackupService` (real, mas não ligada à UI — só cron) |
| Código morto identificado | **~20 blocos** |

**Veredicto global à pergunta-objectivo:** **NÃO.** Um utilizador real que use o SGV de ponta a ponta encontrará, com evidência no código: (1) backup/restauro que **mentem sucesso** e não produzem cópias recuperáveis; (2) produção que pode **consumir matérias-primas em silêncio** e deixar stock negativo; (3) caixa em que **M-Pesa/POS/transferências contam como numerário físico** — a fita Z nunca fechará certo num negócio com meios electrónicos; (4) **três números diferentes para as mesmas vendas** (dashboard vs mapa de vendas vs apuramento de IVA); (5) extrato do cliente que **diverge do saldo real**; (6) pagamento de cotações/encomendas possível; (7) licenciamento forjável e sem efeito restritivo real; (8) utilizador desactivado **re-activado a cada arranque**. Os módulos de stock/caixa/numeração são os mais sólidos; a camada de relatórios, tesouraria multi-método, conformidade AT e gestão de risco (crédito/backup/licença) é onde o sistema falha.

---

## 3. TOP 20 PROBLEMAS MAIS GRAVES (ordenados por impacto) — 41.B

| # | ID | Severidade | Título | Impacto em 1 linha |
|---|---|---|---|---|
| 1 | BUG-001 | CRÍTICO | **Backup/Restauro falsos** | O botão "Criar Backup" regista `COMPLETED` na BD **sem criar qualquer ficheiro** (copia um `sgv.db` inexistente — resto de stack antiga); o "Restaurar" copia um ficheiro único para `C:\xampp\mysql\data\sgv` (caminho hardcoded, nunca funciona em MariaDB real) e marca `RESTORED`. O único backup real (`BackupService` via mysqldump) **não é chamado pela UI** e o agendamento ignora a hora/frequência configurada. **Risco: perda total de dados sem capacidade de recuperação.** |
| 2 | BUG-003 | CRÍTICO | **Meios de pagamento electrónicos lançados no caixa físico** | `SaleService.persistSaleTransaction` regista movimento de caixa `IN` por **qualquer** método ≠ CREDITO (M-Pesa, e-Mola, mKesh, POS, transferência bancária, cheque). O "Dinheiro esperado" do fecho sobrestima o numerário em todo o volume não-cash → **quebra/sobra falsa permanente**, caixa nunca reconcilia. |
| 3 | BUG-002 | CRÍTICO | **Produção: consumo de matérias-primas com `catch` vazio** | `StockBranchService.consumeIngredientsForProduction` engole **todas** as excepções (sem log) e permite `after` negativo → produção gravada sem consumo (ou com stock de MP negativo) **sem nenhum aviso**. Custo e stock do produto acabado ficam errados em silêncio. |
| 4 | BUG-004 | CRÍTICO | **3 números diferentes para as mesmas vendas** | Mapa Geral de Vendas soma **todas** as vendas (incl. ANULADA, COTAÇÃO, ENCOMENDA, demo); Apuramento de IVA exclui ANULADA mas inclui cotações/encomendas/demo e **mau-categoriza documentos mistos**; Dashboard/KPIs exclui ANULADA mas inclui demo+cotações. IVA, mapa, dashboard e caixa divergem do mesmo documento. |
| 5 | BUG-005 | CRÍTICO | **Extrato do cliente ≠ saldo real** | `CustomerAccountService.getCustomerStatement`: NC abate o extrato mas **não** abate o saldo real (`createCreditNote` nunca toca no cliente); pagamento sem fatura abate o saldo real mas **não** aparece no extrato; cotações abertas aparecem como "Venda a Pronto". O saldo final do extrato nunca casa com `customers.balance`. |
| 6 | BUG-007 | CRÍTICO | **Cotações/Encomendas aparecem na lista de recebimentos e podem ser "pagas"** | `findPendingSales` = `state <> 'ANULADA' AND (total-paid) > 0.01` → inclui COTAÇÃO/ENCOMENDA. O formulário de pagamento permite liquidar uma cotação: cria `Payment`, movimento de caixa IN, passa o estado a PAGO — **sem qualquer baixa de stock** e sem que exista dívida. Também infla "Contas a Receber" (KPI) e o aging de devedores. |
| 7 | BUG-006 | CRÍTICO | **DataInitializer re-activa utilizadores e semeia dados "reais"** | A cada arranque `ensureUser("admin","admin",…)` faz `setActive(true)` → **um utilizador desactivado volta a activar-se no próximo arranque**; cria a venda de demonstração nº1 **sem `demoFlag`** (entra em todos os relatórios como venda real); cria `admin/admin` por omissão. |
| 8 | BUG-011 | ALTO | **Anulação: estorno de caixa engolido em silêncio** | `annulSale` devolve stock e (em vendas cash) tenta `registerMovement(OUT, …)` dentro de `catch (Exception ignored)` → se o operador não tiver turno aberto, a anulação **prosseguir com stock devolvido, factura anulada e caixa não debitada** (inconsistência permanente, sem log de erro). O estorno é também lançado no turno do **anulador**, não do turno original. |
| 9 | BUG-010 | ALTO | **Nota de Crédito com efeitos incompletos** | `createCreditNote` devolve stock, mas **não abate a dívida do cliente** (vendas a crédito) e **não regista reembolso no caixa** (vendas cash); NC sem `hashHash`/assinatura AT. O crédito do cliente nunca diminui com a devolução. |
| 10 | BUG-012 | ALTO | **Pagamentos: sem RBAC, sem lock, recibo não fiscal** | `PaymentService.createPayment`: sem verificação de permissão, sem `@Version`/pessimistic lock (pago em dobro sob concorrência); o "recibo" é gerado a **mudar `documentType` da factura para RECIBO em memória** (risco de flush → trigger fiscal `SIGNAL 45000`) e **sem numeração própria da série RECIBO** (mesmo número da FT) — não conforme; o troco do pagamento não é contabilizado em lugar nenhum. |
| 11 | BUG-008 | ALTO | **Troco e valor recebido descartados** | O PDV permite "Valor recebido" e calcula troco, mas `persistSaleTransaction` **sobrescreve** `paidAmount=total` e `changeAmount=0` → o que o operador digitou nunca chega à BD; o documento não tem o troco real. |
| 12 | BUG-009 | ALTO | **Venda a crédito fica com estado PAGO** | `applyDocumentState`/PDV marcam VENDA/FACTURA como `PAGO` independentemente do método → faturas em dívida aparecem como pagas; toda a lógica pendente depende de `paidAmount=0` (frágil). `PAGO_PARCIAL` nem sequer existe no enum `SaleState` (string solta). |
| 13 | BUG-013 | ALTO | **Duplo stock: compras chegam ao armazém, vendas consomem a filial** | `PurchaseService` lança stock para `stock_warehouse`; `SaleService` consome `stock_branch`. A única ponte é a transferência TWA. Sem usar transferências, **produto comprado nunca é vendável no PDV**; "Stock Central" + "Inventário" nunca somam o stock total; o kardex unificado mascara a desconexão. |
| 14 | BUG-015 | ALTO | **Reconciliação FIFO alocada a cotações** | `reconcileCustomerCredits` aloca o pagamento às pendências do cliente — que **incluem cotações/encomendas abertas** (não são dívidas); abate o saldo em `amount` total (excesso gera crédito sem controlo); o extrato não reflete a alocação. |
| 15 | BUG-016 | ALTO | **Cadeia fiscal (hash) incorreta e QR ausente** | `signatureHash` usa como "previous hash" a venda mais recente **por createdAt e de todas as séries da filial** (não por série/hashControl) → cadeia rompida com séries múltiplas ou vendas no mesmo segundo; `hashHash` é um esquema interno simplificado (não o padrão AT/MZ); **`sale.qrCode` nunca é populado** → nenhum documento tem QR (obrigatório na facturação electrónica MZ). |
| 16 | BUG-017 | ALTO | **Transferências filial→filial: backend sem RBAC + numeração racy** | `TransferService.create/approve/complete/cancel` **não verificam nenhuma permissão** (qualquer utilizador autenticado — ou qualquer chamador do service — pode mover stock entre filiais); número = `MAX+1` sem lock → duplicados sob concorrência. |
| 17 | BUG-021 | ALTO | **`forceChangePassword` nunca imposto** | O flag existe na entidade e no formulário de utilizador, mas `DesktopAuthService.authenticate` nunca o verifica → utilizador com password forçada continua a entrar com a password antiga indefinidamente. |
| 18 | BUG-022 | ALTO | **Moeda estrangeira sem taxa de câmbio** | O PDV oferece USD/EUR/ZAR; `exchangeRate` nunca é definido (fica 1.0) nem aplicado → totals em moedas diferentes somados como MZN em dashboard/relatórios/IVA. |
| 19 | BUG-027 | ALTO | **Licenciamento sem efeito** | A chave não é ligada ao machineId; o **salto secreto está no source e o próprio client gera chaves** (`generateLifetimeLicense` público); sem chave activa o sistema trata como "trial ilimitado"; expiração apenas mostra um aviso (não bloqueia). Licença comercial não é tecnicamente exigível. |
| 20 | BUG-033 | ALTO | **Concorrência: lost updates e double-spending** | Sem `@Version` em `Sale`, `Customer`, `Purchase`, `SupplierPayment` → 2 vendas de crédito simultâneas podem ultrapassar o limite de crédito; 2 pagamentos simultâneos à mesma factura passam ambos a validação (pago em dobro, caixa com dois IN). Stock tem `@Version` mas o conflito surge como erro 500 cru (sem retry/mensagem). |

---

## 4. ANÁLISE DETALHADA DOS PRINCIPAIS BUGS (FORMATO OBRIGATÓRIO 33)

### BUG-001 — Backup e Restauro não funcionam (registam sucesso sem produzir cópia)
- **Severidade:** CRÍTICO · **Prioridade:** P0
- **Módulo:** Configurações → Backup/Restauro
- **Tela:** Sistema → Backups (`SystemSettingsPageManager.buildBackupsPane`)
- **FXML:** — (pane programático) · **Controller:** SystemSettingsPageManager (e cópia morta em SistemaModuleController)
- **Service:** `BackupService` (não ligada à UI) · **Repository:** SystemBackupRepository
- **Entidade/Tabela:** `system_backups`
- **Descrição:** O fluxo "Criar Backup" resolve `Paths.get("sgv.db")` (ficheiro de BD embebida de uma stack anterior). Como a BD real é MariaDB, `Files.exists(dbPath)` é **falso**, o `Files.copy` nunca corre, e mesmo assim é gravado um registo `SystemBackup(status=COMPLETED, fileSize=0)` e mostrado o diálogo **"Backup criado com sucesso!"**. O "Restaurar" copia o (inexistente) ficheiro `.db` para o caminho **hardcoded `C:\xampp\mysql\data\sgv`** e, se o directório existir, marca `RESTORED`/`COMPLETED` — mas copiar um único ficheiro para a data-directory do MariaDB **não restaura a base de dados**. `deleteBackup` apaga o registo e tenta apagar `backups/<file>` (que nunca existiu).
- **Comportamento esperado:** dump real da BD (SQL ou binário) gravado, verificado (checksum/size>0), e restauro que devolve a BD ao estado do backup.
- **Comportamento atual:** registo falso de sucesso; nenhum ficheiro criado; restauro inexistente em qualquer perfil (mysql/prod/docker).
- **Passos para reproduzir:** Sistema → Backups → "Criar Backup Agora" → observar "sucesso"; a pasta `./backups` fica **vazia**; a BD `system_backups` tem um registo COMPLETED com tamanho 0.
- **Dados utilizados:** qualquer.
- **Causa provável:** código legado de quando a app usava BD embebida (ficheiro único); nunca adaptado à migração MariaDB.
- **Evidência no código:** `SystemSettingsPageManager.createBackupNow` (L414–441: `Path dbPath = Paths.get("sgv.db"); if (Files.exists(dbPath)) Files.copy(...)`; `bk.setStatus("COMPLETED")` incondicional) e `confirmRestore` (L443–503: `Path mariaDataDir = Paths.get("C:\\xampp\\mysql\\data\\sgv")`). `BackupService.backupNow` (mysqldump correcto) **tem zero chamadores** (verificado por grep em todo o `main`).
- **Evidência durante execução:** NÃO TESTADA (sem runtime) — mas o raciocínio é determinístico: `sgv.db` não é criado por nenhum processo da app.
- **Impacto:** **Perda total de dados sem capacidade de recuperação.** O único backup real é o cron `0 0 3 * * *` de `BackupService` (se `autoBackupEnabled`), que (a) ignora a hora/frequência configuradas na UI, (b) não regista em `system_backups` (invisível na UI) e (c) escreve para `~/Documents/SGV/backups`, fora do histórico.
- **Dependências afetadas:** toda a persistência; "Retenção" configurável (sem job de limpeza excepto o privado `cleanupOldBackups(7)` do cron).
- **Correção recomendada:** ligar a UI ao `BackupService.backupNow` (ou `mysqldump` via JDBC `mysqldump` do driver); gravar só `COMPLETED` se `exitCode==0 && size>0` (com checksum SHA-256); restauro = parar escrita → `mysql < backup.sql` (ou `RESTORE` via API do MariaDB) em transação/backup anterior; persistir o caminho real; agendamento com `@Scheduled` dinâmico ou `TaskScheduler` conforme `appConfig`; adicionar teste E2E de backup→restauro.
- **Prioridade:** P0.

### BUG-002 — Produção: consumo de matérias-primas engolido e stock negativo permitido
- **Severidade:** CRÍTICO · **Prioridade:** P0
- **Módulo:** Produção
- **Tela:** Produção → Nova Ordem (`production_order_form.fxml`)
- **Controller:** ProductionOrderFormController · **Service:** StockBranchService.consumeIngredientsForProduction
- **Repository:** ProductRecipeRepository, StockBranchRepository
- **Entidade/Tabela:** `stock_branch`, `stock_movements`, `product_recipes`, `production_orders`
- **Descrição:** Para cada ingrediente do BOM, o consumo faz read-modify-write sem verificação de saldo e dentro de `try { … } catch (Exception e) { // Log and continue }` **sem qualquer log**. Qualquer falha (falta de linha de stock, lock, constraint) é ignorada e a produção continua. Não há bloqueio de `after < 0` neste método (a `decreaseStock` tem, o consumo **não** tem).
- **Comportamento esperado:** consumo atómico da MP (transacção), bloqueio se MP insuficiente, stock negativo impossível, log/falha visível.
- **Comportamento atual:** ordem gravada COMPLETED + produto acabado entrado em stock **mesmo sem consumo** (ou com MP negativa); nenhum erro, nenhum log.
- **Passos para reproduzir:** produto acabado PAO01 com receita (MAT01 0.035kg, MAT02 0.001kg); fazer com stock de MAT02 = 0; produzir 100 unidades → stock MAT02 = −100 (ou, se a linha não existir, silêncio total); ordem fica COMPLETED e +100 de PAO01 no stock.
- **Dados utilizados:** BOM do seed (Pão Francês 50g).
- **Causa provável:** `catch` genérico para "não bloquear a produção"; falta de verificação de saldo específica do consumo.
- **Evidência no código:** `StockBranchService.consumeIngredientsForProduction` (L96–130: `try { … sb.setStockCurrentAmount(after); … } catch (Exception e) { // Log and continue }`; sem `if (after < 0) throw`).
- **Evidência durante execução:** NÃO TESTADA (sem runtime).
- **Impacto:** custo do produto acabado nunca reflecte o MP consumida; Kardex inconsistentes (movimento de entrada sem saídas); stock negativo de MP; margens erradas em todos os relatórios de lucro.
- **Dependências afetadas:** Stock, Kardex, Produção, Relatórios de lucro, CM
- **Correção recomendada:** remover o `catch` (propagar → transacção completa falha), ou registar erro crítico e marcar a ordem `FAILED`; adicionar verificação `after < 0 → throw` (bloquear produção sem MP suficiente, com lista de faltas); adicionar `@Version` já existe (ok); teste: produção com MP insuficiente deve falhar atomicamente.
- **Prioridade:** P0.

### BUG-003 — Meios de pagamento não-cash lançados como entrada de caixa física
- **Severidade:** CRÍTICO · **Prioridade:** P0
- **Módulo:** Vendas / Tesouraria
- **Tela:** PDV (`sale_form.fxml`)
- **Controller:** SaleFormController.doSave · **Service:** SaleService.persistSaleTransaction → CashSessionService.registerMovement
- **Repository:** CashMovementRepository
- **Entidade/Tabela:** `cash_movements`, `cash_sessions`
- **Descrição:** No bloco de caixa do `persistSaleTransaction`, a condição é apenas `!"CREDITO".equalsIgnoreCase(paymentMethod)` → **MPESA, EMOLA, MKESH, POS, DEBITO, TRANSFERENCIA, CHEQUE, MULTICAIXA** produzem `registerMovement(user, "IN", total, …, OperationKind.SALE)`. Dinheiro de telemóvel/banco é somado ao numerário físico.
- **Comportamento esperado:** apenas DINHEIRO (eventualmente CHEQUE a cobrar) movimenta a gaveta; os outros geram entradas em "conta" (bancos/electrónicos) ou não movem a gaveta; o fecho (fita Z) compara só o numerário contado.
- **Comportamento atual:** `Dinheiro esperado = fundo + vendas cash + vendas M-Pesa + POS + …` → sobra/quebra falsa em todos os turnos com meios electrónicos.
- **Passos para reproduzir:** vender 1.000 MT por M-Pesa; fechar o turno contando o fundo → "sobra" de 1.000 MT.
- **Dados utilizados:** qualquer venda não-cash.
- **Causa provável:** simplificação "tudo que não é crédito entra no caixa".
- **Evidência no código:** `SaleService.persistSaleTransaction` (L213–228: `if (!"CREDITO".equalsIgnoreCase(persistentSale.getPaymentMethod())) { … cashSessionService.registerMovement(currentUser, "IN", saleTotal, …) }`).
- **Evidência durante execução:** NÃO TESTADA (determinístico por código).
- **Impacto:** **perda de fiabilidade total da tesouraria** em qualquer loja com M-Pesa/POS (o caso dominante em MZ); reconciliação diária impossível; quebras reais escondidas.
- **Dependências afetadas:** Caixa, Fecho/Fita Z, Resumo Financeiro, Dashboard (saldo de caixa), Apuramento.
- **Correção recomendada:** `PaymentMethod` com atributo `movesCashDrawer` (DINHEIRO=true; resto false) e só registar `IN` para esses; criar movimentos/contas separadas para electrónicos (ou relatório por método no fecho); actualizar a fita Z para mostrar esperados por método; teste E2E: venda MPESA → `cash_movements` vazio para a sessão.
- **Prioridade:** P0.

### BUG-004 — Inconsistências de valores entre Mapa de Vendas, Apuramento de IVA, Dashboard e Caixa
- **Severidade:** CRÍTICO · **Prioridade:** P0
- **Módulo:** Relatórios / Dashboard / Fiscal
- **Telas:** Relatórios → Mapa Geral de Vendas (`ReportsController.loadVendasData`), Apuramento de IVA (`loadIvaData`), Dashboard (`DashboardKpiManager.loadStats`), Caixa
- **Service/Repository:** ReportService, SaleRepository
- **Tabelas:** `sales`
- **Descrição:** quatro agregações do "mesmo" conceito usam filtros diferentes:
  1. `loadVendasData`: `findByDateRangeAndState(from,to,null)` → **sem filtro de estado** (inclui ANULADA, COTAÇÃO, ENCOMENDA, demoFlag=true);
  2. `loadIvaData`: exclui `ANULADA`, **inclui** cotações/encomendas/demo; categoriza base tributável por documento (`sTax > 0.001`) → documento com linhas isentas+tributadas classifica o subtotal todo como tributável;
  3. `DashboardKpiManager`: `state <> 'ANULADA'` (inclui cotações/encomendas e demo);
  4. Caixa: movimentos IN de vendas de **todos** os métodos (ver BUG-003).
- **Comportamento esperado:** um único conjunto de regras (ex.: vendas ≠ ANULADA e ≠ demo e ≠ COTAÇÃO/ENCOMENDA) partilhado em todas as agregações; base tributável calculada por linha.
- **Comportamento atual:** num dia com 1 venda cash 100 MT, 1 cotação 500 MT e 1 venda anulada 300 MT: Mapa de Vendas = 900 MT (3 docs); Apuramento = 600 MT (2 docs); Dashboard = 600 MT; Caixa (se a anulada e a cotação tivessem movido caixa, moveram na altura) — e o IVA da base isenta está mal alocado.
- **Passos para reproduzir:** criar os 3 documentos acima; comparar os 3 ecrãs.
- **Dados utilizados:** qualquer.
- **Causa provável:** agregações escritas independentemente; sem constante/enum de "venda válida para relatórios".
- **Evidência no código:** `ReportsController.loadVendasData` (L282–296: `findByDateRangeAndState(fromDt, toDt, null)` + somas sem filtro); `loadIvaData` (L581–615: filtro só `ANULADA`; `if (sTax > 0.001) baseTributavel += sSub`); `DashboardKpiManager.loadStats` (L88–95: `sumTotalByDateRangeAndBranch` = `state <> 'ANULADA'`); `SaleRepository.sumTotalByDateRangeAndBranch` (sem filtro `demoFlag`).
- **Evidência durante execução:** NÃO TESTADA (determinístico por código).
- **Impacto:** **falha fiscal potencial** (apuração de IVA declarável errada) e decisão de gestão sobre números divergentes; auditoria AT impossível de reconciliar.
- **Dependências afetadas:** Relatórios, Dashboard, IVA, SAF-T (exporta o que a UI mostra), Caixa.
- **Correção recomendada:** criar `SaleQuerySupport` (JPA spec ou predicates) com `isReportable()` (state IN VENDA-emitidas: PAGO, EMITIDA, PAGO_PARCIAL; docType IN VENDA, FACTURA, RECIBO, NC; demoFlag=false; state<>ANULADA conforme o mapa); usar em todas as queries (KPIs, mapas, IVA, SAF-T, reconciliação); base tributável/ISENTO por **linha** (`sale_items.line_base` onde `tax_rate>0`); teste de paridade: para o mesmo período, dashboard = mapa = IVA = suma de `sales` com o predicado.
- **Prioridade:** P0.

### BUG-005 — Extrato do cliente diverge do saldo real (e a NC não abate a dívida)
- **Severidade:** CRÍTICO · **Prioridade:** P0
- **Módulo:** Clientes / Contas Correntes
- **Telas:** Detalhe do Cliente → Extrato (`CustomerAccountService.getCustomerStatement`); liquidação via `recordReceipt`/`reconcileCustomerCredits`
- **Controller:** DashboardCrudManager (action de extrato) · **Service:** CustomerAccountService
- **Tabelas:** `customers.balance`, `sales`, `payments`, `payment_allocation`
- **Descrição:** três quebras:
  1. `createCreditNote` (SaleService) **nunca altera `customer.balance`** — mas o extrato trata NC como abate (`runningBalance -= total.abs()`);
  2. `recordReceipt` **sem factura** abate o saldo real (`balance -= amount`) mas o extrato (derivado só de `sales`) **não mostra o pagamento** → saldo real ≠ extrato;
  3. COTAÇÃO/ENCOMENDA do cliente entram no extrato como "Venda a Pronto" (branch `else`) e poluem a contagem.
- **Comportamento esperado:** extrato = reconstrução fiel de `customers.balance` (todas as operações que tocam o saldo devem aparecer 1:1).
- **Comportamento atual:** após venda a crédito 1.000 + NC 400: saldo real = 1.000, extrato termina em 600. Após pagamento sem fatura 500: saldo real = 500, extrato termina em 1.000.
- **Passos para reproduzir:** (a) venda a crédito; (b) emitir NC do histórico; (c) abrir extrato → divergência.
- **Dados utilizados:** cliente com limite de crédito.
- **Causa provável:** extrato reconstruído de `sales` em vez de ser o ledger próprio (event-sourcing da conta corrente) ou de incluir `payments` sem venda e `NC` real.
- **Evidência no código:** `CustomerAccountService.getCustomerStatement` (L187–250: branch NC abate o running balance; não itera `paymentRepository`); `SaleService.createCreditNote` (L252–371: sem referência a `customerRepository`); `recordReceipt` (L49–120: `balance -= amount` antes/sem venda).
- **Evidência durante execução:** NÃO TESTADA (determinístico).
- **Impacto:** **dívida real nunca reconhecível** pelo cliente nem pelo operador; reconciliação de créditos impossível; conflito comercial e risco de cobrança errada.
- **Dependências afetadas:** Contas Correntes, Reconciliação, Relatórios de devedores, Pagamentos.
- **Correção recomendada:** ledger de conta corrente (tabela `customer_account_entries` escrita por cada operação que mexe no saldo: FT crédito, pagamento, NC, estorno, reconciliação — com FK ao documento); extrato = leitura do ledger; `createCreditNote` deve, se a origem foi CREDITO, abater `balance` (e caixa OUT se cash); excluir COTAÇÃO/ENCOMENDA do extrato; teste de invariante: `SUM(ledger) == customers.balance` para todos os clientes.
- **Prioridade:** P0.

### BUG-007 — Cotações e encomendas na fila de recebimentos (pagáveis) e nos KPIs de crédito
- **Severidade:** CRÍTICO · **Prioridade:** P0
- **Módulo:** Financeiro / Pagamentos
- **Tela:** Financeiro → Pagamentos (`payment_form.fxml`, lista via `paymentService.findPendingSales()`)
- **Controller:** PaymentFormController · **Service:** PaymentService · **Repository:** SaleRepository.findPendingSales
- **Tabelas:** `sales`, `payments`, `cash_movements`
- **Descrição:** `findPendingSales = state <> 'ANULADA' AND (total − paidAmount) > 0.01` → COTAÇÃO (`COTACAO_ABERTA`, paid=0) e ENCOMENDA aparecem na lista. `createPayment` aceita qualquer `Sale` com pendente > 0: cria `Payment`, `PaymentAllocation`, movimento de caixa IN, e passa o estado da **cotação** a `PAGO`/`PAGO_PARCIAL` — sem baixa de stock (cotação nunca abateu stock) e sem nunca ter existido dívida.
- **Comportamento esperado:** só documentos de dívida (VENDA/FACTURA a crédito ou com recebido parcial) devem ser liquidáveis; cotação deve ser **convertida** em factura (fluxo existe: `originSale` → `COTACAO_PAGA`).
- **Comportamento atual:** o operador pode "pagar" uma cotação; o caixa recebe o valor; a cotação fica `PAGO`; o stock nunca baixa; "Contas a Receber" (KPI dashboard via `sumTotalPendingCreditsByBranch`) e o aging de devedores **sombream o valor das cotações**.
- **Passos para reproduzir:** emitir cotação 500 MT (cliente X) → abrir Pagamentos → a cotação aparece → liquidar 500 → `payments`+1, `cash_movements` IN 500, cotação `PAGO`; stock do artigo intacto.
- **Dados utilizados:** qualquer.
- **Causa provável:** "pendente" definido por aritmética `total-paid` sem considerar o tipo de documento.
- **Evidência no código:** `SaleRepository.findPendingSales` (L141–142) e `findPendingByCustomerId` (L135–136, mesmo padrão — afecta também a reconciliação FIFO, ver BUG-015); `PaymentService.createPayment` (sem filtro de `documentType`).
- **Evidência durante execução:** NÃO TESTADA (determinístico).
- **Impacto:** **dinheiro recebido "a contas" sem dívida correspondente** (conta a receber fictícia); caixa com entradas sem venda; KPIs de crédito inflados; conversão cotação→factura contornável por erro humano.
- **Dependências afetadas:** Pagamentos, Caixa, KPI "Contas a Receber", Aging, Reconciliação, Extrato.
- **Correção recomendada:** `findPendingSales` restringir a `documentType IN (VENDA, FACTURA) AND paymentMethod='CREDITO' OR (paidAmount>0 AND paidAmount<total)`; em `createPayment` validar `documentType` e rejeitar COTAÇÃO/ENCOMENDA com mensagem "converta primeiro em factura"; teste: cotação não aparece na lista de recebimentos.
- **Prioridade:** P0.

### BUG-006 — DataInitializer: re-activa utilizadores desactivados, senha admin/admin, venda demo "real"
- **Severidade:** CRÍTICO · **Prioridade:** P0
- **Módulo:** Arranque / Segurança
- **Controller/Config:** `DataInitializer` (CommandLineRunner)
- **Tabelas:** `users`, `roles`, `sales`, `products`, `stock_branch`, `branches`
- **Descrição:** A cada arranque: (1) `ensureUser("admin","admin",…)` e no ramo de **utilizador existente** faz `user.setActive(true)` → **desactivar o admin (ou qualquer utilizador assegurado) é revertido no próximo arranque**; (2) se `passwordHash` vazio → reescreve `admin`; (3) `ensureDefaultSales()` cria a "primeira venda de demonstração" **sem `demoFlag`** → conta como venda real em todos os relatórios que filtram apenas `ANULADA`; (4) também reactiva `canViewStats` e adiciona o papel ADMIN se faltar.
- **Comportamento esperado:** seeds apenas quando a BD está vazia; um utilizador desactivado permanece desactivado; dados de demonstração marcados `demoFlag=true` ou nunca criados em BD com dados reais.
- **Comportamento actual:** como descrito — **controlo de acesso e integridade financeira comprometidos por efeito de arranque**.
- **Passos para reproduzir:** desactivar utilizador no menu Utilizadores → reiniciar a app → utilizador activo de novo.
- **Dados utilizados:** qualquer instalação.
- **Causa provável:** lógica "ensure" (idempotência) sem distinguir "seed inicial" de "manutenção".
- **Evidência no código:** `DataInitializer.ensureUser` (L120–150: `user.setActive(true); user.setCanViewStats(true);` incondicional) e `ensureDefaultSales` (L342–395: `sale.setState("PAGO"); … saleRepository.save(sale);` sem `demoFlag`).
- **Evidência durante execução:** NÃO TESTADA (determinístico).
- **Impacto:** **segurança** (revogação de acesso ineficaz) + **financeiro** (venda fantasma nº1 em todos os mapas/IVA) + credencial default `admin/admin`.
- **Dependências afectadas:** Segurança/RBAC, Relatórios, IVA, Auditoria.
- **Correção recomendada:** separar `seedEmptyDatabase()` (só se `users.count()==0 && sales.count()==0`) de `ensureRoles/units` (idempotente seguro); nunca `setActive(true)` sobre estado existente; marcar demo (`demoFlag=true`) ou remover a venda de seed; forçar alteração de password de `admin` no primeiro login (ligar ao `forceChangePassword` — ver BUG-021); teste: desactivar→restart→continua inactivo.
- **Prioridade:** P0.

### BUG-011 — Anulação de venda: estorno de caixa engolido em silêncio + turno errado
- **Severidade:** ALTO · **Prioridade:** P1
- **Módulo:** Vendas / Tesouraria
- **Tela:** Histórico de Vendas → Anular (`DashboardCrudManager.annulSelectedSale`)
- **Service:** SaleService.annulSale → CashSessionService.registerMovement
- **Tabelas:** `cash_movements`, `stock_branch`, `customers`, `sales`
- **Descrição:** Em anulação de venda **cash**, o estorno `registerMovement(OUT, total, …)` está dentro de `try { … } catch (Exception ignored) { // Se o operador não tiver sessão aberta … prossegue }`. Falha (sem turno aberto, turno de outro dia, sem permissão) → **anulação completa com stock devolvido e caixa não debitada**. Além disso, o OUT é lançado no turno **do utilizador que anula**, não no turno **da venda original** (o movimento original IN está no turno de outro operador) — mesmo quando há turno aberto.
- **Comportamento esperado:** estorno atómico com a anulação (mesma transacção, sem `catch`); lançamento no turno original quando possível, ou em "estornos" do turno actual com referência inequívoca; aviso se não for possível.
- **Comportamento actual:** inconsistência caixa↔vendas permanente, invisível (o `ignored` nem log deixa); fita Z divergente.
- **Passos para reproduzir:** venda cash no turno do operador A; operador B (sem turno aberto) anula a venda → stock devolvido, `sales.state=ANULADA`, `cash_movements` **sem** OUT → caixa do turno A sobra o valor da venda.
- **Dados utilizados:** dois operadores.
- **Causa provável:** decisão de "não bloquear a anulação fiscal" aplicada à tesouraria (que não pode ser assimétrica).
- **Evidência no código:** `SaleService.annulSale` (L415–431: `catch (Exception ignored)`); o movimento de venda original usa o turno do operador da venda (`persistSaleTransaction` L213–228).
- **Evidência durante execução:** NÃO TESTADA (determinístico).
- **Impacto:** quebra de caixa sistémica a cada anulação feita por operador sem turno; auditoria de tesouraria impossível.
- **Dependências afectadas:** Caixa, Fecho/Fita Z, Auditoria, Stock (devolvido), Relatórios.
- **Correção recomendada:** remover o `catch` (falha → rollback da anulação, com mensagem clara "abra/feche o turno X para estornar"), ou estornar no turno original se aberto (FK `session_id` do movimento original — guardar `cash_session_id` na venda); log `systemLogService.logError` obrigatório se o estorno não for possível; teste E2E: anular com e sem turno.
- **Prioridade:** P1.

### BUG-010 — Nota de Crédito: não abate dívida do cliente nem caixa; sem hash AT
- **Severidade:** ALTO · **Prioridade:** P1
- **Módulo:** Vendas / Fiscal
- **Tela:** Histórico → "Emitir Nota de Crédito" (`DashboardCrudManager.createCreditNoteFromSelectedSale`)
- **Service:** SaleService.createCreditNote
- **Tabelas:** `sales` (NC), `stock_branch`, `customers` (não tocada), `cash_movements` (não tocada)
- **Descrição:** A NC devolve stock (loop `increaseStock` com `DEVOLUCAO`), mas: (1) se a factura original era **a crédito**, `customer.balance` **não é abatonada** (a dívida do cliente nunca diminui — o único abate é o extrato fictício do BUG-005); (2) se era **cash**, não há `registerMovement(OUT, …)` (o dinheiro devolvido não sai do caixa); (3) a NC não recebe `hashHash`/`signatureHash`/`hashControl` (documento fiscal incompleto no padrão da app) e o `paidAmount` fica 0.
- **Comportamento esperado:** NC = estorno simétrico da operação original: stock ✓ (já existe), caixa (OUT) para cash, conta corrente (abate) para crédito, hash AT, auditoria.
- **Comportamento actual:** estorno de 1/2 ou 1/3 — depende do método da venda original.
- **Passos para reproduzir:** venda a crédito 800 MT → NC 800 → `customers.balance` continua +800; caixa sem movimento; extrato (errado) mostra 0.
- **Dados utilizados:** qualquer.
- **Causa provável:** NC implementada como "documento negativo" sem replicar os efeitos colaterais de `annulSale`.
- **Evidência no código:** `SaleService.createCreditNote` (L252–371: só `stockBranchService.increaseStock`; nenhum `customerRepository`/`cashSessionService`).
- **Evidência durante execução:** NÃO TESTADA (determinístico).
- **Impacto:** dívidas que nunca caem com devoluções; caixa com dinheiro "a mais" não registrado; NC fiscalmente incompleta.
- **Dependências afectadas:** Contas Correntes, Caixa, Extrato, Auditoria, Fiscal.
- **Correção recomendada:** em `createCreditNote`: se `originSale.paymentMethod='CREDITO'` → `balance -= |total|` (com lock pessimista); se cash → `registerMovement(OUT, |total|, "NC-…", REFUND)` sem `catch` engolido; gerar `hashHash`/`signatureHash`/`hashControl` como nas vendas; log de auditoria com antes/depois; teste: ciclo FT crédito → NC → saldo volta ao anterior.
- **Prioridade:** P1.

### BUG-012 — Pagamentos: sem RBAC, race de duplo pagamento, recibo sem série própria, conflito com trigger fiscal
- **Severidade:** ALTO · **Prioridade:** P1
- **Módulo:** Financeiro / Pagamentos
- **Tela:** Financeiro → Pagamentos (`payment_form.fxml`)
- **Controller:** PaymentFormController · **Service:** PaymentService.createPayment
- **Tabelas:** `payments`, `payment_allocation`, `sales`, `customers`, `cash_movements`
- **Descrição:** (1) **Sem verificação de permissão** no service (qualquer chamada com permissão de ver ver o serviço executar a gravação); (2) `Sale` sem `@Version` → 2 pagamentos simultâneos à mesma FT leem o mesmo `paidAmount`, passam ambos a validação e gravam ambos (pago em dobro, 2× IN caixa, 2× abate de saldo); (3) o "recibo" é gerado **mudando `documentType` da factura para `RECIBO`** em memória (`try { setDocumentType(RECIBO); pdf=… } finally { setDocumentType(orig) }`) — se o Hibernate fizer flush entre a mudança e o restore (qualquer query de `generateDocument` pode dispará-lo), o trigger `trg_prevent_sales_update` dispara `SIGNAL 45000` e o pagamento **falha com erro SQL**; (4) mesmo que funcione, o recibo usa **número/série da FT** — não consome numeração da série RECIBO (não conforme; a numeração `series_counters` tem suporte para RECIBO que nunca é usado neste fluxo); (5) pagamento por M-Pesa/POS também cai no caixa físico (`registerMovement(IN, …)` sem distinguir método — mesmo anti-padrão do BUG-003).
- **Comportamento esperado:** RBAC (`FINANCEIRO:CREATE`/`PAGAMENTOS:CREATE`), lock da factura (`@Version` ou `FOR UPDATE`), recibo com série/número próprios (ou recibo AT com o próprio hash), caixa só para numerário.
- **Comportamento actual:** como descrito.
- **Passos para reproduzir:** (a) 2 separadores/threads pagando a mesma FT 500 MT com 500 cada → os dois passam `pendingBalance=500`; (b) pagamento que force flush → `SIGNAL SQLSTATE 45000`.
- **Dados utilizados:** FT a crédito com pendente.
- **Causa provável:** implementação rápida sem lock; recibo "emulado" mudando o tipo do documento.
- **Evidência no código:** `PaymentService.createPayment` (L53–130: sem `requirePermission`; `try{ setDocumentType(RECIBO) … } finally { restore }`; `registerMovement(IN,…)` para qualquer método); `DbTriggerConfig.trg_prevent_sales_update` (protege `document_type`).
- **Evidência durante execução:** NÃO TESTADA (o risco do flush é dependente de timing Hibernate — **teste em execução obrigatório**).
- **Impacto:** **double spending real** sob concorrência; não-conformidade documental (recibo sem numeração própria); falhas intermitentes de pagamento (500).
- **Dependências afectadas:** Caixa, Contas Correntes, Fiscal, Auditoria.
- **Correção recomendada:** `@Version` em `Sale` (+ retry com mensagem "documento mudou, recarregue"); `PessimisticLock` no `findById` do pagamento; RBAC no service; recibo como **entidade própria** (série RB/numeração própria via `SaleNumberingService`, com hash próprio) que referencia a FT; caixa só para DINHEIRO; teste de concorrência (2 threads).
- **Prioridade:** P1.

### BUG-008 — Troco e valor recebido do PDV descartados pela camada de serviço
- **Severidade:** ALTO · **Prioridade:** P1
- **Módulo:** PDV
- **Tela:** PDV → painel "Gaveta" (`sale_form.fxml`: `receivedAmountField`, `changeAmountLabel`)
- **Controller:** SaleFormController.doSave · **Service:** SaleService.persistSaleTransaction
- **Tabelas:** `sales.paid_amount`, `sales.change_amount`
- **Descrição:** O PDV calcula troco e grava `sale.setPaidAmount(receivedVal)` / `setChangeAmount(changeVal)`. No serviço, para qualquer venda não-crédito: `setPaidAmountValue(totalAmount); setChangeAmountValue(ZERO)` — **sobrescreve** os valores da UI. O troco real (ex.: cliente deu 500, troco 137.40) nunca persiste.
- **Comportamento esperado:** persistir `received` e `change` tal como operado (útil para reconciliação de gaveta e auditoria de trocos).
- **Comportamento actual:** BD sempre com `paid=total, change=0` independentemente do que foi digitado.
- **Passos para reproduzir:** venda 362.60; digitar recebido 500 → documento impresso mostra o troco (calc UI); consultar `sales` → `paid_amount=362.60, change_amount=0`.
- **Dados utilizados:** qualquer.
- **Causa provável:** serviço normaliza totais sem considerar o par received/change.
- **Evidência no código:** `SaleService.persistSaleTransaction` (L131–138: `else { setPaidAmountValue(totalAmount); setChangeAmountValue(ZERO); }`); `SaleFormController.doSave` (L1548–1556: `sale.setPaidAmount(receivedVal); sale.setChangeAmount(changeVal);`).
- **Evidência durante execução:** NÃO TESTADA (determinístico).
- **Impacto:** auditoria de gaveta/trocos impossível; documento (com troco) ≠ BD (sem troco) — divergência documento/BD.
- **Dependências afectadas:** Auditoria, Documentos, Tesouraria.
- **Correção recomendada:** se `receivedVal > 0` e método=DINHEIRO, persistir `paidAmount=received, changeAmount=received−total` (validar `received ≥ total`); caso contrário manter `paid=total, change=0`; testar os dois casos.
- **Prioridade:** P1.

### BUG-009 — Venda a crédito com estado "PAGO"
- **Severidade:** ALTO · **Prioridade:** P1
- **Módulo:** Vendas
- **Tela:** PDV
- **Service:** SaleService.persistSaleTransaction (applyDocumentState); SaleFormController.doSave
- **Tabelas:** `sales.state`
- **Descrição:** `applyDocumentState`: `VENDA, FACTURA → PAGO` **independentemente do método de pagamento**. Uma factura a crédito (pagamento futuro) nasce `PAGO`. Todo o sistema "pendente" depende de `paidAmount=0` — e o enum `SaleState` **não tem** `PAGO_PARCIAL` (usado como string solta por `PaymentService`/`CustomerAccountService`), nem `EMITIDA_A_CREDITO`/`DEVIDA`.
- **Comportamento esperado:** crédito → estado `EMITIDA` (ou `PAGO_PARCIAL` quando houver pagamento parcial); `PAGO` só quando `paidAmount ≥ total`.
- **Comportamento actual:** histórico mostra faturas em dívida como "PAGO"; relatórios/filtros por estado dão resultados enganosos; o estado só se "corrigir" no primeiro pagamento parcial.
- **Passos para reproduzir:** venda a crédito 500 → histórico: estado PAGO, pendente 500.
- **Dados utilizados:** cliente com limite.
- **Causa provável:** `applyDocumentState` confunde "documento emitido" com "venda liquidada".
- **Evidência no código:** `SaleService.applyDocumentState` (L478–492: `case VENDA, FACTURA -> sale.setState(SaleState.PAGO.name());`); `SaleState` (sem PAGO_PARCIAL); `SaleFormController.doSave` (L1590–1596: `sale.setState(PAGO)` para VENDA/FACTURA).
- **Evidência durante execução:** NÃO TESTADA (determinístico).
- **Impacto:** semântica de estados quebrada em todo o sistema (histórico, filtros, dashboard, reconciliação); risco de decisões sobre dívidas.
- **Dependências afectadas:** Históricos, Filtros, Contas Correntes, Reconciliação, Relatórios.
- **Correção recomendada:** em `applyDocumentState`/`doSave`: `paymentMethod=CREDITO → EMITIDA` (e `PAGO_PARCIAL` após pagamento < total); adicionar `PAGO_PARCIAL` ao enum `SaleState`; actualizar triggers/filtros que assumem o conjunto de estados; teste: venda a crédito → EMITIDA; parcial → PAGO_PARCIAL; total → PAGO.
- **Prioridade:** P1.

### BUG-013 — Duplo stock: compras (armazém) vs vendas (filial) sem ponte obrigatória
- **Severidade:** ALTO · **Prioridade:** P1
- **Módulo:** Stock / Compras / Vendas
- **Telas:** Compras (grava em `stock_warehouse`), PDV (consome `stock_branch`), Stock Central vs Inventário
- **Services:** PurchaseService → WarehouseService.addStock; SaleService → StockBranchService; ponte opcional: WarehouseTransferService
- **Tabelas:** `stock_warehouse`, `stock_branch`, `stock_movements`
- **Descrição:** o modelo é **bifronte**: compra ⇒ `ENTRADA_ARMAZEM` (stock_warehouse); venda ⇒ `SAIDA` (stock_branch). A ponte armazém→filial (guia TWA) **existe mas é opcional e desconhecida do fluxo natural** (compra grava e "termina"). Consequências: (a) artigo comprado mas nunca transferido **não é vendável** no PDV (stock filial 0 → bloqueado); (b) "Stock Central" + "Inventário (filial)" não somam o total real (há um stock "de passagem" que não existe — o mesmo artigo pode aparecer em ambos); (c) o Kardex unificado (`stock_movements` com `branch_id` ou `warehouse_id`) esconde a fronteira — o saldo por artigo = `stock_branch + stock_warehouse` exige 2 tabelas que a UI nunca agrega; (d) `Product.lastMovementAt` é actualizado apenas pelas operações de armazém.
- **Comportamento esperado:** uma fronteira única e visível: ou "compra entra directamente na filial de destino", ou "compra entra no armazém e a transferência é etapa obrigatória e visível" (com alerta "X artigos no armazém por transferir").
- **Comportamento actual:** fluxo compra→venda **interrompido** a menos que o operador saiba e use TWA.
- **Passos para reproduzir:** comprar 100×P001 → abrir PDV → P001 "Sem stock" (filial vazia) apesar de o Stock Central mostrar 100.
- **Dados utilizados:** seed + compra.
- **Causa provável:** evolução incremental (armazéns V3) sobre um modelo já existente de stock por filial, sem unificar.
- **Evidência no código:** `PurchaseService.savePurchase` (L120–170: `warehouseService.addStock(targetWarehouse…)`); `SaleService.persistSaleTransaction` (L172–212: `stockBranchService…`); `WarehouseTransferService` (a ponte, opcional); `StockFlowIntegrationTest.comprasEntramArmazem_transferenciaMoveParaLoja_vendaConsome` (confirma o design).
- **Evidência durante execução:** NÃO TESTADA (determinístico).
- **Impacto:** **venda bloqueada após compra** no uso natural; inventário total não calculável na UI; risco operacional alto em lojas com armazém.
- **Dependências afectadas:** Compras, Stock, PDV, Relatórios de stock, Kardex.
- **Correção recomendada:** (mínimo) no formulário de compra, escolher "destino: filial (armazém anexo)" vs "armazém central" e, no primeiro caso, gravar directamente em `stock_branch`; (melhor) unificar `stock_branch`/`stock_warehouse` num único `stock_locations` com tipo, e tornar a transferência obrigatória com painel "por transferir"; teste E2E compra→(sem transferência)→venda deve dar resultado coerente e documentado.
- **Prioridade:** P1.

### BUG-016 — Cadeia de hashes AT incorrecta, hashHash simplificado e QR Code inexistente
- **Severidade:** ALTO · **Prioridade:** P1
- **Módulo:** Fiscal / Documentos
- **Service:** FiscalService · **Documents:** SaleDocumentService, ThermalPrintService
- **Tabelas:** `sales.hash_hash`, `signature_hash`, `hash_control`, `qr_code`
- **Descrição:** (1) `generateSignatureHash` obtém o "previous hash" via `findByBranchIdOrderByCreatedAtDesc(branchId, 1)` → **a última venda por data e de todas as séries** (não a anterior da **mesma série/hashControl**): duas séries (A-VENDA, B-FACTURA) entrelaçadas quebram a cadeia; vendas no mesmo segundo (ou com `createdAt` igual) escolhem o previous errado; (2) `hashHash = sha256(branchId|series|num|total)` — não corresponde ao cálculo AT Moçambique (que inclui NUIT, taxas, datas, campos do QR); (3) **`qrCode` nunca é populado** (grep: apenas getters/setters e uso condicional `if (qrCode != null)` nos impressores) → nenhum documento tem QR; (4) NC sem hash (BUG-010).
- **Comportamento esperado:** cadeia por (filial, série, hashControl) em ordem sequencial; hash conforme especificação AT; QR gerado e impresso.
- **Comportamento actual:** cadeia quebrável; documento sem QR; "conformidade AT" declarada (labels, certificações, hashes) mas não implementada.
- **Passos para reproduzir:** emitir FT série A, depois VENDA série A, depois FT série B (mesma filial) → o previous hash da 2ª FT é o da VENDA (série A), não o da 1ª FT.
- **Dados utilizados:** qualquer.
- **Causa provável:** implementação simplificada sem a spec AT; QR deixado para "integração futura".
- **Evidência no código:** `FiscalService.generateSignatureHash` (L40–66: `findByBranchIdOrderByCreatedAtDesc`); `generateHashHash` (L24–33); grep `setQrCode` → **zero chamadores** em `main`.
- **Evidência durante execução:** NÃO TESTADA (determinístico).
- **Impacto:** **risco fiscal directo** (facturação electrónica MZ exige QR + hash encadeado verificável pela AT); auditoria de fraude (`hashAudit`) baseada em premissas erradas.
- **Dependências afectadas:** Fiscal, Documentos, SAF-T, Auditoria de hashes.
- **Correção recomendada:** previous hash por `WHERE branch=? AND series=? AND hash_control < ? ORDER BY hash_control DESC LIMIT 1`; gerar `hashHash`/QR conforme spec AT (ou declarar explicitamente o modo "pré-conformidade"); gerar QR (PDFBox `write2DMatrix`) e imprimi-lo; teste: sequência de 3 docs na mesma série → cadeia verificável; docs de 2 séries independentes.
- **Prioridade:** P1.

### BUG-017 — Transferências filial→filial: service sem RBAC e numeração não atómica
- **Severidade:** ALTO · **Prioridade:** P1
- **Módulo:** Transferências
- **Service:** TransferService
- **Tabelas:** `transfers`, `transfer_items`, `stock_branch`, `stock_movements`
- **Descrição:** `create`, `approve`, `reject`, `complete`, `cancel` **não chamam `requirePermission`** (ao contrário de `WarehouseTransferService`, que verifica `TRANSFERENCIAS:CREATE/VIEW`). Qualquer utilizador autenticado (papel CAIXA) pode, via UI se a navegação expuser, ou via qualquer chamador do service, mover stock entre filiais. Numeração: `findMaxDocumentNumberBySeriesAndYearAndBranch + 1` **sem lock** (ao contrário das vendas que usam `series_counters` FOR UPDATE) → 2 transferências simultâneas na mesma filial/série → mesmo número (unique constraint só existe em `sales`; `transfers` não tem constraint único de série/número!).
- **Comportamento esperado:** RBAC por operação (CREATE para emitir, CREATE/APPROVE para processar) e numeração atómica com constraint único.
- **Comportamento actual:** sem RBAC a nível de serviço; duplicados possíveis.
- **Passos para reproduzir:** (RBAC) iniciar sessão com papel CAIXA → invocar o fluxo de transferências; (numeração) 2 criações em paralelo na mesma filial.
- **Dados utilizados:** 2 filiais.
- **Causa provável:** módulo mais recente sem passar pelo mesmo hardening.
- **Evidência no código:** `TransferService` (todo o ficheiro: 0 ocorrências de `requirePermission`); `create` (L56–70: `maxNum + 1`); `transfers` sem unique constraint em `V1__baseline.sql`.
- **Evidência durante execução:** NÃO TESTADA.
- **Impacto:** **movimentação de stock não autorizada**; duplicados de documento.
- **Dependências afectadas:** Stock, Auditoria, RBAC.
- **Correção recomendada:** `requirePermission("TRANSFERENCIAS", "CREATE"/"VIEW")` em cada método (estilo WarehouseTransferService); constraint `UNIQUE(series, document_number, document_year, source_branch_id)` + reuso de `series_counters`; teste de permissão com papel CAIXA (deve falhar).
- **Prioridade:** P1.

### BUG-021 — `forceChangePassword` nunca imposto no login
- **Severidade:** ALTO · **Prioridade:** P1
- **Módulo:** Segurança / Utilizadores
- **Tela:** Login (`login.fxml`) e Utilizadores (checkbox "forçar alteração")
- **Service:** DesktopAuthService.authenticate · **Controller:** LoginController
- **Tabelas:** `users.force_change_password`
- **Descrição:** o flag existe (entidade + UI de gestão de utilizadores), mas `authenticate()` só verifica existência, `active` e BCrypt. Utilizador com password forçada **entra normalmente com a password antiga**; nada o obriga a alterá-la; o flag nunca é resetado por lógica de login.
- **Comportamento esperado:** login com `forceChangePassword=true` → sessão restrita **só** para a mudança de password; flag resetado após.
- **Comportamento actual:** flag ineficaz (muito provavelmente nunca activo em produção — o formulário seta, mas o efeito é nulo).
- **Passos para reproduzir:** criar utilizador com forçar=true → login com a password inicial → passa.
- **Dados utilizados:** qualquer.
- **Causa provável:** feature a meio caminho.
- **Evidência no código:** `DesktopAuthService.authenticate` (L25–50: sem referência a `forceChangePassword`); `User.forceChangePassword` (entidade, sem consumidores).
- **Evidência durante execução:** NÃO TESTADA (determinístico).
- **Impacto:** política de password após criação/reset inejecutável → risco de credenciais partilhadas/default.
- **Dependências afectadas:** Segurança, Utilizadores.
- **Correção recomendada:** em `LoginController.doLogin`, se `user.isForceChangePassword()` → abrir `SystemSettingsPageManager.buildSegurancaPane` obrigatória (bloquear resto da app) e desmarcar o flag após alteração; teste: forçar → login → apenas ecrã de password.
- **Prioridade:** P1.

### BUG-027 — Licenciamento: forjável, sem binding a máquina, sem efeito restritivo
- **Severidade:** ALTO · **Prioridade:** P1
- **Módulo:** Sistema → Licenciamento
- **Service:** LicenseService · **Controller:** SystemSettingsPageManager.buildLicencaPane, MainApp.checkLicenseStartup
- **Tabelas:** `app_config.license_*`
- **Descrição:** (1) `SECRET_SALT = "SGV-SECRET-2024-MOZ"` **no source code** e `generateTrialLicense/generateAnnualLicense/generateLifetimeLicense` **públicos no próprio client** → qualquer pessoa com o JAR gera chaves vitalícias válidas; (2) a assinatura **não inclui o machineId** (o machineId é apenas exibido) → chave de uma máquina funciona em todas; (3) sem chave → `LicenseResult(valid=true, "Trial", daysRemaining=MAX_VALUE)` → **avaliação ilimitada** (o "trial 30 dias" existe só no gerador, não na validação); (4) `checkLicenseStartup` apenas mostra um aviso — a aplicação **não é bloqueada** em expirado.
- **Comportamento esperado:** chaves emitidas por servidor com salt secreto; binding por machineId; expiração bloqueia escrita (ou toda a app); trial com prazo real.
- **Comportamento actual:** licenciamento decorativo.
- **Passos para reproduzir:** descompilar/JShell sobre o JAR → `licenseService.generateLifetimeLicense()` → chave válida para sempre em qualquer máquina.
- **Dados utilizados:** qualquer.
- **Causa provável:** MVP sem backend de licenciamento.
- **Evidência no código:** `LicenseService` (L27 salt; L60–88 geradores públicos; L96–140 validação sem machineId; L152–165 trial ilimitado sem chave); `MainApp.checkLicenseStartup` (L345–371: só `SgvDialog.warning`).
- **Evidência durante execução:** NÃO TESTADA (determinístico).
- **Impacto:** **receita/licenciamento comercial não defendível**; o módulo dá falsa garantia ao dono.
- **Dependências afectadas:** Business, UI.
- **Correção recomendada:** emitir chaves em servidor (salt server-side); incluir `machineId` no payload assinado; bloqueio em expirado (escrever) com modo "só leitura"; trial = data de 1ª utilização + 30 dias persistida; remover os geradores do client (ou manter atrás de perfil `dev`); teste: chave de máquina A rejeitada em B.
- **Prioridade:** P1.

### BUG-033 — Concorrência: lost updates em vendas de crédito, pagamentos e edições
- **Severidade:** ALTO · **Prioridade:** P1
- **Módulo:** Transversal (Vendas, Clientes, Compras)
- **Services:** SaleService, PaymentService, CustomerAccountService, SupplierPaymentService
- **Tabelas:** `sales`, `customers`, `purchases`, `supplier_payments`
- **Descrição:** `@Version` existe apenas em `StockBranch`/`StockWarehouse` (V25). `Sale`, `Customer`, `Purchase` não têm lock optimista:
  - 2 PDVs simultâneos com o mesmo cliente a crédito → ambos leem `balance`, ambos validam o limite (`balance + total ≤ limit`), ambos gravam `balance += total` → **perda do limite** e saldo errado (lost update);
  - 2 pagamentos simultâneos à mesma FT → ambos leem `paidAmount`, ambos passam a validação → **pago em dobro** (ver BUG-012);
  - 2 utilizadores a editar o mesmo cliente/produto → o último grava por cima (lost update de campos);
  - stock: com `@Version`, o conflito lança `OptimisticLockException` → **500 cru na UI** (sem retry, sem mensagem amigável) — a venda falha (correcto em princípio) mas a UX é "Erro ao salvar a operação: <stack>".
- **Comportamento esperado:** lost update impossível (lock optimista com retry/mensagem) ou pessimista (FOR UPDATE) nas entidades financeiras; mensagem amigável em conflito de stock.
- **Comportamento actual:** como descrito.
- **Passos para reproduzir:** 2 instâncias/concorrência: (a) limite 10.000, dívida 9.500, 2 vendas de 1.000 simultâneas → ambas passam; (b) 2 pagamentos de 500 a FT 500 simultâneos → 1.000 pagos.
- **Dados utilizados:** multi-operador (ou testes de threads).
- **Causa provável:** JPA sem `@Version` nas entidades financeiras; UI single-machine mas multi-operador na mesma BD.
- **Evidência no código:** `StockBranch`/`StockWarehouse` têm `@Version` (V25 + entidades); `Sale`/`Customer`/`Purchase` **não**; validações read-modify-write em `SaleService.validateSaleForPersistence` e `PaymentService.createPayment`.
- **Evidência durante execução:** NÃO TESTADA (teste de threads necessário).
- **Impacto:** **perda financeira directa** (double payment, excesso de crédito), dados incoerentes.
- **Dependências afectadas:** Vendas, Pagamentos, Compras, Stock.
- **Correção recomendada:** `@Version` em `Sale`, `Customer`, `Purchase` (+ retry loop 1–2× com mensagem "registo alterado por outro operador"); para o limite de crédito, `SELECT … FOR UPDATE` no cliente durante a venda a crédito; converter `OptimisticLockException` em toast "stock mudou — recarregue"; testes de concorrência (2 threads) para os 4 cenários.
- **Prioridade:** P1.

### Tabela resumo dos restantes bugs identificados (formato curto)

| ID | Sev | Módulo | Descrição curta | Evidência (ficheiro→método) |
|---|---|---|---|---|
| BUG-014 | ALTO | Financeiro | `recordReceipt` sem factura: abate saldo sem limite (crédito negativo) e **sem movimento de caixa** (caixa sub-estima recebido) | CustomerAccountService.recordReceipt L96–118 |
| BUG-015 | ALTO | Financeiro | `reconcileCustomerCredits` aloca FIFO a **cotações**; abate `balance -= amount` total (excesso sem controlo); allocations nunca usadas em relatório | CustomerAccountService.reconcileCustomerCredits L120–185 + SaleRepository.findPendingByCustomerId |
| BUG-018 | MÉDIO | Transferências TWA | `complete()` aceita `PENDING` (salta "Expedição"); approve/complete/cancel exigem só `TRANSFERENCIAS:VIEW` (VIEW executa escrita); cancel sem motivo obrigatório | WarehouseTransferService.complete/approve/cancel |
| BUG-019 | ALTO | Compras | sem validação de quantidade/custo negativos; edição de compra RECEIVED reversão+re-entrada **distorce o PMP**; CMP em `double` (precisão); custo zero ignorado silenciosamente; sem RBAC no service; sem dívida formal do fornecedor | PurchaseService.savePurchase L45–170 |
| BUG-020 | ALTO | Compras | `SupplierPaymentService.savePayment`: sem RBAC, sem auditoria no service; movimento de caixa só se o texto do método contiver "dinheiro"/"caixa" — **"Numerário" não gera saída de caixa** (string matching frágil) | SupplierPaymentService.savePayment L100–125 |
| BUG-022 | ALTO | PDV | moedas USD/EUR/ZAR sem `exchangeRate` (fica 1.0) → somas multi-moeda como MZN | SaleFormController.doSave (sem setExchangeRate); Sale.exchangeRate |
| BUG-024 | ALTO | Stock | `initializeStock` **sem permissão** (qualquer utilizador sobrepõe stock); `adjustStock` rotula **todo** o ajuste como `PERDA_DANO` (incluso aumentos); "admin" = nome do papel conter "ADMIN" (frágil) | StockBranchService.initializeStock/adjustStock L40–70, L100–130 |
| BUG-025 | ALTO | PDV | **desconto inexistente na prática**: `lineDiscount`/`totalDiscount` nunca são preenchidos (entidade tem os campos; UI não tem campo); `app_config.max_discount_percent` sem consumidor | SaleFormController (sem desconto); Sale.totalDiscount; V8 app_config |
| BUG-026 | MÉDIO | PDV | carrinho suspenso **estático** (`static heldCartItems`) → partilhado entre utilizadores (privacidade); `allProducts` carregado 1× no init → produtos novos/inactivados só reflectidos ao reabrir o PDV | SaleFormController L68–69, setupProductSearch |
| BUG-028 | ALTO | Fiscal | Apuramento de IVA por documento (`sTax>0.001`) → doc misto (isento+tributado) classifica o subtotal inteiro como tributável | ReportsController.loadIvaData L600–610 |
| BUG-029 | MÉDIO | Dashboard | `DashboardService` (getStats com `state='EMITIDA'` — **excluiria as vendas cash PAGO**) é **código morto** (zero chamadores); @Cacheable sem eviction alguma (armadilha) | DashboardService (todo); grep de chamadores |
| BUG-030 | MÉDIO | Relatórios | `DailyReconciliationService`/`ComplianceDashboardService` contam só `EMITIDA` como "emitido" (excluem vendas cash PAGO) — órfãos mas com cálculo errado por defeito | DailyReconciliationService.reconcile L36–60 |
| BUG-031 | MÉDIO | Relatórios | `ReportService.topProducts` filtra `state='EMITIDA'` → "Mais Vendidos" **exclui vendas em dinheiro PAGO** (inconsistente com Mapa de Vendas); `paymentMethods`/`topProducts` acedem `sale.getItems()/getPayments()` **lazy fora de transacção** → risco `LazyInitializationException` | ReportService.topProducts L74–79; paymentMethods L110–130 |
| BUG-032 | MÉDIO | Stock/Relatórios | `SeriesManagementService.status()` faz `saleRepository.findAll()` (todas as vendas em memória) para máximos por série | SeriesManagementService.status L33–60 |
| BUG-034 | MÉDIO | Caixa | `registerMovement` sem validação no service (amount≤0/null passam se chamado fora da UI); movimentos manuais permitidos em **sessões de dias anteriores** (regra `requireOpenToday` só aplicada às vendas) | CashSessionService.registerMovement L118–150 |
| BUG-035 | BAIXO | Clientes | código gerado `"CLI-"+count+1` → colisão após eliminações (unique constraint → erro 500 na criação); duplicados de nome permitidos; sem pesquisa por telefone/NUIT no listado | Customer.generateCode; DashboardCrudManager.loadCustomers |
| BUG-036 | MÉDIO | Pesquisa | pesquisa PDV: `toLowerCase().contains` **sensível a acentos** (não encontra "telemovel" para "Telemóvel"); sem preço/barcode no filtro (só código/nome/categoria) | SaleFormController.productMatchesSearch L815–822 |
| BUG-037 | MÉDIO | Pesquisa | pesquisa do histórico de vendas: apenas cliente/NUIT/série/tipo/número — **não pesquisa por artigo/producto** | SaleRepository.searchForList L14–30 |
| BUG-038 | ALTO | Financeiro | "Resumo Financeiro" (`loadFinanceiro`): `totalVendas` soma **todas** as vendas (ANULADA, demo, cotações); `totalRecebido` soma **todos** os pagamentos (inclui reconciliações sem venda); "Pendente" = diferença errada | DashboardCrudManager.loadFinanceiro L207–224 |
| BUG-039 | BAIXO | Dashboard | `loadSales` limita a 200 linhas (truncado em silêncio); `loadProducts/loadCustomers` ignoram o parâmetro `page` (carregam tudo) | DashboardCrudManager L135–165 |
| BUG-040 | MÉDIO | Fiscal | trigger `trg_prevent_sales_update` protege `document_type` — o fluxo de recibo (`setDocumentType(RECIBO)` em memória) pode disparar `SIGNAL 45000` se houver flush intermédio (falha intermitente de pagamento) | DbTriggerConfig + PaymentService.createPayment |
| BUG-041 | ALTO | Produção | ordem nasce `COMPLETED` (sem estado pendente); **editar a quantidade depois de completada não ajusta stock** (ordem ≠ produção real); entrada em stock apenas se `currentUser.branch != null` (senão, silêncio); **custo do MP não é rolled-up** no preço de custo do acabado | ProductionOrderFormController.doSave L281–330 |
| BUG-042 | MÉDIO | Config | `onRestoreBackup` (DirectoryChooser) **não faz restauro nenhum** (só mostra nota) — botão inoperante na variante `SistemaModuleController` (parcialmente morta) | SistemaModuleController.onRestoreBackup L876–886 |
| BUG-043 | MÉDIO | BD/Seed | V27 injeta em produção (Flyway) 4 filiais, 3 armazéns, dezenas de produtos/fornecedores com IDs fixos (INSERT IGNORE) — sem separação demo/real; DataInitializer cria ainda a venda demo real (BUG-006) | V27__seed_extensive_mozambique_dataset.sql |
| BUG-044 | BAIXO | Segurança | login mostra ao utilizador `Linha X do Y` do stack trace em erro interno (info leak / UX) | LoginController.doLogin L85–90 |
| BUG-045 | MÉDIO | Negócio | comissões (`users.commission_percent`, `products.commission_percent`) graváveis mas **nunca calculadas** (sem relatório/funcionalidade); `fidelity_points` do cliente totalmente morta | grep: sem consumidores |
| BUG-046 | BAIXO | UX | FXML `sistema_module.fxml` quase órfã (a navegação usa panes programáticas) — código duplicado e divergente (ex.: backups: 2 implementações) | DashboardNavigationManager.showBackupsPane vs SistemaModuleController |
| BUG-047 | BAIXO | Config | "Programação automática de backup" (frequência/hora/retenção) gravada em `app_config` mas o cron é **hardcoded `0 0 3 * * *`** e só testa `autoBackupEnabled` → a UI dá controlo que não existe | BackupService.scheduledBackup L53–66 vs SystemSettingsPageManager |
| BUG-048 | BAIXO | UX | `documentTypeCombo` do PDV não oferece RECIBO/NC/ND (correcto em si) mas o estado default de RECIBO criado por elsewhere é `EMITIDA` — sem fluxo UI para emitir recibo manual | SaleFormController.initialize L220–225 |
| BUG-049 | MÉDIO | Auditoria | auditoria sem valores antes/depois (só texto livre), `details` VARCHAR(1024) (truncation/erro em textos longos), 2 services a escrever na mesma tabela com formatos diferentes; operações críticas sem log próprio no service (anulação, NC, pagamento a fornecedor dependem do controller) | AuditLog (entidade), AuditLogService, SystemLogService |
| BUG-050 | MÉDIO | Despesas | `ExpenseService.createOrUpdateExpense`: sem RBAC no service; sem validação de valor no service; se `markAsPaid` e o caixa falhar → só log, despesa fica gravada (estado definido pela UI) — possível despesa "paga" sem saída de caixa | ExpenseService L40–70 |
| BUG-051 | BAIXO | Caixa | diferença (quebra/sobra) calculada apenas no UI do fecho — não persistida (reconstruível por reported−system, mas sem campo próprio/relatório consolidado) | CloseSessionFormController + CashSessionService.closeSession |
| BUG-052 | BAIXO | Segurança | RBAC por "nome do papel contém ADMIN" em operações de stock (em vez de permissões); papel CAIXA pode abrir/fechar caixa com `CAIXA:VIEW` (granularidade VIEW≅EXECUTE) | StockBranchService.isAdmin; CashSessionService.openSession |
| BUG-053 | BAIXO | UX | `findRecentActiveSales(maxResults)` ignora o parâmetro (API desonesta) | PaymentService L47–49 |
| BUG-054 | MÉDIO | Perf/UI | várias listagens `findAll()` em memória (DashboardCrudManager, ReportService, SeriesManagementService, DailyReconciliationService) + `getLowStock` carrega todo o `stock_branch` — degrada com o volume | ver vários |
| BUG-055 | BAIXO | Perf | `@EnableCaching` ativo com `DashboardService` morta e sem evictions — se algum dia activada, KPIs ficam stale para sempre no dia | CacheConfig + SgvApplication |

---

## 5. MATRIZ DE COBERTURA — AS 62 FUNCIONALIDADES (34)
Legenda: **OK** = cadeia completa verificada por código (com caveats mínimos) · **PARCIAL** = funciona com quebras reportadas · **FALHA** = resultado incorrecto/incompleto confirmado por código · **AUSENTE** = não existe de facto (ou existe mas não está ligada) · **NÃO TESTÁVEL** = nenhum (todas as linhas têm evidência por código; runtime = §0)

| # | Funcionalidade | UI | Backend | BD | Pesquisa | Validação | Integração | Segurança | Logs | E2E | **Estado** |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 | Splash/Inicialização | OK | OK | OK | — | OK | OK | OK | OK | — | **PARCIAL** (sem verificação aprofundada do splash; arranque força perfil `mysql` — `MainApp.resolveSpringStartupArgs`) |
| 2 | Login / Autenticação | OK | PARCIAL | OK | — | OK | OK | **FALHA** (forceChangePassword ineficaz; admin/admin default) | OK | OK | **PARCIAL** (BUG-006, BUG-021, BUG-044) |
| 3 | DASHBOARD | OK | PARCIAL | OK | PARCIAL | OK | **FALHA** (KPIs contam demo+cotações; panel financeiro errado) | OK | OK | — | **PARCIAL** (BUG-004, BUG-038, BUG-039) |
| 4 | Painel Geral | OK | PARCIAL | OK | — | OK | PARCIAL | OK | OK | — | **PARCIAL** (idem #3; gráfico 7 dias inclui cotações/demo) |
| 5 | PDV / Venda Rápida | OK | PARCIAL | OK | PARCIAL | OK | **FALHA** (não-cash→caixa; troco descartado; estado PAGO no crédito) | PARCIAL | OK | PARCIAL | **PARCIAL** (BUG-003, 008, 009, 022, 025, 026, 036) |
| 6 | Histórico de Facturas & Vendas | OK | OK | OK | PARCIAL (sem pesquisa por artigo) | OK | OK | OK | OK | OK | **PARCIAL** (BUG-037, BUG-039, edição de doc emitido→erro SQL) |
| 7 | Gestão de Clientes & CC | OK | PARCIAL | OK | PARCIAL (sem tel/NUIT) | PARCIAL (colisão de código) | **FALHA** (extrato≠saldo) | OK | OK | — | **PARCIAL** (BUG-005, BUG-035) |
| 8 | Cadastro/Edição de Cliente | OK | OK | OK | OK | OK (NUIT mod-11 9 dígitos; código único) | OK | OK | OK | OK | **OK** (caveat: 10 dígitos não verificados; duplicado de nome permitido) |
| 9 | Liquidação de Dívida | OK | PARCIAL | OK | OK | OK | PARCIAL (sem factura→sem caixa; excesso sem controlo) | **FALHA** (sem RBAC no service) | OK | PARCIAL | **PARCIAL** (BUG-012, BUG-014) |
| 10 | Reconciliação de Créditos | OK | **FALHA** | OK | — | PARCIAL | **FALHA** (FIFO a cotações; saldo≠extrato) | FALHA | PARCIAL | — | **FALHA** (BUG-015, BUG-005) |
| 11 | Detalhes/Extrato do Cliente | OK | **FALHA** | OK | — | — | **FALHA** | OK | — | — | **FALHA** (BUG-005) |
| 12 | Sessão de Caixa — Turno Atual | OK | OK | OK | — | OK | OK | OK | OK | OK | **OK** (KPI saldo em tempo real; regras `requireOpenToday` consistentes p/ vendas) |
| 13 | Sessão de Caixa — Histórico | OK | OK | OK | OK | OK | OK | OK (filtro por utilizador) | OK | OK | **OK** |
| 14 | Abertura de Caixa | OK | OK | OK | — | OK (1 turno/utilizador) | OK | PARCIAL (só CAIXA:VIEW) | OK | OK | **OK** (caveat granularidade — BUG-052) |
| 15 | Movimentos de Caixa | OK | PARCIAL | OK | — | PARCIAL (só UI valida) | PARCIAL (sessões antigas) | OK | OK | OK | **PARCIAL** (BUG-034) |
| 16 | Fecho de Caixa / Fita Z | OK | PARCIAL | OK | — | OK | PARCIAL (diferença não persistida) | OK | OK | OK | **PARCIAL** (BUG-051; e o "esperado" está contaminado por BUG-003) |
| 17 | Recebimento de Facturas (Pagamentos) | OK | **FALHA** | OK | OK | PARCIAL | **FALHA** (cotações pagáveis; duplo pagamento; recibo) | **FALHA** | OK | PARCIAL | **FALHA** (BUG-007, BUG-012, BUG-040) |
| 18 | Despesas (lista) | OK | OK | OK | OK | OK | OK | OK | OK | OK | **OK** |
| 19 | Registo de Despesa | OK | PARCIAL | OK | — | PARCIAL (só UI) | PARCIAL (pago sem caixa→so log) | PARCIAL | OK | PARCIAL | **PARCIAL** (BUG-050) |
| 20 | Resumo Financeiro | OK | **FALHA** | OK | OK | — | **FALHA** (totais sem filtro) | OK | OK | — | **FALHA** (BUG-038) |
| 21 | Categorias | OK | OK | OK | OK | OK | OK | OK | OK | OK | **OK** |
| 22 | Formulário de Categoria | OK | OK | OK | — | OK | OK | OK | OK | OK | **OK** |
| 23 | Unidades de Medida | OK | OK | OK | OK | OK | OK | OK | OK | OK | **OK** |
| 24 | Formulário de Unidade | OK | OK | OK | — | OK (abreviação única) | OK | OK | OK | OK | **OK** |
| 25 | Catálogo de Artigos | OK | OK | OK | PARCIAL (sensível a acentos; `page` ignorado) | OK | OK | OK | OK | OK | **PARCIAL** (BUG-036/039 — pesquisa local vs DB: a listagem usa `searchByCodeOrName` na DB; o PDV usa filtro local) |
| 26 | Cadastro de Artigo | OK | OK | OK | OK | OK | PARCIAL (lotes/BOM existam mas UI de lotes ausente) | OK | OK | OK | **OK** (caveats: lotes órfãos — §8) |
| 27 | Inventário (stock filial) | OK | PARCIAL | OK | PARCIAL (NPE se product null) | OK | PARCIAL (dual stock) | OK | OK | OK | **PARCIAL** (BUG-013, BUG-054) |
| 28 | Ajustes de Stock | OK | PARCIAL | OK | — | PARCIAL (rotulo PERDA_DANO sempre) | OK | PARCIAL (admin por nome) | OK | OK | **PARCIAL** (BUG-024) |
| 29 | Stock Inicial | OK | PARCIAL | OK | — | **FALHA** (sem permissão) | OK | **FALHA** | OK | OK | **PARCIAL** (BUG-024) |
| 30 | Produção | OK | **FALHA** | OK | — | PARCIAL | **FALHA** (consumo em silêncio; stock neg.) | PARCIAL | OK | PARCIAL | **FALHA** (BUG-002, BUG-041) |
| 31 | Ordem de Produção / BOM | OK | PARCIAL | OK | OK (preview receita) | OK | PARCIAL (sem custo roll-up; sem estado) | PARCIAL | OK | PARCIAL | **PARCIAL** (BUG-041) |
| 32 | Armazéns | OK | OK | OK | OK | OK | OK | OK | OK | OK | **OK** |
| 33 | Cadastro de Armazém | OK | OK | OK | — | OK | OK | OK | OK | OK | **OK** |
| 34 | Stock Central | OK | OK | OK | OK | OK | PARCIAL (não agrega filial) | OK | OK | OK | **PARCIAL** (BUG-013) |
| 35 | Compras | OK | PARCIAL | OK | OK (duplicado factura fornecedor) | PARCIAL (negativos) | PARCIAL (dual stock; PMP em edição) | PARCIAL (sem RBAC) | OK | PARCIAL | **PARCIAL** (BUG-013, BUG-019) |
| 36 | Entrada de Compra / CMP | OK | PARCIAL | OK | — | PARCIAL | PARCIAL (double no CMP; custo zero) | PARCIAL | OK | OK | **PARCIAL** (fórmula PMP correcta; execução com double e distorção em edição) |
| 37 | Fornecedores | OK | OK | OK | OK | OK (active) | OK | OK | OK | OK | **OK** |
| 38 | Cadastro de Fornecedor | OK | OK | OK | — | OK | OK | OK | OK | OK | **OK** |
| 39 | Pagamentos a Fornecedores | OK | PARCIAL | OK | OK | OK (excede saldo bloqueado) | PARCIAL (caixa frágil) | **FALHA** (sem RBAC) | FALHA (sem log no service) | OK | **PARCIAL** (BUG-020) |
| 40 | Transferências (filial→filial) | OK | **FALHA** | OK | OK | OK (estados) | PARCIAL (numeração racy) | **FALHA** (sem RBAC) | OK | PARCIAL | **FALHA** (BUG-017) |
| 41 | Emissão de Guia de Transferência (TWA) | OK | PARCIAL | OK | OK | OK | PARCIAL (salta expedição; VIEW executa) | PARCIAL | OK | OK | **PARCIAL** (BUG-018) |
| 42 | Mapa Geral de Vendas | OK | **FALHA** | OK | PARCIAL (filtros OK) | — | **FALHA** (ANULADA+demo+cotações) | OK | OK | — | **FALHA** (BUG-004) |
| 43 | Artigos para Reposição | OK | OK | OK | OK | OK | OK | OK | OK | OK | **OK** (alertas ESGOTADO/ABAIXO_MINIMO/SEM_FICHA) |
| 44 | Mais Vendidos | OK | **FALHA** | OK | OK | — | **FALHA** (só EMITIDA → exclui cash PAGO) | OK | OK | — | **FALHA** (BUG-031) |
| 45 | Apuramento de IVA | OK | **FALHA** | OK | OK | — | **FALHA** (doc misto; cotações; demo) | OK | OK | — | **FALHA** (BUG-004, BUG-028) |
| 46 | Contas Correntes (aging devedores) | OK | PARCIAL | OK | OK | OK | PARCIAL (inclui cotações) | OK | OK | — | **PARCIAL** (BUG-007) |
| 47 | Kardex | OK | OK | OK | OK (paginação) | OK | OK (unificado filial+armazém) | OK | OK | OK | **OK** (mas os movimentos que originam podem estar errados — ver BUG-002/003) |
| 48 | Stock por Armazém | OK | OK | OK | OK | OK | OK | OK | OK | OK | **OK** |
| 49 | Guias de Transferência (mapa) | OK | OK | OK | OK | OK | OK | OK | OK | OK | **OK** |
| 50 | Pagamentos a Fornecedores (mapa) | OK | OK | OK | OK | OK | OK | OK | OK | OK | **OK** |
| 51 | Auditoria / Logs | OK | PARCIAL | OK | OK (filtro por categoria) | OK | PARCIAL (antes/depois ausente; gaps por operação) | PARCIAL | — | OK | **PARCIAL** (BUG-049) |
| 52 | Dados da Empresa | OK | OK | OK | — | OK | OK | OK | OK | OK | **OK** (parâmetros fiscais: série default, NUIT consumidor, taxas) |
| 53 | Utilizadores | OK | PARCIAL | OK | OK | OK | **FALHA** (re-activação no arranque) | PARCIAL | OK | PARCIAL | **PARCIAL** (BUG-006) |
| 54 | Cadastro de Utilizador | OK | OK | OK | — | OK | OK | OK | OK | OK | **OK** (papéis+permissões via diálogos) |
| 55 | Filiais | OK | OK | OK | OK | OK | OK | OK | OK | OK | **OK** |
| 56 | RBAC / Permissões | PARCIAL | PARCIAL | OK | — | OK | **FALHA** (gaps: TransferService, PurchaseService, SupplierPaymentService, PaymentService, initializeStock; VIEW≅EXECUTE) | PARCIAL | PARCIAL | — | **PARCIAL** (BUG-017, 019, 020, 024, 052) — a segurança **não** pode ser garantida só pela UI |
| 57 | Backup / Restauro | OK | **FALHA** | OK | — | FALHA | **FALHA** | OK | OK | **FALHA** | **FALHA** (BUG-001) |
| 58 | Treinamento / Simulação | OK | PARCIAL | OK | — | OK (UI+SaleService) | PARCIAL (createCreditNote/annulSale sem check no service; outros services sem check — prática OK via UI única) | OK | OK | PARCIAL | **PARCIAL** (o claim "nenhum dado real é alterado" não é garantido por serviço) |
| 59 | Licenciamento | OK | **FALHA** | OK | — | FALHA | FALHA | **FALHA** (forjável; sem binding; sem bloqueio) | OK | — | **FALHA** (BUG-027) |
| 60 | Alteração de Palavra-passe | OK | OK | OK | — | OK (mín 6, coincidência, actual verificada) | OK | OK | OK | OK | **OK** (política mínima 6 = fraca — melhoria) |
| 61 | Document Preview | OK | OK | — | — | OK | OK (4 formatos: 80mm/58mm/A4/A5) | OK | OK | OK | **OK** (mas os documentos carecem de QR — BUG-016) |
| 62 | DetailDialog / ConfirmDialog | OK | OK | — | — | OK | OK | OK | — | OK | **OK** (SgvDialog centralizado) |

**Contagem (verificada linha a linha):** OK=25 · PARCIAL=26 · FALHA=11 · AUSENTE=0 nas 62 — as 20 funcionalidades implícitas ausentes estão em §8

---

## 6. MATRIZ DE RELAÇÕES ENTRE MÓDULOS (35)

| Módulo A | Módulo B | Relação esperada | Existe? | Funciona? | Problema |
|---|---|---|---|---|---|
| Venda (cash) | Stock (filial) | baixa atómica + kardex | Sim | Sim | OK (com @Version; erro 500 em conflito — BUG-033) |
| Venda (crédito) | Stock | baixa atómica | Sim | Sim | OK |
| Venda (não-cash) | Caixa | **não** deve mover a gaveta | Não | **Não** | **BUG-003 — lança IN físico para M-Pesa/POS/transferência** |
| Venda | Cliente (crédito) | débito da conta corrente | Sim | Sim | OK, mas sem lock (BUG-033) e estado PAGO (BUG-009) |
| Venda | Documento fiscal | hash+numeração+série | Sim | Parcial | cadeia previous-hash errada; QR ausente (BUG-016) |
| Venda | Auditoria | log da operação | Parcial | Parcial | só via controller (`VENDA_CRIADA`); sem antes/depois (BUG-049) |
| Venda | AT (submissão) | envio/pending_sync | **Não ligada** | **Não** | `AtSubmissionService` órfã; `pendingSync` nunca setado (BUG-§8) |
| Anulação | Stock | reposição | Sim | Sim | OK |
| Anulação | Caixa | estorno OUT | Sim | **Não (silencioso)** | **BUG-011 — catch engolido; turno errado** |
| Anulação | Conta corrente | abate a dívida | Sim | Sim | OK |
| Anulação | Auditoria | log | Parcial | Parcial | sem log próprio no service (BUG-049) |
| NC | Stock | reposição | Sim | Sim | OK |
| NC | Conta corrente | **abate a dívida** | **Não** | **Não** | **BUG-010** |
| NC | Caixa | **reembolso OUT** | **Não** | **Não** | **BUG-010** |
| NC | Documento AT | hash/numeração | **Não** | **Não** | BUG-010 |
| Cotação | Factura | conversão (originSale→COTACAO_PAGA) | Sim | Parcial | funciona, mas a cotação continua pendível na lista de recebimentos (BUG-007) |
| Compra | Stock (armazém) | entrada + kardex | Sim | Sim | OK |
| Compra | Stock (filial) | **ponte p/ PDV** | Parcial | Parcial | **BUG-013 — só via TWA opcional** |
| Compra | PMP/CMP | recálculo custo | Sim | Parcial | double; edição distorce; custo zero ignorado (BUG-019) |
| Compra | Fornecedor (dívida) | débito conta corrente | Implícita | Parcial | sem saldo formal no fornecedor; dívida = Σ(purchases−payments) derivada |
| Compra | Caixa | **saída se paga** | **Não** | **Não** | compra paga não lança OUT (só `SupplierPaymentService` quando "dinheiro") |
| Pagamento cliente | Caixa | IN (só numerário) | Sim | Parcial | qualquer método lança IN (idem BUG-003); pagamento sem factura **não** lança IN (BUG-014) |
| Pagamento cliente | Factura | paidAmount/estado | Sim | Sim | OK, mas sem lock (BUG-012) e recibo sem série (BUG-012) |
| Pagamento cliente | Conta corrente | abate | Sim | Sim | OK (excesso sem controlo — BUG-014) |
| Pagamento fornecedor | Caixa | OUT (só numerário) | Sim | Parcial | matching por substring "dinheiro"/"caixa" (BUG-020) |
| Pagamento fornecedor | Compra | paidAmount/estado PAID | Sim | Sim | OK (sem lock) |
| Transferência TWA | Stock origem | baixa em COMPLETED | Sim | Sim | OK (transaccional) |
| Transferência TWA | Stock destino | entrada em COMPLETED | Sim | Sim | OK |
| Transferência TWA | Estados | PENDING→IN_TRANSIT→COMPLETED | Sim | Parcial | `complete` aceita PENDING (salta expedição — BUG-018) |
| Transferência TWA | Auditoria | log | Sim | Sim | OK (controller) |
| Transferência filial | Stock origem/destino | baixa/entrada em COMPLETED | Sim | Sim | OK, mas **sem RBAC** (BUG-017) e numeração racy |
| Produção | Matéria-prima | consumo BOM | Sim | **Não (silencioso)** | **BUG-002 — catch vazio; stock negativo** |
| Produção | Produto acabado | entrada stock | Sim | Parcial | OK se `user.branch≠null`; sem custo roll-up (BUG-041) |
| Produção | Kardex | movimentos | Sim | Parcial | depende do BUG-002 |
| Despesa | Caixa | OUT se paga | Sim | Parcial | falha do caixa só log (BUG-050) |
| Caixa | Fecho/Fita Z | esperado vs contado | Sim | Parcial | esperado contaminado (BUG-003); diferença não persistida (BUG-051) |
| Backup | BD | cópia real recuperável | **Não** | **Não** | **BUG-001** |
| Login | forceChangePassword | bloqueio p/ mudar | **Não** | **Não** | BUG-021 |
| Utilizador desactivado | Login | recusa | Sim (login) | **Não (persiste?)** | **re-activado no arranque** (BUG-006) |
| PDV | Moeda | câmbio | **Não** | **Não** | BUG-022 |
| PDV | Desconto | linha/subtotal | **Não (UI)** | **Não** | BUG-025 |
| Licença | Arranque/Operações | bloqueio em expirado | **Não** | **Não** | BUG-027 |
| Treinamento | Serviços de escrita | bloqueio total | Parcial | Parcial | só SaleService no service; `createCreditNote`/`annulSale` e demais services sem check (aceitável via UI única, mas não garantido) |
| Relatório Mapa Vendas | Dashboard | mesmo número p/ o mesmo período | Sim | **Não** | **BUG-004** |
| Mapa Vendas | Apuramento IVA | mesmo universo de vendas | Sim | **Não** | **BUG-004/028** |
| Extrato Cliente | customers.balance | iguais no fim | Sim | **Não** | **BUG-005** |
| Pagamentos | Cotações | impagáveis | Sim | **Não** | **BUG-007** |
| SAF-T | Vendas | exporta o universo reportável | Sim | Parcial | exporta `sales` sem o filtro partilhado (mesmo risco BUG-004) |

---

## 7. MATRIZ DE PESQUISAS (36)

| Tela | Campo | Tempo real? | Backend/Local | Debounce | Filtros activos | Resultado correcto? | Problema |
|---|---|---|---|---|---|---|---|
| PDV | `productSearchCombo` | **Sim** (debounce 140ms) | Local (`allProducts` + `FilteredList`) | 140ms | código (contains, ci), nome (contains, ci), categoria (contains, ci) + combo de categoria | Parcial | **sensível a acentos**; sem pesquisa por preço/barcode; lista congelada no init (produtos novos não aparecem sem reabrir); "popular" usa query 90 dias (OK) |
| PDV | barcode (entrada rápida) | Sim (auto-deteção por velocidade) | DB (`product_barcodes`) + fallback código | — | exact/ignorecase | Sim | OK; `tryAddByBarcode` engole excepções (mostra erro, devolve true) |
| PDV | `quantityField` | — | UI | — | — | OK | Enter adiciona; numpad OK |
| Histórico Vendas | campo de pesquisa + estado + tipo + datas | Ao pressionar (Filtrar) | DB (`searchForList`) | — | cliente, NUIT, série, tipo doc, nº doc (não: artigos) | Parcial | **sem pesquisa por artigo**; cap 200 (silencioso); acentos: `LOWER+LIKE` (MySQL utf8mb4 — case-insensitive ok, acento-sensível) |
| Produtos (dashboard) | pesquisa | Ao pressionar | DB `searchByCodeOrName` | — | código/nome contains ci | Parcial | `page` ignorado (tudo em memória); acentos |
| Clientes (dashboard) | pesquisa | Ao pressionar | DB `searchByCodeOrName` | — | código/nome (não: NUIT/telefone) | Parcial | idem; sem NUIT/telefone |
| Stock (dashboard) | pesquisa | Ao pressionar | Local (findAll + filter) | — | nome/código contains ci | Parcial | **NPE se `product` null**; carrega tudo |
| Caixa — histórico | (sem pesquisa livre; filtros por estado/utilizador) | — | DB | — | estado, utilizador | OK | OK |
| Relatórios — Vendas | De/Até/Filial | Ao Filtrar | DB + filtro Java | — | datas, filial | Parcial | universo errado (BUG-004) |
| Relatórios — IVA | De/Até | Ao Filtrar | DB | — | datas | Parcial | idem + classificação por doc (BUG-028) |
| Relatórios — Devedores | cliente/doc + datas | Ao Filtrar | Java (aging) | — | cliente/doc contains | Parcial | inclui cotações (BUG-007) |
| Relatórios — Kardex | (filtros de tipo/período) | Ao Filtrar | DB paginado | — | — | OK | OK |
| Relatórios — Transferências | (datas/estado) | Ao Filtrar | DB | — | — | OK | OK |
| Sistema — Utilizadores | pesquisa | Ao Filtrar | Local/DB | — | username? | OK | OK |
| Sistema — Backups | (sem pesquisa) | — | DB | — | — | OK | OK |
| Logs | pesquisa por categoria/texto | Ao Filtrar | DB | — | categoria, texto | OK | OK |
| Cliente — Extrato | (sem pesquisa) | — | DB | — | — | **FALHA** | conteúdo divergente (BUG-005) |
| Pagamentos | (lista pendente) | — | DB | — | — | **FALHA** | inclui cotações (BUG-007) |
| Fornecedores | (listagem) | — | DB | — | — | OK | OK |
| Compras | (listagem + duplicado factura) | — | DB | — | duplicado por fornecedor | OK | OK |

**Notas transversais:** (1) nenhuma pesquisa normaliza acentos (`ç→c` etc.) — relevante p/ produto "Telemóvel"; (2) `searchForList` e as queries `LIKE` dependem do collation utf8mb4_unicode_ci da BD (OK p/ case, não p/ acento); (3) pesquisa em tempo real **só existe no PDV** (correcto com debounce); o resto usa botão Filtrar (aceitável); (4) não foi identificada pesquisa que execute queries em excesso/race (o PDV tem guard `isRefreshingProducts`).

---

## 8. FUNCIONALIDADES AUSENTES (29 / 41.C+M) — "deveria existir e não existe"

| # | Funcionalidade ausente | Por que deveria existir | Módulo dependente | Entidades | Risco de não existir | Prioridade |
|---|---|---|---|---|---|---|
| A1 | **Emissão de AT real** (submissão online + QR + hash conforme spec MZ) | A app declara conformidade AT (certificações, hashes, SAF-T) mas nunca envia nada nem gera QR | Fiscal | sales.qr_code, hash_hash, pending_sync | **Risco fiscal directo** (não facturação electrónica) | P0 |
| A2 | **Backup/Restauro reais** | qualquer SGBD de negócio precisa de recuperação de desastre | Config | system_backups | perda total de dados | P0 |
| A3 | **Troco/pagamento recebido persistido** | auditoria de gaveta e trocos | PDV | sales.paid_amount/change_amount | gaveta não auditável | P1 |
| A4 | **Desconto na venda** (linha ou documento) | o próprio `app_config.max_discount_percent` e `SaleItem.lineDiscount` assumem que existe | PDV | sale_items, app_config | limite de desconto configurável sem funcionalidade | P1 |
| A5 | **Ledger de conta corrente** (eventos por operação) | extrato fiel e reconciliável | Clientes | nova tbl customer_account_entries | extrato≠saldo (BUG-005) | P0 |
| A6 | **Bloqueio efectivo de licenciamento** + trial com prazo real | defesa da receita comercial | Config | app_config.license_* | pirataria trivial | P1 |
| A7 | **forçar alteração de password** funcional | política de segurança pós-criação/reset | Segurança | users.force_change_password | credenciais partilhadas | P1 |
| A8 | **Lotes/validade** (UI de `ProductBatch` + alerta de expiração) | entity/service/repo existem; perecíveis (lacticínios seed) | Produtos | product_batches | venda de produto expirado sem alerta | P1 |
| A9 | **Encomendas de Compra (PO)** (UI + conversão em factura de compra) | `PurchaseOrderService` completo existe, zero UI | Compras | purchase_orders | planeamento de compras ausente | P2 |
| A10 | **Recibo WhatsApp/SMS** (ligar `ReceiptDeliveryService`) | expectável p/ PDV MZ | Vendas | sales | experiência | P2 |
| A11 | **Apuramento de fim de dia** (ligar `DailyReconciliationService` — com o cálculo corrigido) | controlo fiscal diário | Relatórios | sales | ausência de controlo | P1 |
| A12 | **Dashboard de conformidade AT** (ligar `ComplianceDashboardService`) | visibilidade do dono | Relatórios | sales | ausência de controlo | P2 |
| A13 | **Comissões de vendedores** (cálculo + relatório) | campos `commission_percent` (user e produto) graváveis | Relatórios | users, products, sales | funcionalidade prometida inexistente | P2 |
| A14 | **Fidelidade** (gasto/recompensa de pontos) | `fidelity_points` existe e nunca mexe | Clientes | customers | funcionalidade morta | P3 |
| A15 | **Impressão/numeração de recibo próprio** (série RB) | tributação correcta de recibos | Fiscal | sales (série RECIBO) | não-conformidade | P1 |
| A16 | **Validação de NUIT de 10 dígitos** (checksum do formato com letra) | o 9 dígitos é verificado; o 10 não | Clientes | customers.nuit | NUIT inválidos aceites | P2 |
| A17 | **Diferença de fecho persistida** (campo `variance`) | histórico de quebras | Caixa | cash_sessions | apenas reconstruível | P2 |
| A18 | **Bloqueio de escrita em expirado/licença inválida** | idem A6 | Config | — | idem | P1 |
| A19 | **Alerta/verificação de duplicação de vendas** (a query `findRecentDuplicates` existe — sem UI/uso) | anti-erro de dupla venda | Vendas | sales | erro humano | P3 |
| A20 | **Multi-utilizador: verificação de sessão no PDV ao longo do tempo** (turno fica stale se fechar noutro terminal) | consistência | Caixa | cash_sessions | venda sem caixa | P3 |

---

## 9. MATRIZ DE EVENTOS (37) — amostra representativa (botões/atalhos/listeners principais)

| Tela | Componente | Evento | Handler | Serviço chamado | Resultado | UI actualizada? | Problema |
|---|---|---|---|---|---|---|---|
| PDV | productSearchCombo editor | key-pressed ENTER | eventFilter | tryAddByBarcode → handleProductSelection | item no carrinho | Sim | OK |
| PDV | productSearchCombo editor | text (debounce) | setupDebounce | refreshProductSearchResults (local) | dropdown actualizado | Sim | OK (acentos) |
| PDV | btnExact/Plus50..1000 | action | addCashToReceived/received | — | troco recalculado | Sim | OK |
| PDV | Numpad 0-9/. | action | appendToActiveInput | — | digitação | Sim | OK (foco quantity/received) |
| PDV | F2/F4/F6/F7/F8/F10/ESC | key | setupGlobalShortcuts | — | focus/hold/recall/cliente/save/cancel | Sim | OK (F3/F5/F9 livres) |
| PDV | btnHoldCart/btnRecallCart (F6/F7) | action | holdCurrentCart/recallHeldCart | — (static) | carrinho guardado | Sim | **static entre utilizadores** (BUG-026) |
| PDV | X (item) | action | setupActionColumn | — | remove item | Sim | OK |
| PDV | saveButton / F10 | action | doSave | SaleService.processAndSave | venda+PDF | Sim | troco perdido (BUG-008); estado crédito (BUG-009) |
| PDV | cancelButton / ESC | action | doCancel | — | limpa | Sim | OK (sem confirmação — UX) |
| Histórico | btn Anular | action | annulSelectedSale | SaleService.annulSale | ANULADA+stock+CC | Sim | estorno caixa engolido (BUG-011) |
| Histórico | btn Nota de Crédito | action | createCreditNoteFromSelectedSale | SaleService.createCreditNote | NC+stock | Sim | sem caixa/CC (BUG-010) |
| Histórico | btn Reimprimir | action | (DashboardCrudManager) | saleDocumentService.generateDocument | PDF | Sim | `reprint_count` não incrementado (não verificado em runtime) |
| Histórico | btn Editar (doc emitido) | action | setSale(existente)→doSave | saleRepository.save | **trigger DB SIGNAL 45000** | Erro 500 | UX: deveria estar desabilitado para EMITIDA/PAGO |
| Caixa | Abrir Sessão | action | OpenSessionFormController | CashSessionService.openSession | OPEN | Sim | OK |
| Caixa | Fechar Sessão | action | CloseSessionFormController | closeSession | CLOSED+systemValue | Sim | diferença não persistida (BUG-051) |
| Caixa | Movimento manual | action | CashMovementFormController | registerMovement | IN/OUT | Sim | validação só UI (BUG-034) |
| Pagamentos | Guardar | action | PaymentFormController.doSave | PaymentService.createPayment | Payment+caixa+CC | Sim | cotações pagáveis (BUG-007); race (BUG-012) |
| Despesas | Guardar (paga) | action | ExpenseFormController | ExpenseService.createOrUpdateExpense | Expense+OUT | Sim | falha caixa só log (BUG-050) |
| Compras | Guardar (RECEIVED) | action | PurchaseFormController | PurchaseService.savePurchase | compra+stock armazém+PMP | Sim | sem RBAC; negativos; PMP edição (BUG-019) |
| Compras | Anular | action | (manager) | PurchaseService.annulPurchase | CANCELLED+reversão | Sim | OK (bloqueia se paga) |
| TWA | Emitir/Aprovar/Receber/Cancelar | actions | WarehouseTransferFormController | WarehouseTransferService | estados+stock | Sim | complete de PENDING (BUG-018) |
| Produção | Guardar | action | ProductionOrderFormController | repo.save + increaseStock + consume | ordem+stock | Sim | consumo em silêncio (BUG-002) |
| Sistema | Criar Backup | action | createBackupNow | (Nenhum real) | **registo falso COMPLETED** | Sim | **BUG-001** |
| Sistema | Restaurar | action | confirmRestore | (cópia p/ C:\xampp) | **não restaura** | Sim | **BUG-001** |
| Sistema | Guardar Programação | action | (appConfig) | AppConfigService.save | settings | Sim | cron hardcoded (BUG-047) |
| Sistema | Activar Treino | action | toggleTraining | AppConfigService | demoMode | Sim | OK |
| Sistema | Activar Licença | action | activateLicense | LicenseService | app_config | Sim | forjável (BUG-027) |
| Sistema | Alterar Password | action | submitPasswordChange | authService+repo | BCrypt novo | Sim | OK |
| Dashboard | logout | action | doLogout | — | volta ao login | Sim | OK (log LOGOUT) |
| Login | loginButton / Enter | action | doLogin | DesktopAuthService | dashboard | Sim | forceChange ineficaz (BUG-021) |
| Relatórios | Exportar Excel/PDF | action | ExportUtil/ReportsController | POI/PDFBox | ficheiro | Sim | OK |
| Relatórios | Exportar SAF-T | action | (ReportsController) | SafTExportService | XML | Sim | universo sem filtro partilhado (risco BUG-004) |

**Handlers vazios/TODO/FIXME:** não foram encontrados `TODO`/`FIXME` em `src/main`; **handlers vazios: nenhum** nos FXML (todos os `onAction` têm método). Código morto relevante: §11.

---

## 10. MATRIZ DE DADOS (38) — cadeia por entidade

| Entidade | CRIAR | ARMAZENAR | CONSULTAR | EDITAR | REFLETIR NOUTRAS TELAS | RELATÓRIOS | AUDITORIA | ANULAR/ELIMINAR |
|---|---|---|---|---|---|---|---|---|
| Venda/Sale | PDV OK | OK | OK | **parcial** (editado→trigger DB) | OK (histórico) | **FALHA** (universo) | parcial | ANULAR OK (com BUG-011); DELETE: trigger DB bloqueia emitidas; **cotações elimináveis fisicamente** |
| Item de venda | OK | OK | OK | bloqueado (trigger) | OK | idem | — | idem |
| Pagamento | OK | OK | OK | N/A | OK | OK | OK | **sem anulação de pagamento** (ausente) |
| Cliente | OK | OK | OK | OK | OK (extrato FALHA) | PARCIAL | OK | **sem desactivar** (sem active) — DELETE físico |
| Conta corrente (saldo) | implícita | OK | OK (extrato FALHA) | implícita | **FALHA** | **FALHA** | parcial | — |
| Produto | OK | OK | OK | OK | OK (PDV stale) | OK | OK | OK (inactivar via isActive) |
| Lote (ProductBatch) | **sem UI** | OK | OK | **sem UI** | **AUSENTE** | **AUSENTE** | OK (service) | **sem UI** |
| StockBranch | OK (init sem permissão) | OK | OK | PARCIAL (ajuste) | OK | OK | parcial | OK (admin) |
| StockWarehouse | OK (compra) | OK | OK | via TWA/compra | PARCIAL (não agrega) | OK | OK | — |
| StockMovement (kardex) | OK (derivado) | OK | OK | N/A | OK | OK | OK | N/A |
| Compra | OK | OK | OK | PARCIAL (distorce PMP) | OK | OK | OK | ANULAR OK (bloqueia paga) |
| Fornecedor | OK | OK | OK | OK | OK | OK | OK | desactivar OK |
| Pagamento fornecedor | OK | OK | OK | N/A | OK | OK | **FALHA** (sem log service) | **sem anulação** |
| Transfer TWA | OK | OK | OK | N/A | OK | OK | OK | OK (sem motivo obrigatório) |
| Transfer filial | OK (sem RBAC) | OK | OK | N/A | OK | OK | OK | OK |
| Produção | OK (nascida COMPLETED) | OK | OK | **perigoso** (qtd pós-completa) | PARCIAL | PARCIAL | OK | **sem anulação de produção** (ausente) |
| Caixa Session | OK | OK | OK | N/A | OK | OK | OK | fecho OK; **reabrir = nova sessão** (OK) |
| Caixa Movement | OK | OK | OK | N/A | OK | OK | OK | **sem anular movimento** (ausente — estorno manual só via novo movimento) |
| Despesa | OK | OK | OK | OK | OK | OK | OK | ANULAR OK (com caveats BUG-050) |
| Utilizador | OK | OK | OK | OK | **FALHA** (re-activação arranque) | — | OK | DELETE (protegido admin) |
| Papel/Permissões | OK | OK | OK | OK | OK (imediato) | — | OK | OK |
| Backup | **FALHA** (falso) | — | OK (lista) | — | — | — | OK (registo) | OK (ficheiro inexistente) |

**Quebras principais:** conta corrente (extrato), lote (UI), anulações de pagamento/movimento/produção (ausentes), utilização de backup, edição de venda emitida.

---

## 11. FUNCIONALIDADES MORTAS / CÓDIGO MORTO (28 / 41.K)

| # | Item | Tipo | Evidência |
|---|---|---|---|
| K1 | `DashboardService` (todo, ~130 linhas, @Cacheable+@CacheEvict) | Serviço sem chamadores | grep zero referências em `desktop/` |
| K2 | `AtSubmissionService` (189 linhas, HTTP AT) | Serviço órfão | zero chamadores |
| K3 | `ReceiptDeliveryService` (WhatsApp/SMS) | Serviço órfão | zero chamadores |
| K4 | `DailyReconciliationService` | Serviço órfão | zero chamadores |
| K5 | `ComplianceDashboardService` | Serviço órfão | zero chamadores |
| K6 | `ProductBatchService` + entity + repo + tabela V26 | Funcionalidade órfã (lotes) | zero chamadores |
| K7 | `PurchaseOrderService` + entity + repo + tabela V26 | Funcionalidade órfã (PO) | zero chamadores |
| K8 | `PrintHtmlService` (374 linhas) | Serviço órfão | zero chamadores |
| K9 | `SaleFormController.refreshPopularProducts` — bloco `if (true) { … return; }` + código inalcançável após | Código morto | L920–960 |
| K10 | `SaleFormController.setupBarcodeAutoDetection` — conta teclas rápidas **sem efeito** (o real é o ENTER) | Código morto | L1042–1070 |
| K11 | `SaleFormController.searchMatchPriority` — método sem utilizadores | Código morto | grep |
| K12 | `PaymentService.findRecentActiveSales(maxResults)` — parâmetro ignorado | API morta | L47–49 |
| K13 | `DashboardCrudManager.loadProducts/loadCustomers(…, int page)` — `page` ignorado | Parâmetro morto | L152–168 |
| K14 | `SistemaModuleController` (1329 linhas) — grande parte órfã (a navegação usa panes programáticas); inclui o backup/restore falso duplicado | Duplícata | DashboardNavigationManager usa `settingsPageManager` |
| K15 | `Sale.demoFlag` só setável por… (grep: nunca set true em `main`!) | Flag morta na escrita | seed/initializer não a marcam → BUG-006 |
| K16 | `Sale.qrCode`, `offlineFlag`, `pendingSync`, `withholdingTax*` (em parte) | Campos sem escritor | grep |
| K17 | `Customer.fidelityPoints` | Campo morto | grep |
| K18 | `Product.commissionPercent`, `User.commissionPercent` | Sem cálculo/relatório | grep |
| K19 | `CashMovement` — `OperationKind.TRANSFER` inferido por string | Heurística frágil | inferOperationKind |
| K20 | `BackupService.createSnapshot` (ZIP SAF-T+backup) | Método sem chamador | grep |

---

## 12. FUNCIONALIDADES ÓRFÃS (41.L) — funcionam isoladamente, não integradas

| # | Funcionalidade | Estado |
|---|---|---|
| L1 | Submissão AT (K2) | implementada (HTTP+JSON), nunca chamada → `pending_sync` nunca processado |
| L2 | Lotes/validade (K6) | CRUD completo no service, sem UI, sem ligação a compras/vendas (venda de expirado não bloqueada) |
| L3 | Encomendas de compra (K7) | CRUD completo, sem UI, sem conversão em `Purchase` |
| L4 | Apuramento diário + Compliance (K4/K5) | cálculos prontos (com o bug EMITIDA — BUG-030), sem UI |
| L5 | Recibo digital (K3) | fallback wa.me pronto, sem UI |
| L6 | Backup real (mysqldump) | correcto, sem UI, cron desconectado da configuração |
| L7 | Detecção de duplicação de vendas (`findRecentDuplicates`) | query pronta, sem UI/regra |
| L8 | Snapshots ZIP (SAF-T+backup) | pronto, sem UI |

---

## 13. AUDITORIA DE ESTADOS (19)

| Entidade | Estados | Transições implementadas | Proibidas/ausentes | Problemas |
|---|---|---|---|---|
| Sale | EMITIDA, PAGO, PAGO_PARCIAL*, ANULADA, COTACAO_ABERTA, COTACAO_PAGA, ENCOMENDA_ABERTA | EMITIDA→PAGO (pagamento), PAGO_PARCIAL→PAGO, (qualquer)→ANULADA, COTACAO_ABERTA→COTACAO_PAGA (conversão) | — | ***PAGO_PARCIAL não existe no enum** (string solta — `SaleState.fromString` devolve EMITIDA para valores desconhecidos: **estado órfão normalizado para EMITIDA**); venda cash nasce PAGO; venda a crédito nasce PAGO (deveria EMITIDA); COTACAO_PAGA sem UI de "paga" (é convertida); ENCOMENDA_ABERTA nunca progride (sem transição p/ fechamento — encomenda vira facta via originSale? não implementado) |
| Purchase | RECEIVED, PENDING, CANCELLED, PAID*, PAGO_PARCIAL* | RECEIVED→CANCELLED (anular), (pago)→PAGO_PARCIAL→PAID | anular com pagos bloqueado | ***estados extra fora do enum comentado na entidade; entity default RECEIVED vs DB default PENDING** (divergência V1 vs entity); PENDING nunca criado pela UI |
| Transfer (filial) | PENDING, APPROVED, REJECTED, COMPLETED, CANCELLED | PENDING→APPROVED→COMPLETED; PENDING/APPROVED→REJECTED/CANCELLED | COMPLETED não cancelável | sem RBAC; numeração racy |
| WarehouseTransfer | PENDING, IN_TRANSIT, COMPLETED, CANCELLED | PENDING→IN_TRANSIT→COMPLETED; PENDING→COMPLETED (**salta**); não-COMPLETED→CANCELLED | — | complete de PENDING; cancel sem motivo |
| CashSession | OPEN, CLOSED | OPEN→CLOSED | reabrir = nova sessão | OK |
| ProductionOrder | PENDING, COMPLETED | (nascida)→COMPLETED | — | **PENDING órfão** (nunca usado); edição pós-COMPLETED sem controlo |
| Expense | PENDING, PAID, CANCELLED | →PAID (UI), →CANCELLED (estorno caixa) | — | cancel de PAID com falha de caixa só log |
| User | active boolean | active→inactive (UI) | — | **re-activação no arranque** (BUG-006) |
| License | (não estado; key+expiry) | — | — | sem estado de bloqueio |

**Estados órfãos/impossíveis:** `PAGO_PARCIAL` (fora do enum, normalizado para EMITIDA pelo `fromString` — qualquer código que use `SaleState.fromString("PAGO_PARCIAL")` trata como EMITIDA! verificar utilizadores: `SaleState.fromString` usado em… (grep mostra utilização em poucos sítios — risco); `ENCOMENDA_ABERTA` sem progressão; `COTACAO_PAGA` sem UI; `PENDING` de produção/compra.

---

## 14. TRANSAÇÕES (20) E CONCORRÊNCIA (21)

**Transacções — avaliação por operação (atomicidade real vs declarada):**

| Operação | @Transactional? | Efeitos no mesmo TX? | Comportamento em falha intermédia | Veredicto |
|---|---|---|---|---|
| Venda (persistSaleTransaction) | Sim | venda+itens+stock+caixa+CC+hashes | **rollback total** (caixa lança IllegalStateException) | **OK** (bom design: stock antes de caixa; PDF fora do TX) |
| Anulação | Sim | estado+stock+CC/caixa | stock+CC rollback juntos; **caixa fora do TX de facto** (catch engolido) | **PARCIAL** (BUG-011) |
| NC | Sim | NC+stock | rollback total | **PARCIAL** (falta caixa/CC — BUG-010) |
| Pagamento cliente | Sim | payment+alloc+sale+CC+caixa | **caixa catch engolido** (mesmo anti-padrão) | **PARCIAL** |
| Reconciliação | Sim | payment+allocs+sales+CC+caixa | caixa catch engolido | **PARCIAL** |
| Compra | Sim | purchase+items+stock armazém+PMP | rollback total (reversão de edição lança) | **OK** |
| Pagamento fornecedor | Sim | payment+purchase+caixa | caixa só log (não rollback) | **PARCIAL** |
| Transfer TWA | Sim | items+stock origem/destino | rollback total | **OK** (bom) |
| Transfer filial | Sim | items+stock | rollback total | **OK** |
| Produção | **NÃO no service** (repo.save + 2 serviços @Transactional separados) | ordem / entrada stock / consumo — **3 TX separados** | ordem COMPLETED sem stock (falha entre saves) | **FRÁGIL** (BUG-041) |
| Fecho de caixa | Sim | session+systemValue | rollback | OK |
| Backup | — | registo BD + ficheiro | registo sem ficheiro | **FALHA** (BUG-001) |

**Concorrência (cenários → risco):**
| Cenário | Mecanismo | Risco |
|---|---|---|
| 2 vendas mesmo artigo (stock) | @Version stock_branch | conflito → 1 falha com 500 cru (sem retry) — **dados OK, UX má** |
| 2 vendas crédito mesmo cliente (limite) | sem lock | **limite ultrapassado + lost update** (BUG-033) |
| 2 pagamentos mesma FT | sem lock | **pago em dobro** (BUG-012) |
| 2 pagamentos fornecedor mesma compra | sem lock | **pago em dobro** |
| 2 transferências mesma série (filial) | max+1 sem lock | **número duplicado** (sem constraint!) |
| venda + anulação simultâneas | @PreUpdate | um dos dois falha (500) |
| 2 utilizadores editam mesmo cliente | sem lock | lost update |
| Numeração de vendas | series_counters FOR UPDATE + synchronized | **OK** (mitigado) |
| hashControl | MAX+1 synchronized | OK intra-JVM (single desktop) |

---

## 15. SEGURANÇA / RBAC (17 / 41.H)

**Modelo:** `users → user_roles → roles → role_permissions` (string `PAGE:ACTION` com wildcards `*:*`, `PAGE:*`, `*:ACTION`). Verificação em 2 camadas: UI (menu/botões) + **alguns** services (`SaleService`, `CashSessionService`, `StockBranchService.adjustStock/deleteStock`, `WarehouseTransferService`). **Sem Spring Security no runtime** (só `spring-security-crypto` para BCrypt).

### Matriz ROLE × FUNCIONALIDADE × OPERAÇÃO (papel default do `DataInitializer`)

| Operação (permissão) | ADMIN | GESTOR | CAIXA | Serviço valida? | UI valida? |
|---|---|---|---|---|---|
| Resumos/VIEW | ✅ | ✅ | ✅ | parcial | ✅ |
| VENDAS:CREATE (vender) | ✅ | ✅ | ✅ | ✅ SaleService | ✅ |
| VENDAS:DELETE (anular) | ✅ | ❌ | ❌ | ✅ SaleService | ✅ |
| VENDAS:CREATE p/ PAGAR (PaymentService) | ✅ (FINANCEIRO:VIEW?) | ✅ | ❌ (FINANCEIRO:VIEW) | **❌ NUNCA** | parcial (menu) |
| CAIXA:VIEW (ver caixa) | ✅ | ✅ | ✅ | ✅ | ✅ |
| Abrir/fechar caixa (CAIXA:VIEW como proxy) | ✅ | ✅ | ✅ | ✅ (VIEW) | ✅ |
| Movimento de caixa | ✅ | ✅ | ✅ | ✅ (VIEW) | ✅ |
| PRODUTOS:CREATE/DELETE | ✅ | ✅ | ❌ | **❌ ProductService sem check** | ✅ |
| CLIENTES:CREATE/DELETE | ✅ | ✅ | ❌ (CAIXA só VIEW) | **❌ CustomerService sem check** | ✅ |
| STOCK:CREATE (ajustar stock) | ✅ | ✅ | ❌ | ✅ (adjustStock) + "admin por nome" | ✅ |
| **initializeStock (stock inicial)** | ✅ | ✅ | ❌ | **❌ NENHUMA** | ✅ |
| ARMAZENS:CREATE/DELETE | ✅ | ✅ (CREATE) | ❌ | **❌ WarehouseService sem check** | ✅ |
| COMPRAS:CREATE | ✅ | ✅ | ❌ | **❌ PurchaseService sem check** | ✅ |
| TRANSFERENCIAS:CREATE (TWA) | ✅ | ✅ | ❌ | ✅ | ✅ |
| **Transferências filial (create/approve/complete)** | ✅ | ✅ | ❌ (UI) | **❌ NENHUMA** | ✅ (menu) |
| Pagamentos a fornecedor | ✅ | ✅ | ❌ (UI) | **❌ NENHUMA** | ✅ (menu) |
| PRODUÇÃO (guardar ordem) | ✅ | ✅ | ❌ | **❌ NENHUMA** | ✅ (menu) |
| RELATORIOS:VIEW | ✅ | ✅ | ❌ | n/a (service público) | ✅ (menu) |
| SISTEMA:CREATE/DELETE (utilizadores) | ✅ | ❌ | ❌ | **❌ (deleteUser usa `canManageSystem()` no controller)** | ✅ |
| Backup/Restauro | ✅ | ❌ | ❌ | **❌ NENHUMA** | ✅ (menu) |
| Licença activar | ✅ | ❌ | ❌ | **❌ NENHUMA** | ✅ (menu) |
| Modo treino | ✅ | ❌ | ❌ | **❌ NENHUMA** | ✅ (menu) |

**Conclusões de segurança:**
1. **A segurança NÃO pode depender só da UI** (regra da auditoria) — e não pode: 9 áreas de escrita sem verificação no service. Numa instalação com o menu mal configurado, ou com um chamador directo do Bean (ex.: future API, script, outro terminal na mesma JVM), qualquer utilizador autenticado cria/anula/compra/transfere. **ALTO.**
2. **VIEW como proxy de EXECUTE**: `CAIXA:VIEW` permite abrir/fechar/caixar; `TRANSFERENCIAS:VIEW` permite receber (mover stock). Granularidade insuficiente. **MÉDIO.**
3. **"Admin por nome do papel"** (`role.name.contains("ADMIN")`) em stock — um papel `SALES_ADMIN` teria poderes de admin de stock; um `ADMIN` descontinuado deixaria de ser admin. **MÉDIO.**
4. **Revogação ineficaz**: desactivar utilizador é revertido no arranque (BUG-006). **CRÍTICO.**
5. **forceChangePassword ineficaz** (BUG-021). **ALTO.**
6. **Sem lockout/timeout de sessão** (brute-force local; sessão JavaFX sem expiração). **BAIXO/MÉDIO** (single-machine).
7. **BCrypt** (strength default 10) — OK. Palavra-passe mínima 6 — **fraca (MÉDIO)**.
8. **Licença forjável** (BUG-027). **ALTO (business).**
9. **Auditoria**: login falhado/sucesso, logout, anulações, backups, settings — presentes; **ausentes**: pagamentos a fornecedor, NC, anulação (nível service), edição de preços (só texto livre, sem antes/depois). **MÉDIO.**
10. **Triggers fiscais** (protecção de documentos) — bons; mas dependem de MariaDB (H2/testes sem) e o fluxo de recibo pode colidir (BUG-040).

---

## 16. PERFORMANCE (41.I)

| # | Problema | Evidência | Impacto |
|---|---|---|---|
| P1 | `saleRepository.findAll()` em memória em: `ReportService.period/topProducts/paymentMethods/hashHistory/hashAudit/getAccountsReceivableAging`, `DailyReconciliationService`, `ComplianceDashboardService`, `SeriesManagementService.status()`, `DashboardCrudManager.loadFinanceiro` | vários | **O(n) total** — degradação linear com o histórico; 100k vendas = minutos + memória |
| P2 | `DashboardService.getLowStock` carrega **todo** o `stock_branch` | L74–84 | idem (e é código morto — mas o padrão repete-se em `loadStock`) |
| P3 | `searchForList` com `CAST(documentNumber AS string) LIKE` — impede índice no nº | SaleRepository | scan por busca no nº |
| P4 | `findByDateRangeAndState` com `BETWEEN` em `created_at` — índice existe (`idx_sales_created_at`) — OK; mas sem filtro de filial em vários mapas (filtro Java pós-consulta) | ReportsController | transfere mais do que precisa |
| P5 | PDV: `allProducts` + `branchStockCache` em memória (OK, correcto); `findTopSellingProductsToday` 90 dias por abertura de PDV | SaleFormController | aceitável |
| P6 | `@EnableCaching` com `simple` cache e **sem evictions** (DashboardService morta) | CacheConfig | armadilha futura (stale KPIs) |
| P7 | N+1: `loadSales` faz `LEFT JOIN FETCH customer+items` (OK); mas listagens que tocam `sale.getItems()`/`payments` fora de TX → lazy init (risco de exception, não só perf) | ReportService | ver BUG-031 |
| P8 | `synchronized` em `SaleNumberingService` (singleton) serializa **todas** as numerações de todas as filiais | SaleNumberingService | throughput do PDV sob carga multi-filial (baixo impacto desktop) |

---

## 17. UX (41.J)

| # | Problema | Onde |
|---|---|---|
| U1 | **Botões de "sucesso" que mentem** (Backup criado!; Restore concluído!) | Sistema → Backups |
| U2 | Erro SQL cru ao editar documento emitido ("Documentos fiscais não podem…") em vez de desabilitar o botão | Histórico → Editar |
| U3 | Login mostra `Linha X do Y` do stack trace | Login |
| U4 | Carrinho suspenso partilhado entre utilizadores (aparece o "Recuperar (3)" do anterior) | PDV |
| U5 | F10/ESC globais no PDV — ESC limpa o carrinho **sem confirmação** (perda de trabalho) | PDV |
| U6 | Estado "PAGO" em fatura a crédito confunde no histórico | Histórico |
| U7 | Mensagem de stock insuficiente correcta, mas **conflito de stock (500)** aparece como "Erro ao salvar a operação: <stack>" | PDV |
| U8 | Cap 200 linhas no histórico sem aviso ("há mais") | Histórico |
| U9 | Pesquisa sem normalização de acentos (frustração em MZ: "café" vs "cafe") | PDV/listagens |
| U10 | Fecho de caixa: diferença visível mas não guardada; sem relatório de quebras históricas | Caixa |
| U11 | "Programação de backup" dá falsa sensação de controlo | Sistema |
| U12 | Moeda USD/EUR/ZAR no PDV sem conversão (total enganoso) | PDV |
| U13 | Produção: "guardar" = produzir imediatamente, sem vista de pendentes/progresso | Produção |
| U14 | Anulação exige motivo (bom) mas o dialog não valida motivo em branco de forma consistente em todos os fluxos (TWA cancel sem motivo) | Transferências |

---

## 18. TESTES END-TO-END (30) — veredicto por análise de código

| Teste | Etapas percorridas no código | Veredicto | Quebra encontrada |
|---|---|---|---|
| **T1 Venda em dinheiro** | login → abrir caixa (OK) → pesquisar (OK) → adicionar (OK) → qtd (OK) → total (OK) → recebido/troco (UI OK / **BD descarta**) → finalizar (OK, TX) → documento (PDF OK, **sem QR**) → baixa stock (OK) → caixa IN (OK p/ cash) → dashboard (KPI OK p/ cash **mas inclui demo/cotações**) → histórico (OK) | **PARCIAL** | BUG-008 (troco), BUG-016 (QR), BUG-004 (dashboard) |
| **T2 Venda a crédito** | cliente (OK) → limite (validação **OK** na convenção saldo+) → venda (OK) → dívida (OK, sem lock) → CC (OK) → **fatura pendente** (estado **PAGO** ✗) → pagamento parcial (OK, race ✗) → saldo (OK) → reconciliação (**FIFO a cotações** ✗) → pagamento total (OK) → dívida=0 (OK) | **PARCIAL→FALHA** | BUG-009, BUG-033, BUG-015, BUG-007 |
| **T3 Compra** | fornecedor (OK) → compra (OK, sem RBAC) → entrada stock **armazém** (OK) → CMP (OK em double; edição distorce) → **dívida fornecedor** (implícita — sem conta corrente) → pagamento (OK; caixa frágil; sem RBAC/audit) → caixa (só "dinheiro") → relatório (OK) → **ponte p/ filial ausente no fluxo natural** | **PARCIAL** | BUG-013, BUG-019, BUG-020 |
| **T4 Transferência** | armazém A → guia (OK, stock validado) → retirar (**só em COMPLETED**) → IN_TRANSIT (approve) → receber no B (OK, re-valida) → concluir (OK) → kardex (OK) | **PARCIAL** | BUG-018 (complete de PENDING; VIEW executa) |
| **T5 Produção** | BOM (OK) → ordem (**nascida COMPLETED**) → consumo MP (**catch vazio; negativo permitido**) → stock acabado (OK se branch≠null) → kardex (parcial) → custo (**sem roll-up**) | **FALHA** | BUG-002, BUG-041 |
| **T6 Fecho de caixa** | abertura (OK) → vendas (OK) → recebimentos (**todos os métodos contam** ✗) → sangria/reforço (OK) → despesa (OK) → contagem (OK) → fecho (OK) → quebra/sobra (**esperado contaminado** ✗; não persistida) → Fita Z (display OK) → histórico (OK) | **FALHA** (p/ negócio com meios electrónicos) | BUG-003, BUG-051 |

---

## 19. RECOMENDAÇÕES — TESTES DE REGRESSÃO A ADICIONAR (mínimo viável)

1. **Invariante financeira** (teste de BD por operação): `SUM(cash_movements IN/OUT por sessão) + fundo == systemValue no fecho`; `SUM(payments) == Σ paidAmount das vendas`; `SUM(stock_branch.current) == Σ stock_movements.qty` por (filial, artigo); `customers.balance == Σ ledger`.
2. **E2E venda cash**: após venda, assert em `sales`, `sale_items`, `stock_branch`, `stock_movements`, `cash_movements`, `audit_logs` (1 teste já existe parcialmente — `SaleServiceTest` — expandir para caixa e kardex).
3. **E2E crédito completo**: venda→parcial→total→dívida 0; **NC sobre crédito**→saldo abate; **anulação**→caixa OUT existe.
4. **Concorrência**: 2 threads × (venda crédito mesmo cliente; pagamento mesma FT; stock mesmo artigo; numeração transferências) — 4 testes.
5. **Estado**: venda a crédito → `EMITIDA`; parcial → `PAGO_PARCIAL`; total → `PAGO`; `SaleState.fromString("PAGO_PARCIAL")` == PAGO_PARCIAL (hoje: EMITIDA!).
6. **Relatórios de paridade**: para um dataset conhecido, `dashboard.hoje == mapa.vendas == apuramento.universo` e `extrato.fim == customers.balance`.
7. **Backup/restore**: `backupNow()` cria ficheiro com size>0; restore devolve o dataset (teste com MariaDB em container).
8. **RBAC**: papel CAIXA → `TransferService.create` deve lançar; `PurchaseService.savePurchase` deve lançar; `SupplierPaymentService.savePayment` deve lançar; `StockBranchService.initializeStock` deve lançar.
9. **Fiscal**: cadeia `signatureHash` de 3 docs na mesma série verificável; QR presente no PDF (bytes QR detectáveis); recibo com série RB própria.
10. **Produção**: MP insuficiente → ordem não gravada/stock intacto; stock MP nunca <0.
11. **Arranque**: utilizador desactivado permanece desactivado após restart; `admin` criado só se BD vazia; venda demo com `demoFlag=true`.
12. **Licença**: chave de máquina A rejeitada em B; sem chave → trial com prazo; expirado → escrita bloqueada.

---

## 20. VEREDICTO FINAL (43)

**Pergunta:** *Se um utilizador real utilizar este sistema de ponta a ponta, tudo funcionará correctamente e permanecerá consistente?*

**Resposta: NÃO.**

Com evidência por código (runtime não testável neste ambiente — §0):

1. **O dinheiro não reconcilia**: meios electrónicos entram no caixa físico (BUG-003); o troco é descartado (BUG-008); anulações/NCs não estornam o caixa em silêncio (BUG-010/011); pagamentos sem factura não entram no caixa (BUG-014). A fita Z e a reconciliação diária são **inalcançáveis** num negócio com M-Pesa/POS.
2. **Os números divergem entre si**: dashboard ≠ mapa de vendas ≠ apuramento de IVA ≠ caixa ≠ extrato do cliente (BUG-004/005/038/031). O apuramento de IVA — o número mais sensível fiscalmente — **não pode ser usado para declaração** (universo errado + base por documento + cotações).
3. **A recuperação de desastre não existe**: backup falso e restauro inexistente (BUG-001) — um único acidente (disco, ransomware, erro humano) é irreversível.
4. **A conformidade AT é aparente**: QR ausente, hash simplificado, cadeia incorrecta, submissão nunca feita, recibo sem série (BUG-016/010/012).
5. **O controlo interno tem buracos**: transferências/compras/pagamentos sem RBAC no backend (BUG-017/019/020), revogação de acesso revertida no arranque (BUG-006), password forçada ineficaz (BUG-021), licença forjável (BUG-027), double-spending por concorrência (BUG-033/012).
6. **A produção é não-fiel**: consumo de matérias-primas em silêncio e stock negativo (BUG-002).

**O que funciona bem (reconhecido com evidência):** a core transaccional da venda (atomicidade stock→caixa→documento, com rollback correcto), a numeração fiscal atómica (`series_counters` + `FOR UPDATE`), o bloqueio optimista de stock (V25), as transferências TWA (estado + re-validação + transacção), o PDV (debounce, barcode, atalhos, validações em tempo real), os triggers de protecção fiscal, a auditoria de login/anulação/backup, o modelo de dados (BigDecimal pós-V17/V22, constraints, índices) e uma suite de testes que cobre os fluxos principais de stock/venda/pagamentos (sem contudo cobrir relatórios, caixa multi-método, backup, concorrência).

**Plano de remediação sugerido (ordem):**
- **Sprint 1 (P0, ~2 semanas):** BUG-001 (backup/restore real), BUG-003 (caixa por método), BUG-002 (produção atómica), BUG-004/028 (predicado reportável partilhado + IVA por linha), BUG-005/007 (ledger de CC + pendências por docType), BUG-006 (seeds idempotentes seguros).
- **Sprint 2 (P1):** BUG-008/009 (troco + estados), BUG-010/011 (NC/anulação simétricas), BUG-012/033 (locks + RBAC em pagamentos), BUG-016 (cadeia/QR), BUG-017/019/020 (RBAC services), BUG-021/022/027 (password forçada, câmbio, licença), BUG-013 (ponte stock armazém→filial), BUG-041 (estado de produção + custo).
- **Sprint 3 (P2/P3):** ligar órfãos (lotes, PO, apuramento, WhatsApp) ou remover; auditoria antes/depois; normalização de acentos em pesquisa; persistir quebra de fecho; relatórios de comissões; testes de regressão do §19.

---
*Fim do relatório. Cada afirmação marcável por "evidência no código" aponta para ficheiro+método inspecionados nesta auditoria. Nenhuma linha de cobertura foi marcada OK sem evidência; as linhas que requerem confirmação em runtime estão identificadas em §0 e nos "Evidência durante execução" de cada bug.*

---

## ANEXO — Correções aplicadas neste repositório (Sprint 1, pós-auditoria)

> **Nota de leitura:** o corpo do relatório (seções 0–20) é o registo do
> estado do código **antes** das correções; os contadores mantêm-se intactos.
> Este anexo documenta as correções P0 (Sprint 1) já aplicadas na branch de
> trabalho, com o mesmo critério de evidência da auditoria: tudo o que aqui se
> afirma foi verificado por leitura de código. **Nenhuma compilação nem
> execução foi possível neste ambiente** (sem JDK/Maven/MariaDB) — a
> verificação de runtime fica para o primeiro `mvn test` com JDK 21 e para a
> suite de regressão do §19 (inventário e estado em `TESTES_REGRESSAO.md`).

### A1. BUG-001 — Backup/restore real (CRÍTICO) + BUG-047 (BAIXO)
- `service/BackupService.java`: novo `runBackup(type, createdBy)` — mysqldump
  real, **COMPLETED apenas se o ficheiro existir com conteúdo** (caso
  contrário `FAILED` com erro), sempre com checksum **SHA-256** gravado;
  novo `restoreFrom(sqlFile, expectedChecksum)` — aplica o dump via cliente
  `mysql` (equivalente a `mysql < backup.sql`) com validação de integridade
  **antes** de aplicar (checksum divergente → restore bloqueado);
  `scheduledBackup()` (BUG-047) deixa o cron fixo `0 0 3 * * *` / retenção 7
  dias e passa a respeitar `autoBackupEnabled`, `backupFrequency`
  (Diario/Semanal/Mensal), `backupHour` e `backupRetentionDays` do
  `app_config` (disparo horário com controlo de "já correu hoje").
- `desktop/SystemSettingsPageManager.java` (`createBackupNow`,
  `confirmRestore`) e `desktop/SistemaModuleController.java` (`onBackupNow`,
  `onRestoreBackupFile`; `onRestoreBackup` — botão antes morto, ficou a
  restaurar um dump .sql escolhido) — a cópia fictícia de `sgv.db` e a cópia
  para `C:\xampp\mysql\data\sgv` foram substituídas pelos fluxos reais; o
  estado `RESTORED` só é gravado se o `mysql` terminar com código 0 e os
  registos `RESTORE_*` passam a reflectir o resultado real (COMPLETED/FAILED
  com detalhe).
- `entity/SystemBackup.java` + `V29__system_backups_checksum.sql`: nova
  coluna `checksum_sha256`.
- Teste: `BackupRestoreTest` (continua `@Disabled` — requer MariaDB real em
  CI), já apontado para o novo fluxo (`runBackup`/`restoreFrom`).

### A2. BUG-002 — Produção atómica (CRÍTICO)
- `service/ProductionService.java` (novo): `produce(ordem, filial, operador)`
  — pré-verificação de matérias-primas (`listIngredientShortages`), consumo,
  entrada do produto acabado e `COMPLETED` **na mesma transacção**; sem MP
  suficiente → `IllegalStateException` com a lista das faltas e nada é
  gravado (a ordem não fica "em produção" com stock negativo).
- `service/StockBranchService.java`: `consumeIngredientsForProduction`
  reescrito (bloqueio prévio, sem catch engolido, com guarda defensiva contra
  stock negativo) + novo `listIngredientShortages`.
- `desktop/ProductionOrderFormController.java`: `doSave` de nova ordem chama
  `productionService.produce` (operador/filial obrigatórios); edições mantêm
  o comportamento antigo — o ajuste de stock em edição é o BUG-041 (Sprint 2).
- Teste: `ProductionStockTest.production_insufficientMp_neverNegativeStock`.

### A3. BUG-003 — Só o numerário move a gaveta (CRÍTICO)
- `model/PaymentMethod.java`: novos `movesCashDrawer()` /
  `movesCashDrawer(String)` (apenas DINHEIRO move a gaveta; texto livre
  normalizado por `fromString`).
- Aplicado em: `SaleService.persistSaleTransaction` (entrada da venda),
  `SaleService.annulSale` (estorno **simétrico** — um M-Pesa/POS que nunca
  entrou na gaveta não sai dela na anulação), `PaymentService.createPayment`,
  `CustomerAccountService.recordReceipt`, `SupplierPaymentService.savePayment`
  (deixou de testar `substring("dinheiro"/"caixa")`). A reconciliação de CC
  mantém a entrada em caixa incondicional (caixa física por natureza).

### A4. BUG-004/028/031 — Universo reportável partilhado + IVA por linha
- Predicado reportável único — `documentType IN (VENDA, FACTURA, NC)`,
  `state <> 'ANULADA'`, não demo (a NC entra com total negativo e abate o
  apuramento) — aplicado em:
  - `repository/SaleRepository.java`: `sumTotalAll`,
    `sumTotalByDateRangeAndBranch`, `countByDateRangeAndBranch`,
    `sumTotalByPaymentMethodToday`, `sumTotalByMonthAndYear`;
    `sumTotalPendingCreditsByBranch` limitado a VENDA/FACTURA (alinha o KPI
    "Contas a Receber" com `findPendingSales`, BUG-007);
  - `service/ReportService.java`: novo `isReportable`; `period`,
    `topProducts` (antes só `state='EMITIDA'` — BUG-031; a parte do risco de
    lazy-init de BUG-031 mantém-se), `paymentMethods` e
    `getAccountsReceivableAging` (antes contava cotações abertas como dívidas);
  - `desktop/DashboardKpiManager.java`: corrigido indirectamente pelos
    agregados acima (KPI "hoje", gráfico 7 dias, pizza por método, cartões de
    mês/semana/total);
  - `desktop/ReportsController.java`: `loadVendasData` (antes sem qualquer
    filtro de estado), `loadIvaData` — base tributável/isenta agora apurada
    **por linha** (BUG-028: um documento com uma única linha isenta já não
    desclassifica a base inteira; NC abate a base com sinal) — e
    `refreshTopSold`/exportação SAF-T MZ (exclui cotações/encomendas/demo;
    as ANULADAS permanecem no SAF-T com estado "A", como o esquema prevê).
- Teste: `ReportParityTest.samePeriod_allViewsSameUniverse`.

### A5. BUG-005/010/014 — Livro de conta corrente do cliente
- `entity/CustomerAccountEntry.java` +
  `repository/CustomerAccountEntryRepository.java` +
  `V28__customer_account_ledger.sql`: lançamentos imutáveis com valor
  **sinalizado** (+ aumenta a dívida do cliente, − diminui).
- `service/CustomerAccountLedger.java` (novo): **ponto único de escrita** de
  `customer.balance` — saldo e lançamento na mesma transacção; a invariante
  Σ(lançamentos) == saldo passa a ser estrutural (os 5 sítios de escrita
  directos da auditoria foram migrados; verificação por grep: só o ledger
  ancora `setBalanceAmount` em `src/main`).
- Lançamentos: venda a crédito (`SaleService`, `CREDITO_SALE`), anulação a
  crédito (`ANNULMENT`), pagamento (`PaymentService`, `PAYMENT`), recibo com
  factura (`PAYMENT`) e **sem factura** (`RECEIPT_WITHOUT_SALE` — este último
  é o BUG-014: antes baixava o saldo sem rastro no extrato), reconciliação
  (`RECONCILIATION`) e crédito manual da UI (`DashboardCrudManager`,
  `ADJUSTMENT` — o 6.º sítio de escrita, encontrado durante a implementação).
- BUG-010: `SaleService.createCreditNote` agora reverte a venda original —
  crédito → lançamento `CREDIT_NOTE` (a dívida baixa); caixa → estorno `OUT`
  na gaveta **sem catch engolido** (sem turno aberto a NC não é emitida —
  rollback total, em vez de gaveta com dinheiro a mais).
- `CustomerAccountService.getCustomerStatement`: extrato construído a partir
  do livro (mesma origem do saldo; linha de "saldo de abertura" para bases
  pré-V28) com fallback para a visão legada em clientes sem lançamentos.
- Testes: `FinancialInvariantsTest.customer_receiptWithoutSale_statementStillMatchesBalance`,
  `CreditCycleE2ETest.creditNoteOnCreditSale_decreasesCustomerBalance`,
  `ReportParityTest.customerStatement_finalEqualsRealBalance`.

### A6. BUG-006 — Seeds idempotentes seguros (CRÍTICO)
- `config/DataInitializer.java`: `ensureDefaultUsers()` não executa se já
  existirem utilizadores (o `admin` desactivado **não volta** no restart);
  `ensureUser` deixa de forçar `setActive(true)`/`setCanViewStats(true)` em
  utilizadores existentes; a venda demo do seed passa com `demoFlag=true`
  (deixa de contaminar os apuramentos — sinergia directa com A4).
- Testes: `StartupSeedTest` (3 testes antes a vermelho ficam verdes).

### A7. BUG-007/015 — Pendências apenas VENDA/FACTURA (ALTO)
- `SaleRepository.findPendingSales` / `findPendingByCustomerId`:
  `documentType IN ('VENDA','FACTURA')` (a FIFO de reconciliação deixava de
  alocar pagamentos a cotações/encomendas); `PaymentService.createPayment`:
  guarda explícita contra liquidar documentos pré-venda, com mensagem que
  orienta a conversão em factura.

### A8. Testes de regressão do §19
- 12 novas classes em `src/test/java/com/sgv/service/` (42 testes) —
  inventário, estado actual e convenções em `TESTES_REGRESSAO.md`, incluindo a
  secção "Efeito do Sprint 1" com o mapa teste→correção.
- Ajustes mínimos em 3 testes Mockito pré-existentes (`SaleServiceTest`,
  `CustomerAccountServiceTest`, `CustomerAccountServiceReconcileTest`) para o
  novo construtor dos services — o ledger é um objecto real sobre os mesmos
  mocks, pelo que o comportamento de saldo testado é idêntico.

### A9. O que **não** foi alterado (deliberadamente fora do Sprint 1)
- BUG-008 (troco), BUG-009 (estados/enum `PAGO_PARCIAL`), BUG-011 (estorno
  atómico da anulação — o catch-ignore em `annulSale` mantém-se),
  BUG-012 (locks/recibo com série própria), BUG-016 (cadeia por série/QR),
  BUG-017/019/020/024 (RBAC nos services), BUG-021/022/027, BUG-033,
  BUG-041, BUG-049/050 (auditoria antes/depois, despesas) e os orfãos (§13) —
  todos mantêm o estado documentado no corpo do relatório; os testes do §19
  que os cobrem ficam a vermelho de propósito até à Sprint 2/3.

### A10. Verificação pendente (obrigatória antes de produção)
1. `mvn test` completo com JDK 21 (H2) + `BackupRestoreTest` desactivado a
   menos em CI com container MariaDB + `mysqldump`/`mysql` no PATH.
2. Exercício manual do ciclo de conta corrente: venda a crédito → pagamento
   parcial → NC integral → recibo sem factura → extrato tem de fechar no
   saldo real.
3. Exercício de integridade: backup → alterar 1 byte no ficheiro → restore
   (tem de ser bloqueado pelo checksum SHA-256).
4. Revisão do `git diff` pelos donos de domínio (finanças/AT) antes do merge.
