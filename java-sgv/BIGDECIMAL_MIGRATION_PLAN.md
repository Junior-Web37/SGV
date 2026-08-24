# Plano completo de correção para o SGV

## Objetivo
Corrigir em ordem de risco o sistema desktop SGV, restaurar a estabilidade de arranque, estabilizar o modelo monetário e fechar os gaps de negócio, segurança e persistência.

## Estado atual comprovado
A evidência do workspace confirma que:
- o build Maven consegue concluir com `BUILD SUCCESS` em modo package sem testes;
- o projeto usa Java 21, Spring Boot 3.5.16, JavaFX 21 e MariaDB/MySQL;
- o schema Flyway já está no nível `V14` de migração;
- a camada desktop consegue chegar à tela de login em runtime após o arranque Spring Boot.

## Causas raiz já identificadas
1. Modelagem monetária instável quando o sistema mistura `double` com regras fiscais e stock.
2. Fluxo de arranque desktop dependente de um ambiente de runtime coerente (Java 21 + profile + banco).
3. Migrações e schema de dados com histórico de correções parciais, o que exige cuidado de compatibilidade.
4. Regras de negócio críticas (venda, stock, caixa, documento fiscal) precisam de uma prova de regressão por cenário real.

## Estrutura de correção por fases

### Fase 1 — Baseline e prova de execução
Objetivo: congelar o estado atual com um ponto de validação objetivo.

Ações:
- manter uma baseline de build em `mvn -DskipTests package`;
- manter uma baseline de runtime em `java -jar target\java-sgv-0.1.0.jar`;
- registrar o profile ativo com `spring-boot.run.profiles=mysql` ou equivalente;
- guardar um pacote de backup da base antes de qualquer migração funcional.

Arquivos afetados:
- [pom.xml](java-sgv/pom.xml)
- [application.properties](java-sgv/src/main/resources/application.properties)
- [application-mysql.properties](java-sgv/src/main/resources/application-mysql.properties)
- [MainApp.java](java-sgv/src/main/java/com/sgv/desktop/MainApp.java)

Critério de aceite:
- build reprodutível;
- runtime reprodutível;
- login visível e contexto Spring montado.

### Fase 2 — Corrigir arranque e perfil de execução
Objetivo: remover qualquer ambiguidade do ambiente de execução.

Ações:
- padronizar o `JAVA_HOME` para JDK 21 em todos os comandos de launch;
- escolher um único perfil de runtime para uso local e documentá-lo;
- unificar o fluxo de inicialização desktop + Spring Boot em um único ponto de entrada garantido;
- verificar que a base MariaDB/MySQL está acessível antes de iniciar a UI.

Arquivos afetados:
- [MainApp.java](java-sgv/src/main/java/com/sgv/desktop/MainApp.java)
- [SgvApplication.java](java-sgv/src/main/java/com/sgv/SgvApplication.java)
- [application.properties](java-sgv/src/main/resources/application.properties)
- [application-mysql.properties](java-sgv/src/main/resources/application-mysql.properties)

Critério de aceite:
- arranque sem fallback oculto;
- contexto Spring criado sem erro de configuração;
- perfil de execução consistente entre build e runtime.

### Fase 3 — Estabilizar a modelagem monetária e quantitativa
Objetivo: remover toda a ambiguidade de cálculo e garantir precisão total.

Ações:
- usar `BigDecimal` como regra comum em todas as entidades monetárias;
- usar `BigDecimal.ZERO` como default; nunca `0` sem tipo explícito em cálculos; 
- trocar comparações de valores com `==` por `compareTo()`;
- aplicar `RoundingMode.HALF_UP` nos pontos de regra de negócio;
- manter quantidade e stock por `BigDecimal` quando o domínio exigir precisão decimal.

Entidades e serviços principais:
- [Product.java](java-sgv/src/main/java/com/sgv/entity/Product.java)
- [Sale.java](java-sgv/src/main/java/com/sgv/entity/Sale.java)
- [SaleItem.java](java-sgv/src/main/java/com/sgv/entity/SaleItem.java)
- [Purchase.java](java-sgv/src/main/java/com/sgv/entity/Purchase.java)
- [StockBranch.java](java-sgv/src/main/java/com/sgv/entity/StockBranch.java)
- [StockMovement.java](java-sgv/src/main/java/com/sgv/entity/StockMovement.java)
- [SaleService.java](java-sgv/src/main/java/com/sgv/service/SaleService.java)
- [StockBranchService.java](java-sgv/src/main/java/com/sgv/service/StockBranchService.java)

Critério de aceite:
- sem `double` em regras de total, imposto, desconto, stock e custo;
- arredondamento consistente nos cálculos;
- totais e stock calculados sem drift numérico.

### Fase 4 — Provar o fluxo crítico de venda/stock/caixa
Objetivo: garantir que o coração do sistema funciona como operação real.

Ações:
- validar o cenário de criação de venda completo;
- confirmar que `stockCurrent` é decrementado corretamente;
- confirmar que a venda gera os totais esperados;
- confirmar que a sessão de caixa e o saldo final permanecem consistentes;
- validar a sincronização entre UI, service, repository e schema.

Arquivos afetados:
- [SaleFormController.java](java-sgv/src/main/java/com/sgv/desktop/SaleFormController.java)
- [SaleService.java](java-sgv/src/main/java/com/sgv/service/SaleService.java)
- [StockBranchService.java](java-sgv/src/main/java/com/sgv/service/StockBranchService.java)
- [CashSessionService.java](java-sgv/src/main/java/com/sgv/service/CashSessionService.java)
- [CashSessionController.java](java-sgv/src/main/java/com/sgv/desktop/CashSessionController.java)

