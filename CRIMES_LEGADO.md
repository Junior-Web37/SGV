# 🚨 CRIMES DO CÓDIGO LEGADO — LEVANTAMENTO FORENSE (FASE 1)

Este documento lista as 10 piores violações estruturais, de segurança, fiscais e de integridade encontradas no código-fonte original antes da refatoração completa para o padrão de produção em Moçambique.

---

### 🔴 CRIME 1: Liquidação Fictícia & Recebimentos Duplicados sobre Vendas já Pagas
* **Ficheiro:** `java-sgv/src/main/java/com/sgv/service/PaymentService.java` (Linha 28) & `SaleRepository.java` (Linha 48)
* **Violação:** O método `findRecentActiveSales` usava a query `SELECT s FROM Sale s WHERE s.state != 'CANCELLED'`, retornando **todas as vendas do sistema**, inclusive as já liquidadas a 100% no balcão (`state = 'PAGO'`).
* **Impacto:** Ao selecionar uma venda de 5.000 MT já paga e clicar em salvar, o sistema executava `sale.setPaidAmount(paidAmount + amount)`, inflando o total pago para 10.000 MT, duplicando a receita contabilística e emitindo recibos sobre dívidas inexistentes.

---

### 🔴 CRIME 2: Race Condition Crítica na Numeração de Facturas e Hash Fiscal AT
* **Ficheiro:** `java-sgv/src/main/java/com/sgv/service/SaleNumberingService.java` (Linhas 18-28)
* **Violação:** Obtenção de número sequencial de fatura através de `SELECT MAX(document_number)` sem lock de linha (`FOR UPDATE`) ou sequência atómica.
* **Impacto:** Dois caixas a faturar no mesmo segundo recebiam o mesmo número de fatura (`FT A/105`), gerando colisão de chaves no banco ou faturas duplicadas que quebravam a cadeia de assinatura digital SHA-256 exigida pela Autoridade Tributária de Moçambique.

---

### 🔴 CRIME 3: Vazamento de Material Criptográfico em Arquivos de Log (OWASP CWE-532)
* **Ficheiro:** `java-sgv/src/main/java/com/sgv/service/DesktopAuthService.java` (Linhas 30 e 36)
* **Violação:** O serviço de autenticação gravava em disco (`sgv.log`) os primeiros 10 a 20 caracteres do hash BCrypt do utilizador (`hash_prefix=...`) em tentativas de login falhadas e bem-sucedidas.
* **Impacto:** Exposição de salts e prefixos de hash de senhas de administradores e operadores no sistema de arquivos local.

---

### 🔴 CRIME 4: Bloqueio de Conexões de Base de Dados por I/O Lento em `@Transactional`
* **Ficheiro:** `java-sgv/src/main/java/com/sgv/service/SaleService.java` (Linhas 65-247)
* **Violação:** A renderização de documentos PDF (Apache PDFBox), carregamento de fontes e comunicação com impressoras térmicas eram executados **dentro** do método anotado com `@Transactional`.
* **Impacto:** Conexões do pool HikariCP e locks de tabelas eram retidos durante segundos enquanto o disco escrevia o arquivo, causando esgotamento do pool de conexões sob carga no PDV.

---

### 🔴 CRIME 5: Ausência de Bloqueio Otimista (`@Version`) no Controlo de Estoque
* **Ficheiro:** `java-sgv/src/main/java/com/sgv/entity/StockBranch.java` & `StockWarehouse.java`
* **Violação:** As tabelas de inventário não possuíam controlo de versão nem lock optimista/pessimista.
* **Impacto:** Vendas simultâneas da última unidade em caixas diferentes realizavam *Lost Updates*, vendendo mais artigos do que as existências físicas reais e gerando stock negativo sem disparo de exceção.

---

### 🔴 CRIME 6: `NullPointerException` Imediato no Formulário de Fabrico / Padaria
* **Ficheiro:** `java-sgv/src/main/java/com/sgv/desktop/ProductionOrderFormController.java` (Linhas 22, 25, 47)
* **Violação:** O controlador declarava `@FXML private ComboBox<String> stateCombo;` e `@FXML private TextField unitField;` que **não existiam** no arquivo FXML (`production_order_form.fxml`), invocando métodos sobre referências nulas no `initialize()`.
* **Impacto:** Impossibilidade de abrir ou gravar qualquer ordem de produção de padaria/pastelaria.

---

### 🔴 CRIME 7: Entidades Mapeadas sem Tabelas no Schema Flyway
* **Ficheiro:** `java-sgv/src/main/java/com/sgv/entity/PaymentAllocation.java`
* **Violação:** A entidade `@Entity PaymentAllocation` era persistida em regras de negócio de reconciliação de crédito (`CustomerAccountService`), mas a tabela `payment_allocation` não existia em nenhum script de migração Flyway (`V1` a `V22`).
* **Impacto:** Falha em validação de schema do Hibernate (`ddl-auto=validate`) e erro SQL `Table doesn't exist` ao reconciliar contas correntes.

---

### 🔴 CRIME 8: Esgotamento de Threads por Spawning Cru sem Pool (`Thread Storm`)
* **Ficheiro:** Presente em mais de 38 ficheiros (`CustomerFormController`, `ProductFormController`, `SaleFormController`, etc.)
* **Violação:** Uso indiscriminado de `new Thread(task).start()` para tarefas em segundo plano (como debounce de pesquisa e validação de duplicados).
* **Impacto:** Sob digitação rápida, dezenas de threads nativas do sistema operativo eram criadas e destruídas sem controlo de pool, elevando o consumo de CPU e memória.

---

### 🔴 CRIME 9: Lançamento Ilimitado de Pagamentos a Fornecedores
* **Ficheiro:** `java-sgv/src/main/java/com/sgv/desktop/SupplierPaymentFormController.java` (Linhas 77-84)
* **Violação:** Listagem de todas as compras do fornecedor, inclusive as já pagas a 100%, sem validação de saldo devedor.
* **Impacto:** Possibilidade de lançar pagamentos acima da dívida real com o fornecedor, corrompendo o mapa de contas a pagar.

---

### 🔴 CRIME 10: Inexistência de Abate de Matérias-Primas por Ficha Técnica (BOM)
* **Ficheiro:** `java-sgv/src/main/java/com/sgv/service/StockBranchService.java`
* **Violação:** Ordens de produção de artigos de panificação davam entrada no produto final sem dar saída nos ingredientes (farinha, fermento, açúcar).
* **Impacto:** Estoque de matérias-primas ficava inflacionado indefinidamente, tornando o inventário contábil e físico da padaria completamente divergente.
