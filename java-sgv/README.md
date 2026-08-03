# SGV Desktop - Sistema de Gestão de Vendas

Aplicação desktop 100% offline construída com JavaFX e Spring Boot (não-web).

## Requisitos

- Java 25 (JDK) — testado com Eclipse Adoptium `jdk-25.0.3.9-hotspot`
- XAMPP com MySQL/MariaDB a correr na porta 3306 (utilizador `root` sem password)
- Base de dados: `sgv` (o Flyway cria/atualiza o schema automaticamente no arranque)
- Maven (opcional, apenas para desenvolvimento)

## Credenciais padrão

- Utilizador: `admin`
- Senha: `admin`

## Como executar

### Opção 1 — Launcher (recomendado)

Faça duplo clique em `SGV-Launcher.bat` (deteta o JDK e usa `run-sgv.bat`).

### Opção 2 — JAR empacotado

```bat
mvn -DskipTests package
SGV-Desktop.bat
```

### Opção 3 — Modo desenvolvimento

```bat
mvn -DskipTests javafx:run
```

## Estrutura

- `run-sgv.bat` — executa o JAR do `target`.
- `SGV-Desktop.bat` — localiza o Java e executa o JAR empacotado.
- `SGV-Launcher.bat` — ponto de entrada simples que delega no `run-sgv.bat`.
- `src/main/resources/application.properties` — configuração principal (base de dados, logging).
- `src/main/resources/db/migration/` — migrações Flyway.

## Notas

- O frontend web e a camada Spring Security web foram removidos. A autenticação é feita na própria aplicação desktop com BCrypt.
- Roles: `ADMIN`, `GESTOR`, `CAIXA`, `CLIENTE`.
- Problemas comuns:
  - "Access denied for user 'root'": o XAMPP tem password no root — remova-a ou ajuste `spring.datasource` em `application.properties`.
  - A janela abre e fecha rápido: execute `run-sgv.bat` pelo terminal para ver a mensagem de erro.
