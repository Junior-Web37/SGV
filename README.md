# 🇲🇿 SGV — Sistema de Gestão de Vendas & Facturação (Moçambique)

Sistema completo de gestão comercial, faturação fiscal certificada, inventário central e tesouraria desenvolvido em **Java 21 (LTS) / JavaFX 21** e **Spring Boot 3.1.4**, com persistência sobre **MariaDB / MySQL** (XAMPP).

O sistema opera **100% offline** e está em total conformidade com a legislação fiscal de Moçambique (**Autoridade Tributária - AT / CIVA - Decreto 7/2024 / SAF-T MZ**).

---

## 📥 Download — SGV Desktop 1.0.9

**[⬇ Descarregar SGV Desktop 1.0.9 (ZIP)](https://github.com/Junior-Web37/SGV/archive/refs/tags/v1.0.9.zip)**

Também disponível na [página de Releases](https://github.com/Junior-Web37/SGV/releases/tag/v1.0.9).

### Instalação em 3 passos (Windows)
1. Extraia o ZIP para uma pasta (ex.: `C:\SGV`).
2. Abra o **XAMPP Control Panel** e clique em **Start** no MySQL (porta `3306`).
3. Dê duplo clique em **`INSTALAR-SGV.bat`** — o atalho é criado no Ambiente de Trabalho.

**Login padrão:** `admin` / `admin`

---

## 📋 Entregáveis de Engenharia & Documentação

* [CRIMES_LEGADO.md](CRIMES_LEGADO.md) — Levantamento forense das 10 piores violações do código legado.
* [PLANO_MIGRACAO.md](PLANO_MIGRACAO.md) — Matriz de impacto e ordem de execução das refatorações.
* [java-sgv/DIAGNOSTICO.md](java-sgv/DIAGNOSTICO.md) — Diagnóstico exaustivo de arquitetura e regras de negócio.
* [java-sgv/DB_SCHEMA.md](java-sgv/DB_SCHEMA.md) — Especificação completa do schema e 25 migrações Flyway.

---

## 🚀 Requisitos de Execução

* **Java JDK:** Java 21 ou Java 25 (LTS) — Recomendado: [Eclipse Adoptium Temurin](https://adoptium.net/).
* **Base de Dados:** MariaDB ou MySQL 8.x (ex: via **XAMPP** na porta `3306`, utilizador `root` sem palavra-passe).
* **Esquema:** `sgv` (o **Flyway cria e atualiza as 25 migrações automaticamente no arranque**).

---

## 🔑 Credenciais Padrão de Acesso

* **Utilizador:** `admin`
* **Palavra-passe:** `admin` *(recomendado alterar no primeiro login em Configurações > Alterar Palavra-passe)*

---

## ⚙️ Variáveis de Ambiente (Configuração Opcional de Produção)

| Variável | Valor Padrão | Descrição |
| :--- | :--- | :--- |
| `DB_HOST` | `localhost` | Endereço do servidor MySQL/MariaDB |
| `DB_NAME` | `sgv` | Nome da base de dados |
| `DB_USER` | `root` | Utilizador da base de dados |
| `DB_PASSWORD` | *(vazio)* | Senha da base de dados |
| `SPRING_PROFILES_ACTIVE` | `mysql` | Perfil de execução (`mysql`, `prod`, `docker`) |

---

## 💻 Como Compilar e Executar

### 1. Compilação Completa via Maven
```bash
cd java-sgv
mvn clean install
```

### 2. Execução Direta no Windows (Recomendado)
Dê duplo clique no ficheiro:
```bat
java-sgv\SGV-Launcher.bat
```

### 3. Execução em Modo Desenvolvimento
```bash
cd java-sgv
mvn -DskipTests javafx:run
```

### 4. Execução via JAR Empacotado
```bash
cd java-sgv
mvn -DskipTests clean package
java -jar target/java-sgv-0.1.0.jar --spring.profiles.active=mysql
```

---

## 🏢 Módulos do Sistema (Padrão Comercial Moçambicano)

1. **⚡ Vendas & Facturação:**
   * Ponto de Venda (PDV) com ecrã táctil (Numpad 48x48dp), leitura de código de barras, troco em tempo real, botões rápidos de numerário e suspensão de venda (`F6`/`F7`).
   * Emissão de Facturas (FT), Talões de Venda (TV/VD), Recibos (RC), Cotações/Proformas (FP) e Notas de Crédito (NC).
   * Pagamentos móveis moçambicanos (**M-Pesa**, **e-Mola**, **mKesh**), Numerário, Cartão POS e Transferência.
   * Gestão de Clientes com validação de NUIT (Módulo 11) e contas correntes.

2. **📦 Stock & Artigos:**
   * Catálogo de artigos com margens automáticas, múltiplos códigos de barras e controlo de inventário em loja.
   * **Ficha Técnica & Fabrico (BOM):** Ordens de produção para panificação com abate automático de ingredientes por receita.

3. **💰 Caixa & Tesouraria:**
   * Abertura com Fundo de Maneio, Sangrias, Reforços e Fecho cego com emissão térmica de **Fita Z**.
   * Recebimentos de faturas a crédito com amortização de dívida sem risco de liquidações duplicadas.

4. **🚚 Armazém & Compras:**
   * Gestão de armazéns centrais e **Guias de Transferência** com controlo em 2 etapas (Envio ➔ Confirmação física de receção).
   * Facturas de compra a fornecedores com Custo Médio Ponderado (CMP).

5. **📈 Mapas & Relatórios:**
   * Mapa geral de vendas, **Apuramento de IVA (16%)** com isenções CIVA (M01..M18) e **Exportação oficial SAF-T Moçambique** (XML).
   * Extrato de movimentações de stock (Kardex).

6. **⚙️ Configurações & Sistema:**
   * Dados fiscais da empresa e NUIT da sede.
   * Utilizadores e Perfis com controlo de acesso baseado em papéis (**RBAC**).
   * Cópias de Segurança (Backup) automático e restauro.
