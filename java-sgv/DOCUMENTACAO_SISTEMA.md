# SGV Desktop — Documentação do sistema

## 1. O que é este sistema

O SGV é um sistema de gestão comercial desktop, voltado para vendas, estoque, clientes, compras, caixa e relatórios. Ele foi construído como uma aplicação JavaFX com backend Spring Boot, operando localmente e persistindo os dados em uma base MariaDB/MySQL.

A proposta principal é centralizar a operação diária de uma empresa em um ambiente offline, com fluxo de vendas, controle de stock, gestão de despesas, abertura/fecho de caixa e emissão de documentos fiscais e relatórios.

## 2. Como ele é

### Visão geral
- Aplicação desktop, não web.
- Interface em JavaFX.
- Backend gerido por Spring Boot.
- Persistência via JPA/Hibernate.
- Schema versionado com Flyway.
- Base de dados MariaDB/MySQL local.
- Autenticação local com utilizadores e permissões por papel.

### Arquitetura geral
A aplicação organiza-se em camadas:
- Frontend: controladores JavaFX e telas FXML.
- Camada de serviço: regras de negócio e orquestração.
- Camada de acesso a dados: repositories JPA.
- Persistência: MariaDB com Flyway para migrações.
- Configuração: propriedades de ambiente e inicialização automática.

## 3. O que tem no sistema

### Módulos principais
- Autenticação e autorização
- Gestão de utilizadores e papéis
- Gestão de filiais/branch
- Gestão de produtos
- Gestão de clientes
- Gestão de fornecedores
- Gestão de vendas
- Gestão de compras
- Gestão de stock e movimentações
- Gestão de caixa e sessões de caixa
- Gestão de despesas
- Relatórios e dashboards
- Exportação/integração fiscal
- Backup e auditoria

### Entidades principais
- User
- Role
- Branch
- Product
- Category
- Customer
- Supplier
- Sale
- SaleItem
- Payment
- Purchase
- PurchaseItem
- Expense
- CashSession
- CashMovement
- StockBranch
- StockMovement
- Warehouse / WarehouseTransfer
- AppConfig
- AuditLog

## 4. Como funciona

### 4.1 Arranque da aplicação
1. O ponto de entrada é a classe MainApp.
2. A aplicação mostra um splash screen imediatamente.
3. Em segundo plano, o Spring Boot é iniciado.
4. O contexto Spring fica pronto e a tela de login é carregada.
5. O utilizador entra no sistema e é levado para o dashboard.

### 4.2 Autenticação e permissões
- O login é feito por username e password.
- A senha é validada usando BCrypt via Spring Security.
- Cada utilizador tem um ou mais papéis.
- Os papéis têm permissões do tipo pagina:ação, por exemplo VENDAS:CREATE.
- O acesso a funcionalidades é controlado por essas permissões.

### 4.3 Fluxo de vendas
O fluxo de venda é um dos pontos centrais do sistema.

1. O utilizador abre a tela de vendas.
2. Seleciona ou cria um cliente.
3. Adiciona produtos à venda.
4. O sistema calcula subtotal, impostos, desconto e total.
5. O sistema valida se o stock disponível é suficiente.
6. A venda é guardada na base de dados.
7. O stock é atualizado automaticamente.
8. O sistema gera o documento fiscal/recibo e/ou impressão térmica.
9. Se a venda for a crédito, o saldo do cliente pode ser atualizado.

Em termos de dados, o fluxo é:
- UI -> Controller -> SaleService -> Repository -> MariaDB
- Depois: StockBranchService -> atualização de stock
- Depois: FiscalService -> geração de hashes/documentos
- Depois: impressão/relatório

### 4.4 Fluxo de stock
- Cada produto tem stock por filial.
- Quando uma venda é processada, o stock é decrementado.
- Quando uma compra é registada, o stock é incrementado.
- Movimentações de stock são registadas em StockMovement.
- O sistema permite também ajustes manuais de stock e transferências entre armazéns/filiais.

### 4.5 Fluxo de caixa
- O utilizador abre uma sessão de caixa.
- O sistema regista movimentos de entrada e saída.
- Ao fechar a sessão, o sistema calcula o saldo final.
- O estado da sessão é usado para conciliar operações diárias.

### 4.6 Fluxo de despesas
- As despesas são introduzidas com descrição, categoria, montante e estado.
- O sistema permite associar a utilizador e filial.
- Essas despesas aparecem em relatórios e dashboards.

### 4.7 Fluxo de relatórios e dashboard
- O dashboard agrega dados de vendas, despesas, stock baixo, clientes e produtos.
- Os dados são trazidos por services dedicados e podem ser cacheados.
- Os relatórios usam estes agregados para mostrar o desempenho do negócio.

## 5. Fluxo de dados existente

### 5.1 Fluxo geral de um registo
O fluxo de dados mais comum segue esta lógica:
1. O utilizador interage com uma tela JavaFX.
2. O controlador lê os dados introduzidos.
3. O controlador invoca um service.
4. O service valida regras de negócio.
5. O service usa repositories para persistir ou consultar dados.
6. O resultado é devolvido à interface e mostrado ao utilizador.

### 5.2 Exemplo real: criação de uma venda
- O utilizador preenche os campos na UI.
- O controller cria/ajusta um objeto Sale.
- O SaleService normaliza os dados.
- O sistema calcula os totais.
- O sistema valida stock.
- O sistema grava a venda e os itens.
- O sistema atualiza o stock e saldo do cliente.
- O sistema gera documento/recibo.

### 5.3 Exemplo real: login
- O login controller recebe username e password.
- O DesktopAuthService busca o utilizador na base de dados.
- A password é comparada com o hash guardado.
- Se estiver correta, o utilizador entra no dashboard.

## 6. Como o sistema guarda dados

### Base de dados
- O projeto usa MariaDB/MySQL como motor principal.
- As credenciais estão definidas no ficheiro de propriedades da aplicação.
- O Flyway aplica migrações SQL automaticamente na inicialização.

### Persistência
- Os objetos Java são mapeados para tabelas com JPA/Hibernate.
- O esquema é tratado como parte do ciclo de deploy do sistema.
- O projeto usa validação de schema com Flyway e Hibernate em modo controlado.

## 7. Pontos fortes do sistema
- Funciona como sistema comercial completo para operação local.
- Integra vendas, stock, caixa, compras e relatórios num mesmo ambiente.
- Usa organização por camadas, o que facilita manutenção.
- Tem suporte a permissões e roles.
- Tem integração com mecanismos fiscais e impressão.
- Tem logs, auditoria e inicialização de dados padrão.

## 8. Limitações e pontos a ter em conta
- É uma aplicação desktop e depende de um ambiente local com MariaDB/MySQL.
- O funcionamento está fortemente ligado à presença do banco e aos ficheiros de configuração.
- A experiência é mais orientada para operação local do que para uso multiutilizador distribuído.
- O sistema tem lógica fiscal e de stock bastante relevante, pelo que deve ser tratado com cuidado em alterações.

## 9. Resumo executivo
Este SGV é um sistema de gestão comercial desktop para pequenas e médias operações, com foco em vendas, stock, clientes, caixa, compras e relatórios. Ele funciona com uma arquitetura JavaFX + Spring Boot + MariaDB, onde a interface recolhe dados do utilizador, os services aplicam as regras de negócio e os repositories persistem tudo na base de dados.

Em termos práticos, ele é um sistema operacional de negócio que acompanha o ciclo completo de venda, desde o login até ao processamento final, incluindo stock, caixa e documentação.