Critério de aceite:
- venda salva e stock atualizado sem inconsistência;
- caixa abre/fecha sem saldo divergente;
- operação com dados reais sem regressão funcional.

### Fase 5 — Fechar permissões, auditoria e segurança
Objetivo: transformar o sistema em um software auditable e controlável.

Ações:
- validar roles e permissões por ação;
- garantir que hash de password continue usando BCrypt e não viole o fluxo de login;
- assegurar auditoria de ações sensíveis e operações de gestão;
- controlar acesso a módulos e ações por definição estrutural, não por UI escondida.

Arquivos afetados:
- [DesktopAuthService.java](java-sgv/src/main/java/com/sgv/service/DesktopAuthService.java)
- [User.java](java-sgv/src/main/java/com/sgv/entity/User.java)
- [Role.java](java-sgv/src/main/java/com/sgv/entity/Role.java)
- [SecurityConfig.java](java-sgv/src/main/java/com/sgv/config/SecurityConfig.java)
- [AuditLogService.java](java-sgv/src/main/java/com/sgv/service/AuditLogService.java)

Critério de aceite:
- autenticação com hash forte;
- permissões aplicadas corretamentes;
- ações críticas rastreáveis em auditoria.

### Fase 6 — Fiscalidade, documentação e impressão
Objetivo: garantir que a camada fiscal e documental não se torne um ponto de falha silenciosa.

Ações:
- validar geração de documentos fiscais e recibos;
- confirmar que exportações e relatórios usam valores consistentes, sem drift; 
- cobrir os cenários de impressão térmica e HTML com prova real;
- garantir que os dados que entram em documento saem idênticos aos que ficaram persistidos.

Arquivos afetados:
- [FiscalService.java](java-sgv/src/main/java/com/sgv/service/FiscalService.java)
- [SaleDocumentService.java](java-sgv/src/main/java/com/sgv/service/SaleDocumentService.java)
- [PrintHtmlService.java](java-sgv/src/main/java/com/sgv/service/PrintHtmlService.java)
- [ThermalPrintService.java](java-sgv/src/main/java/com/sgv/service/ThermalPrintService.java)
- [ReceiptDeliveryService.java](java-sgv/src/main/java/com/sgv/service/ReceiptDeliveryService.java)
- [SafTExportService.java](java-sgv/src/main/java/com/sgv/service/SafTExportService.java)

Critério de aceite:
- documentos com totais reproduzíveis;
- exportação sem truncamento de valores;
- impressão consistente em cenário real.

### Fase 7 — Backup, restauração e integridade de schema
Objetivo: garantir continuidade operacional sem perda de integridade.

Ações:
- validar backup completo e restauração em ambiente de teste;
- comparar o schema pós-restauração com o estado Flyway da versão alvo;
- rejeitar imports ou restores que não respeitem a cadeia de migrações;
- garantir que o estado dos dados não fique inconsistente após import.

Arquivos afetados:
- [BackupService.java](java-sgv/src/main/java/com/sgv/service/BackupService.java)
- [SystemBackup.java](java-sgv/src/main/java/com/sgv/entity/SystemBackup.java)
- [SystemBackupRepository.java](java-sgv/src/main/java/com/sgv/repository/SystemBackupRepository.java)
- [V1__baseline.sql](java-sgv/src/main/resources/db/migration/V1__baseline.sql)
- [V7__decimal_monetary_columns.sql](java-sgv/src/main/resources/db/migration/V7__decimal_monetary_columns.sql)
- [V13__decimal_monetary_columns_fix.sql](java-sgv/src/main/resources/db/migration/V13__decimal_monetary_columns_fix.sql)
- [V14__product_flow_integrity.sql](java-sgv/src/main/resources/db/migration/V14__product_flow_integrity.sql)

Critério de aceite:
- backup e restore reproduzíveis;
- schema validado por Flyway;
- consistência pós-restore confirmada.

### Fase 8 — UX e tratamento de erro
Objetivo: reduzir riscos operacionais e travamentos causados por UI e validações inconsistentes.

Ações:
- padronizar mensagens de erro;
- impedir campos vazios ou inválidos que geram falhas ocultas;
- garantir que formulários usam o mesmo padrão de validação e cálculo;
- preparar telas com feedback claro de sucesso/erro.

Arquivos afetados:
- [BaseFormController.java](java-sgv/src/main/java/com/sgv/desktop/BaseFormController.java)
- [UiUtils.java](java-sgv/src/main/java/com/sgv/desktop/UiUtils.java)
- controllers e telas FXML do fluxo principal

Critério de aceite:
- sem falha silenciosa;
- formulário consistente entre módulos;
- recuperação clara do estado de erro.

## Ordem correta de execução
1. ambiente de boot e profile;
2. schema e migracões;
3. cálculo monetário e stock;
4. venda e caixa;
5. auditoria e permissões;
6. fiscalidade e documentos;
7. backup/restauração;
8. UX e regressão final.

## Verificações obrigatórias
A correção só está completa quando a evidência for confirmada com comandos reais.

Comandos de prova:
- `mvn -DskipTests package`
- `java -jar target\java-sgv-0.1.0.jar`
- smoke test de login
- smoke test de criação de venda
- smoke test de abertura/fecho do caixa
- smoke test de stock e transferências
- execução de backup e restauração em ambiente controlado

## Saída esperada do projeto
Depois do plano concluído, o SGV deve ficar em estado de:
- arranque durável e reproduzível;
- sem drift monetário;
- stock e vendas consistentes;
- auditoria e permissões verificáveis;
- exportação fiscal e backup operacionais.

## Observação final
A correção precisa seguir este encadeamento: primeiro `boot`, depois `dados`, depois `negócio`, depois `auditoria`, e só por fim `documento e exportação`. Isso evita que uma correção local esconda uma regressão em outra camada.
