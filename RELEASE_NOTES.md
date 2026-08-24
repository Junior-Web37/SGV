# SGV Desktop 1.0.10 — Notas de Lançamento

**Data:** 22 de Agosto de 2026  
**Tag:** `v1.0.10`

## Pacote de download

- **ZIP oficial:** [SGV-Desktop 1.0.10.zip](https://github.com/Junior-Web37/SGV/archive/refs/tags/v1.0.10.zip)
- **Página da release:** https://github.com/Junior-Web37/SGV/releases/tag/v1.0.10

## O que mudou nesta versão

A pesquisa em tempo real nos formulários só actualizava quando o popup estava fechado. Ao apagar o texto e escrever outro nome, a lista antiga ficava presa.

- Venda e compra voltam a filtrar a cada letra — inclusive com o dropdown aberto
- Apagar o texto restaura a lista completa; um nome novo substitui o anterior
- Serviços (ex.: SRV-003) deixam de parecer «com stock»: texto roxo + «· Serviço»
- Artigos físicos sem stock ficam pálidos + «· Sem stock»
- O sino de notificações, o KPI e o mapa de reposição usam a mesma regra: esgotado, abaixo do mínimo ou sem ficha — serviços não entram
- No catálogo, um serviço já não aparece como stock 0,0 (vermelho)

A venda de serviços continua sem exigir ficha de stock.

## Como actualizar

1. Feche o SGV.
2. Apague a pasta `SGV-1.0.8` / `SGV-1.0.9` (o MySQL/XAMPP fica).
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
