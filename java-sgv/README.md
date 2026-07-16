# SGV Java Scaffold

Este projecto é um scaffold inicial para o Sistema de Gestão de Vendas (SGV) com foco em Moçambique.

## Características

- Spring Boot 3 + Java 17
- Spring Security para desktop authentication
- Usuários com roles: `ADMIN`, `GESTOR`, `CAIXA`, `CLIENTE`
- JPA/Hibernate para persistência
- Conexão padrão com MariaDB/XAMPP para produção local
- Estrutura de camadas: `desktop`, `service`, `repository`, `entity`, `security`, `dto`

## Como executar

1. Instalar Maven e JDK 17.
2. Iniciar o MariaDB do XAMPP.
3. No directório `java-sgv`, executar:

```bash
mvn spring-boot:run
```

4. O sistema Java inicia em modo desktop sem servidor web.

## Observações

- A `application.properties` usa MariaDB no XAMPP como padrão.
- O desktop Java usa serviços locais embutidos e não realiza chamadas HTTP para `localhost`.
- Para produção, ajuste `spring.datasource.url`, `spring.datasource.username` e `spring.datasource.password`.
- A inicialização cria os roles básicos.
- Utilitários e ficheiros H2 foram removidos do código-fonte; quaisquer ficheiros antigos H2 foram movidos para `java-sgv/data/archived_h2`.
