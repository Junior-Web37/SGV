# Testes de Regressão SGV (auditoria §19)

Doze classes de teste JUnit adicionadas em `java-sgv/src/test/java/com/sgv/service/`,
correspondentes às 12 áreas de risco identificadas na auditoria
(`AUDITORIA_SGV.md`, §19). Cada classe documenta, no Javadoc, o estado
esperado **antes** das correções ("FALHA AGORA" = o teste falha
intencionalmente porque fixa o comportamento correcto que o bug impede).

## Como correr

```bash
cd java-sgv
mvn test                          # suite completa (H2 in-memory, perfil "test")
mvn test -Dtest=FinancialInvariantsTest   # uma classe
```

> Nota de ambiente: a suite corre sobre H2 in-memory
> (`src/test/resources/application-test.properties`). Os testes que
> assinalam "NÃO TESTADO em runtime" na auditoria ficam por aqui; o teste de
> backup (§7) requer MariaDB real + `mysqldump` e está `@Disabled` até haver
> CI com container.

## Inventario

| # | Classe | §19 | Testes | Estado actual | Pasa após |
|---|---|---|---|---|---|
| 1 | `FinancialInvariantsTest` | 1 | 5 | 4 PASSA / 1 FALHA (BUG-014/005) | ledger de conta corrente |
| 2 | `SaleCashE2ETest` | 2 | 2 | 1 PASSA / 1 FALHA (BUG-049 auditoria) | reforço de auditoria |
| 3 | `CreditCycleE2ETest` | 3 | 4 | 2 PASSA / 2 FALHA (BUG-011, BUG-010) | anulação atómica + NC simétrica |
| 4 | `ConcurrencyTest` | 4 | 4 | 1 PASSA / 3 FALHA (BUG-033, 012, 017) | locks + constraint único |
| 5 | `SaleStateMachineTest` | 5 | 3 | 1 PASSA / 2 FALHA (BUG-009) | estados EMITIDA/PAGO_PARCIAL no enum |
| 6 | `ReportParityTest` | 6 | 2 | 2 FALHA (BUG-004, BUG-005/010/014) | universo reportável partilhado + ledger |
| 7 | `BackupRestoreTest` | 7 | 2 | @Disabled (requer MariaDB) | correção BUG-001 + CI com container |
| 8 | `RbacEnforcementTest` | 8 | 4 | 4 FALHA (BUG-017, 019, 020, 024) | `requirePermission` nos services |
| 9 | `FiscalChainTest` | 9 | 4 | 1 PASSA / 3 FALHA (BUG-016, 012) | cadeia por série + QR + série RECIBO |
| 10 | `ProductionStockTest` | 10 | 2 | 1 PASSA / 1 FALHA (BUG-002) | consumo atómico com bloqueio |
| 11 | `StartupSeedTest` | 11 | 4 | 1 PASSA / 3 FALHA (BUG-006) | seeds idempotentes seguros |
| 12 | `LicenseEnforcementTest` | 12 | 6 | 3 PASSA / 1 FALHA (BUG-027) + 2 @Disabled | trial com prazo + binding + bloqueio |

Total: **42 testes** (36 activos + 2 @Disabled por ambiente + 2 @Disabled por
API ainda inexistente).

## Efeito do Sprint 1 (correções P0 já aplicadas neste repositório)

As correções Sprint 1 (ver `AUDITORIA_SGV.md`, anexo final) tornam verdes,
**por construção de código** (a compilação/execução não é possível neste
sandbox — ver ressalva), os seguintes testes que ficavam a vermelho:

| Classe | Teste | Bug | Motivo de ficar verde |
|---|---|---|---|
| `FinancialInvariantsTest` | `customer_receiptWithoutSale_statementStillMatchesBalance` | BUG-014/005 | livro de conta corrente: recibo sem factura grava `RECEIPT_WITHOUT_SALE` |
| `CreditCycleE2ETest` | `creditNoteOnCreditSale_decreasesCustomerBalance` | BUG-010 | NC sobre venda a crédito abre `CREDIT_NOTE` (saldo −valor) + stock restaurado |
| `ReportParityTest` | `samePeriod_allViewsSameUniverse` | BUG-004 | agregados (KPI, `period()`, relatório, SAF-T) usam o mesmo universo reportável |
| `ReportParityTest` | `customerStatement_finalEqualsRealBalance` | BUG-005/010/014 | extrato construído a partir do livro (mesma origem do saldo) |
| `ProductionStockTest` | `production_insufficientMp_neverNegativeStock` | BUG-002 | pré-verificação de matérias-primas + produção atómica (`ProductionService`) |
| `StartupSeedTest` | `demoSale_isMarkedAsDemo` | BUG-006 | venda demo do seed passa com `demoFlag=true` |
| `StartupSeedTest` | `deactivatedAdmin_staysDeactivatedAfterRestart` | BUG-006 | `ensureUser` deixa de reactivar utilizadores existentes |
| `StartupSeedTest` | `adminNotReseededWhenUsersExist` | BUG-006 | seed de utilizadores bloqueado quando já existem utilizadores |

Ficam a **vermelho de propósito** (correkções P1 — Sprint 2, fora do âmbito
Sprint 1): `SaleStateMachineTest` ×2 (BUG-009), `CreditCycleE2ETest.annulCashSale_withoutOpenSession_mustFailAtomically` (BUG-011),
`ConcurrencyTest` ×3 (BUG-033/012/017), `RbacEnforcementTest` ×4 (BUG-017/019/020/024),
`FiscalChainTest` ×3 (BUG-016/012), `SaleCashE2ETest.cashSale_auditLogExists` (BUG-049),
`LicenseEnforcementTest.noActiveKey_trialHasRealDeadline` (BUG-027).

`BackupRestoreTest` continua `@Disabled` (requer MariaDB real + `mysqldump`),
mas o fluxo testado já é o da UI pós-correção: `backupService.runBackup`
(COMPLETED só com ficheiro real + checksum) e `restoreFrom` (mysql + validação
SHA-256).

## Convenções

- **Códigos únicos por classe** (prefixos `INV-`, `E2ECASH`, `CC…`, `CONC-`,
  `SM…`, `PARITY`, `RBAC`, `FISCAL`, `PROD…`, `PRD…`) para as classes não
  transaccionais (concorrência/arranque/licença) cujo setup commita; os
  asserts estão sempre delimitados por filial/cliente/artigo próprios.
- **Fluxos reais**: os testes invocam os services exactamente como a UI
  (`processAndSave`, `createPayment`, `createCreditNote`, `annulSale`,
  `savePurchase`, `create`, …) — não simulam a camada de serviço.
- **Testes de race** (classe 4): barreira `CountDownLatch` para maximizar a
  janela de colisão; após a correção (locks + constraint) deixam de ser
  probabilísticos.
- **Testes FALHA AGORA** não são ignorados: ficam a vermelho no `mvn test`
  até a correção correspondente ser aplicada — é a rede de segurança que
  impede regressão silenciosa do bug.

## Legenda de severidade dos bugs fixados por cada teste

Ver `AUDITORIA_SGV.md` §3 (top 20) e §4 (formato completo). Cada referência
`BUG-0xx` nos Javadocs aponta para a entrada correspondente.
