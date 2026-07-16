# SGV Implementation Log

## Data
- **2026-06-08**

## O que foi feito
- Continuei o desenvolvimento do backend Java/Spring Boot em `java-sgv`.
- Adicionei endpoints REST para gestão de `branches`, `categories` e `suppliers`.
- Implementei CRUD básico com permissões de criação/alteração para `ADMIN` e `GESTOR`.
- Mantive o padrão de roles já existente: `ADMIN`, `GESTOR`, `CAIXA`, `CLIENTE`.
- Criei um log de implementação centralizado para registrar ações, alterações e próximos passos.
- Criei novos DTOs de entrada para `Branch`, `Category` e `Supplier`.
- Adicionei suporte a pagamentos com `PaymentController` e relacionamento `Sale` ↔ `Payment`.

## O que já estava feito
- Autenticação JWT e autorização com Spring Security.
- Entidades normalizadas em 3FN para `users`, `roles`, `customers`, `products`, `stock`, `sales`.
- Controladores de `product`, `customer`, `stock` e `sale` existentes.
- Repositórios JPA para as novas entidades.

## Ficheiros criados/alterados
- `java-sgv/src/main/java/com/sgv/controller/BranchController.java`
- `java-sgv/src/main/java/com/sgv/controller/CategoryController.java`
- `java-sgv/src/main/java/com/sgv/controller/SupplierController.java`
- `java-sgv/src/main/java/com/sgv/controller/PaymentController.java`
- `java-sgv/src/main/java/com/sgv/repository/PaymentRepository.java`
- `java-sgv/src/main/java/com/sgv/dto/PaymentRequest.java`
- `implementation-log.md`

## Próximas tarefas
1. Adicionar endpoints para gestão de `branches`, `categories` e `suppliers` com DTOs dedicados e validação de entradas.
2. Implementar histórico de estoque mais robusto e associar movimentos a utilizadores autenticados.
2.1 Adicionar validação básica de entrada (JSR-380 / Jakarta Validation) nos DTOs e handlers para devolver erros amigáveis.
3. Adicionar suporte a pagamentos de venda com `PaymentController` e `PaymentRequest`.
4. Criar testes de integração para garantir que o fluxo de vendas, stock e pagamentos funciona corretamente.
5. Validar e compilar com Maven assim que o ambiente tiver `mvn` instalado.

## Progresso adicional
- Criado `PaymentRepository`, `PaymentController` e `PaymentRequest`.
- Adicionado endpoint POST `/api/payments` para registrar pagamentos ligados a uma venda.
- Adicionado `findByProductIdAndBranchId` em `StockBranchRepository`.
- Atualizado `StockController` para usar lookup por filial/produto e associar o movimento ao utilizador autenticado.
- Criado o wrapper PowerShell `scripts/build-and-run.ps1` para compilar e executar o backend.
- O wrapper foi testado e valida corretamente a presença de `mvn` e `java`.
