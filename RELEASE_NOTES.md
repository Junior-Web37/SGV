# SGV Desktop 1.0.8 — Notas de Lançamento

**Data:** 22 de Agosto de 2026  
**Tag:** `v1.0.8`

## Pacote de download

- **ZIP oficial:** [SGV-Desktop 1.0.8.zip](https://github.com/Junior-Web37/SGV/archive/refs/tags/v1.0.8.zip)
- **Página da release:** https://github.com/Junior-Web37/SGV/releases/tag/v1.0.8

## O que mudou nesta versão

Vender o serviço **SRV-003** (Consultoria) rebentava:

`Stock da filial não encontrado para o produto: SRV-003`

O serviço está marcado `is_service = 1` e **não tem** ficha de stock (é correcto).
A validação já ignorava serviços; o abate de stock **não**. A venda falhava
depois de tentar creditar o caixa.

Nesta versão:

- Serviços não abatem stock
- O movimento de caixa só corre depois do stock
- Inclui a correcção da guia de transferência (v1.0.7)

## Como actualizar

1. Feche o SGV.
2. Apague a pasta `SGV-1.0.6` / `SGV-1.0.7` (o MySQL/XAMPP fica).
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
