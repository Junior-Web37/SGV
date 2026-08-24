# 🇲🇿 SGV Desktop — Sistema de Gestão de Vendas & Facturação

Aplicação desktop corporativa de alta disponibilidade, concebida para operar **100% offline**, construída em **Java 21 (LTS) / JavaFX 21** e **Spring Boot 3.1.4**, com persistência sobre **MariaDB / MySQL** (XAMPP).

O sistema está em total conformidade com a legislação fiscal de Moçambique (**Autoridade Tributária - AT / CIVA - Decreto 7/2024 / SAF-T MZ**).

---

## 🚀 Requisitos do Sistema

* **Java JDK:** Java 21 ou Java 25 (LTS) — Recomendado: [Eclipse Adoptium Temurin](https://adoptium.net/).
* **Base de Dados:** MariaDB ou MySQL 8.x (ex: via **XAMPP** a correr na porta `3306`, utilizador `root` sem palavra-passe).
* **Esquema:** `sgv` (o **Flyway cria e atualiza as 25 migrações automaticamente no primeiro arranque**).

---

## 🔑 Credenciais Padrão de Acesso

* **Utilizador:** `admin`
* **Palavra-passe:** `admin`

---

## 💻 Como Executar

### Opção 1 — Launcher Windows (Recomendado para Usuários Finais)
Dê duplo clique no ficheiro:
```bat
SGV-Launcher.bat
```

### Opção 2 — Modo Desenvolvimento (Maven)
```bash
mvn -DskipTests javafx:run
```

### Opção 3 — Compilação e Empacotamento JAR
```bash
mvn -DskipTests clean package
SGV-Desktop.bat
```

---

## 🏢 Arquitetura e Módulos do Sistema

1. **⚡ Vendas & Facturação:**
   * Ponto de Venda (PDV) com leitura de código de barras, cálculo de troco em tempo real, botões rápidos de numerário e atalhos de teclado (`F2` a `F10`, `ESC`).
   * Emissão de Facturas (FT), Talões de Venda (TV/VD), Recibos (RC), Cotações/Proformas (FP) e Notas de Crédito (NC).
   * Suporte a pagamentos móveis moçambicanos (**M-Pesa**, **e-Mola**, **mKesh**), Numerário, Cartão POS e Transferência Bancária.
   * Gestão de Clientes com validação de NUIT (algoritmo de 9 dígitos) e contas correntes.

2. **📦 Stock & Artigos:**
   * Catálogo completo de artigos com preços de venda, margem de lucro automática, preço por grosso e múltiplos códigos de barras.
   * Inventário em loja com controlo de quebras, perdas, ajustes manuais e alertas de stock mínimo.
   * **Ficha Técnica & Fabrico (BOM):** Ordens de produção para padaria e confeitaria com abate automático de matérias-primas por receita.

3. **💰 Caixa & Tesouraria:**
   * Sessão de caixa com Fundo de Maneio, Sangrias de numerário e Reforços de troco.
   * Fecho de turno cego com emissão térmica da **Fita Z** (Relatório Diário com quebras/sobras e assinaturas).
   * Recebimentos de facturas a crédito com amortização da conta corrente do cliente sem duplicação de receitas.
   * Lançamento de despesas operacionais e resumo financeiro.

4. **🚚 Armazém & Compras:**
   * Gestão de armazéns centrais e depósitos.
   * **Guias de Transferência:** Controlo em 2 etapas (Envio em trânsito ➔ Confirmação física de receção na filial).
   * Facturas de compra a fornecedores com recalculo de Custo Médio Ponderado (CMP).

5. **📈 Mapas & Relatórios:**
   * Mapa geral de vendas e desempenho comercial.
   * **Apuramento de IVA (16%)** com motivos de isenção CIVA (M01 a M18) e exportação para Excel/PDF.
   * **Exportação oficial SAF-T Moçambique** (Ficheiro XML certificado).
   * Extrato de movimentações de stock (Kardex).

6. **⚙️ Configurações & Segurança:**
   * Dados fiscais da empresa e NUIT da sede.
   * Gestão de Utilizadores e Perfis com controlo de acesso baseado em papéis (**RBAC**).
   * Sistema de Cópias de Segurança (Backup automático/manual e restauro).
   * Gestão de Licenças e Modo de Treinamento/Simulação.

---

## 🔒 Engenharia e Alta Disponibilidade

* **Sequenciação Atómica:** Lock pessimista na tabela `series_counters` (elimina risco de facturas com numeração duplicada).
* **Bloqueio Otimista (`@Version`):** Impede venda de stock negativo sob acessos concorrentes.
* **Desacoplamento de I/O:** Transações de banco de dados executadas em `< 30ms`, com geração de PDFs fora do lock transacional.
* **Segurança OWASP:** Logs de autenticação sanitizados (sem vazamento de hashes/salts no `sgv.log`).
* **Previous Hash Chaining:** Assinatura digital encadeada de facturas conforme as diretrizes da Autoridade Tributária.
