# SGV Desktop 1.0.7 — Notas de Lançamento

**Data:** 22 de Agosto de 2026  
**Tag:** `v1.0.7`

## Pacote de download

- **ZIP oficial:** [SGV-Desktop 1.0.7.zip](https://github.com/Junior-Web37/SGV/archive/refs/tags/v1.0.7.zip)
- **Página da release:** https://github.com/Junior-Web37/SGV/releases/tag/v1.0.7

## O que mudou nesta versão

A v1.0.5/1.0.6 **escondiam** o crash do ComboBox na transferência
(`fromIndex 0, toIndex 1, size 0`) em vez de o resolver. O formulário
ficava inutilizável: clicar em armazém / produto não escolhia nada.

Nesta versão a guia de transferência deixa de usar ComboBox editável:

- Lista visível de artigos (campo de pesquisa + ListView)
- Armazém e filial com ComboBox simples, sem `selectFirst()` no arranque
- Número da guia TWA passa a ser `MAX + 1` (a 2.ª guia já não colide)
- Recepção carrega os itens da guia (não fica vazia)

A v1.0.6 (pesquisas mais rápidas) continua incluída.

## Como actualizar

1. Apague a pasta `SGV-1.0.5` ou `SGV-1.0.6` (a base de dados no XAMPP fica).
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
