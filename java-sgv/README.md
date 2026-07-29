# SGV Java Desktop App

Este projeto é a aplicação desktop SGV, construída com JavaFX e Spring Boot.

## Características

- Java 21
- JavaFX para interface desktop
- Spring Boot para serviços de negócio e persistência
- Spring Security com roles: `ADMIN`, `GESTOR`, `CAIXA`, `CLIENTE`
- JPA/Hibernate para persistência com MariaDB
- Relatórios, compras, vendas, reconciliação e backups integrados no desktop

## Como executar

1. Instale o JDK 21 ou superior.
2. Configure `JAVA_HOME` e certifique-se de que `mvn` está no PATH.
3. No diretório `java-sgv`, execute:

```bash
mvn -DskipTests package
mvn -DskipTests javafx:run
```

4. Ou use o JAR gerado:

```bash
java -jar target\java-sgv-0.1.0.jar
```

## Executar no Windows

- `run.bat` — executa o app jar empacotado.
- `run_dev.bat` — compila e executa o app com `javafx:run`.
- `run.ps1` — helper PowerShell para rodar o app.

## Observações

- O frontend web original foi removido do repositório.
- O aplicativo desktop inicia em modo não-web e usa o Spring Boot localmente.
- Ajuste `application-prod.properties` se precisar conectar a uma instância MariaDB diferente.
