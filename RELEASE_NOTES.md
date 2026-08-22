# SGV Desktop 1.0.9 — Notas de Lançamento

**Data:** 22 de Agosto de 2026  
**Tag:** `v1.0.9`

## Pacote de download

- **ZIP oficial:** [SGV-Desktop 1.0.9.zip](https://github.com/Junior-Web37/SGV/archive/refs/tags/v1.0.9.zip)
- **Página da release:** https://github.com/Junior-Web37/SGV/releases/tag/v1.0.9

## O que mudou nesta versão

No arranque o JavaFX 21 escrevia avisos CSS (não eram “barulho”):

1. `-fx-text-fill: linear-gradient(...)` no título do splash — só aceita cor
2. `-fx-letter-spacing` no splash — propriedade que o JavaFX não tem
3. `linear-gradient(to bottom right, ...)` no fundo do splash — dois sentidos inválidos
4. `-fx-accent: linear-gradient(...)` na barra de progresso — só aceita cor
5. O mesmo `to bottom right` no ecrã de login
6. `-fx-text-fill: linear-gradient(...)` + `-fx-letter-spacing` no título do login

Também corrigido: o mapa de pagamentos a fornecedores rebentava ao converter o saldo (`Double` vs `BigDecimal`).

## Como actualizar

1. Feche o SGV.
2. Apague a pasta `SGV-1.0.6` / `SGV-1.0.7` / `SGV-1.0.8` (o MySQL/XAMPP fica).
3. Extraia este ZIP.
4. XAMPP → MySQL **Start**.
5. Duplo clique em `SGV-Launcher.bat`.

**Login:** `admin` / `admin`

## Requisitos

| Componente | Versão |
| :--- | :--- |
| Windows | 10 / 11 (64-bit) |
| Java JDK | 21 LTS ou 25 (Adoptium Temurin) |
| Base de dados | MariaDB / MySQL 8 via XAMPP, porta 3306 |
