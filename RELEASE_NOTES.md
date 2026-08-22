# SGV Desktop 1.0.6 — Notas de Lançamento

**Data:** 22 de Agosto de 2026  
**Tag:** `v1.0.6`

## Pacote de download

- **ZIP oficial:** [SGV-Desktop 1.0.6.zip](https://github.com/Junior-Web37/SGV/archive/refs/tags/v1.0.6.zip)
- **Página da release:** https://github.com/Junior-Web37/SGV/releases/tag/v1.0.6

## O que mudou nesta versão

A v1.0.5 já arranca, autentica e vende. O log do utilizador mostrou o dashboard a
abrir em ~4 s e cada pesquisa a bloquear a interface: a thread do JavaFX fazia
SQL a cada tecla e carregava os 9 relatórios no login.

Nesta versão:

- Pesquisa de produto (venda, compra, transferência) com debounce — deixa de
  disparar uma query por letra
- Stock da filial em cache (1 query), em vez de 1 SELECT por artigo
- Lista de vendas filtra na base, sem `JOIN FETCH` de itens, máximo 200 linhas
- Relatórios já não carregam no login — só quando abre o módulo
- Coluna Stock dos artigos sem N+1
- Popularidade de produtos por agregação SQL, não 90 dias de itens em memória

A v1.0.5 (ComboBox JavaFX 21) continua incluída: o `fromIndex/toIndex` no log
é ignorado e o ecrã não fica vermelho.

## Como actualizar

1. Apague a pasta `SGV-1.0.5` (a base de dados no XAMPP fica).
2. Extraia este ZIP.
3. MySQL do XAMPP em **Start**, porta `3306`.
4. Duplo clique em `SGV-Launcher.bat`.

**Login:** `admin` / `admin`

## Requisitos

| Componente | Versão |
| :--- | :--- |
| Windows | 10 / 11 (64-bit) |
| Java JDK | 21 LTS ou 25 (Adoptium Temurin) |
| Base de dados | MariaDB / MySQL 8 via XAMPP, porta 3306 |
