# 💧 BILLY WATER — Identidade Visual (Design System)

**Princípio de design combinado:**
> **Mesmas dimensões / geometria do SGV** (tamanho igual). **Identidade refrescada** (cores, ícones e
> alguns elementos podem mudar), **mantendo a mesma linhagem** do SGV (cartões, gradiente de topo,
> tabelas, badges, diálogos, KPI). **Tema escolhido: AZUL CLARO** (luminoso, tom "céu/água").

| Acordo | Concretização |
|---|---|
| ✅ **Mesmo tamanho** | Todas as dimensões do SGV são preservadas (ver §1) |
| ✅ **Mesma linhagem** | Mesma estrutura de componentes, cartões, tabelas, diálogos, KPIs |
| 🔄 **Pode trocar (quero melhorar)** | Paleta (**azul claro**), ícones (Ikonli vectoriais), alguns elementos visuais |
| ❌ **NÃO fugir** | Não muda a geometria, os raios, a hierarquia, o comportamento das interacções |

---

## 1. Dimensões e geometria — IGUAIS ao SGV (não alterar)

| Elemento | Valor SGV (mantido) |
|---|---|
| Barra de navegação superior | 58px |
| Sub-bar de título (page title) | 40px |
| Cell de tabela | 42px |
| KPI icon (fundo) | 24px · raio 12 |
| Numpad touch | 48×48dp |
| Raio de cartão / painel | 10–12 |
| Raio de botão | 6 |
| Cabeçalho de tabela (header) | 42px |
| Espaçamento de cartão | 20 |
| Padding global do workspace | 24 |
| Tooltip | fundo escura, raio 6 |
| Sombra de cartão | `dropshadow(gaussian, rgba(15,23,42,0.05), 10, 0, 0, 4)` |
| Sombra hover cartão | `rgba(15,23,42,0.08), 12, 0, 0, 6` · `-fx-translate-y:-1` |

> **Regra de ouro:** a *forma e o espaço* são o esqueleto herdado do SGV. A *pele* (cor + ícone + detalhe)
> é a novidade do BILLY WATER.

---

## 2. Paleta — “AZUL CLARO” (luminoso, tom céu/água)

A família continua **azul** — mas **clara e brilhante** (sky), em vez do cobalto escuro
`#1E3A8A/#1D4ED8/#2563EB`.

### 2.1 Cores base

| Papel | SGV | BILLY WATER (novo) | Uso |
|---|---|---|---|
| Superfície | `#F8FAFC` | `#EFF8FF` (azul-claro 50) | fundo geral / painéis |
| Cartões | `#FFFFFF` | `#FFFFFF` | cards, tabelas, diálogos |
| Borda | `#E2E8F0` / `#CBD5E1` | `#DBEAFE` / `#7DD3FC` | contornos (tom azul) |
| **Primário** | `#2563EB` | **`#0284C7`** (sky-600) | botão primário, foco |
| **Primário hover** | `#1D4ED8` | **`#0369A1`** (sky-700) | hover |
| **Primário profundo** | `#1E40AF` | **`#075985`** (sky-800) | borda primária, gradiente |
| Texto forte | `#0F172A` | `#0F172A` (mantém) | títulos, números |
| Texto médio | `#475569`/`#334155` | `#475569`/`#334155` (mantém) | rótulos |
| Texto suave | `#64748B`/`#94A3B8` | `#64748B`/`#94A3B8` (mantém) | neutral |

### 2.2 Gradiente da barra de topo / cabeçalho de página

| Contexto | SGV | BILLY WATER (novo) |
|---|---|---|
| Top bar | `#1E3A8A → #1D4ED8 → #1E40AF` | **`#075985 → #0284C7 → #38BDF8`** (azul claro) |
| Cabeçalho de janela/PDV | `#1E3A8A → #1D4ED8` | **`#075985 → #0284C7`** |
| Login root | `#1E3A8A → #1D4ED8 → #0F172A` | **`#0C4A6E → #0284C7 → #0B1220`** |

### 2.3 Acentos e estados

| Elemento | SGV | BILLY WATER (novo) | Uso |
|---|---|---|---|
| Sucesso | `#10B981` (verde) | `#10B981` (verde, mantém) | confirmações, pago |
| Sucesso hover | `#059669` | `#059669` (mantém) | — |
| Avançado | `#F59E0B` | `#F59E0B` (mantém) | alertas, pendência |
| Perigo | `#EF4444` | `#EF4444` (mantém) | anular, eliminar |
| Destaque INFO | `#7C3AED` | `#0EA5E9` (sky-500) | KPI secundário, unidade |
| Azul claro | — | `#38BDF8` / `#BAE6FD` / `#7DD3FC` | destaques “água/céu” |

### 2.4 Estados de célula (tabelas) — linhagem mantida, matizes azul-claro

| Estado | Fundo / Texto (novo) | SGV (referência) |
|---|---|---|
| PAGO / COMPLETED / CONCLUÍDO | `#D1FAE5` / `#065F46` | `#D1FAE5`/`#065F46` |
| EMITIDA / INFO | `#DBEAFE` / `#1E40AF` | `#DBEAFE`/`#1E40AF` |
| PENDENTE / ESTIMADA / EM CURSO | `#FEF3C7` / `#92400E` | `#FEF3C7`/`#92400E` |
| ANULADA / CANCELLED / INACTIVO | `#FEE2E2` / `#991B1B` | `#FEE2E2`/`#991B1B` |
| RECTIFICACAO / ENCOMENDA | `#EDE9FE` / `#5B21B6` | `#EDE9FE`/`#5B21B6` |

---

## 3. Ícones — Ikonli vectoriais (substituem emojis na UI operacional)

> Regra do BILLY (mantida): **sem emojis na UI**; usar Ikonli Feather/Material (vectorial). O SGV usava
> emojis; o BILLY troca-os por glyphs coerentes com a água, **mantendo o mesmo tamanho/posição** (24px KPI,
> 20px em diálogos, 22px em DetailDialog).

### 3.1 Mapa de ícones por módulo (Ikonli)

| Módulo / Acção | Emoji SGV | Ícone BILLY WATER (Ikonli) |
|---|---|---|
| Marca / Logo | `⚡` | `md-droplet` |
| Dashboard | `⌂` | `md-dashboard` |
| Clientes | `👥` | `md-account-circle` |
| Contadores | `📦` | `md-straighten` |
| Zonas / Rotas | `🗺️` | `md-routes` |
| Leituras | `📊` | `md-list-alt` |
| Facturação | `🧾` | `md-receipt` |
| Tarifário | `📑` | `md-tune` / `md-format-list-bulleted` |
| Caixa / Cobrança | `💼` / `💰` | `md-cash` / `md-account-balance-wallet` |
| REC / Recibos | `📄` | `md-receipt-long` |
| Dívidas & Cortes | `⚠` | `md-alert` / `md-report-problem` |
| Loja / POS | `🛒` | `md-point-of-sale` / `md-shopping-cart` |
| Stock / Produtos | `📦` | `md-inventory` |
| Armazéns | `🏢` | `md-warehouse` |
| Transferências | `🔁` | `md-swap-horiz` |
| Serviços / Canalização | `🔧` | `md-build` |
| Relatórios | `📊` | `md-insert-chart` |
| Parâmetros | `⚙️` | `md-settings` |
| Utilizadores | `👤` | `md-account-box` |
| Backups | `💾` | `md-backup` / `md-save` |
| Treinamento | `🎓` | `md-school` |
| Licenciamento | `🔑` | `md-key` |
| Segurança | `🔒` | `md-lock` |
| Logs / Auditoria | `🛡️` | `md-shield` |
| Sair | `⏻` | `md-exit-to-app` |
| Pesquisar | `🔍` | `md-search` |
| Adicionar | `+` | `md-add` |
| Actualizar | `↻` | `md-refresh` |
| Imprimir | `🖨️` | `md-print` |
| Ver / Detalhes | `👁️` | `md-visibility` |
| Exportar Excel | `⬇` | `md-file-download` |
| Limitador | `0–9` | `md-format-list-numbered` |

> **Como integrar:** carregar os GLIFS Ikonli via identificadores de fonte (ex.: `icon("md-drop")`) e trocar
> os `Label` com emoji por `Label` com `GlyphIcon` de Ikonli, **sem alterar tamanho/posição/alinhamento**.

---

## 4. Logo / Marca

- **Símbolo:** gota de água estilizada (`md-droplet`) + nome **BILLY WATER**.
- **Badge de versão:** `AURA` (em vez do `PRO` do SGV), pequeno, `9px`, peso 800, cor `#E0F2FE`.
- **Subtítulo:** `SISTEMA DE FACTURAÇÃO DE ÁGUA & SANEAMENTO`.

**Barra de topo (mesma estrutura de 58px):**
```
[ 💧 BILLY WATER  AURA ] | [ ⌂ Painel  Água ▾  Loja ▾  Rede ▾  Relatórios ▾  Configurações ▾  🛡️ Logs ] | [○ Caixa] [🔔 Alertas] [A Utilizador / ADMIN] [⏻ Sair]
```

---

## 5. Botões — mesma classes, matizes de azul claro

| Classe | SGV | BILLY WATER (novo) |
|---|---|---|
| `.primary-button` | fundo `#2563EB` | fundo `#0284C7`; hover `#0369A1`; borda `#075985`; sombra `rgba(2,132,199,0.28)` |
| `.primary-button-success` | `#10B981` | `#10B981`; hover `#059669` (mantém) |
| `.secondary-button` | branco, borda `#CBD5E1` | branco, borda `#7DD3FC` |
| `.danger-button` | `#FEE2E2`/`#DC2626` | `#FEE2E2`/`#DC2626` (mantém) |
| `.nav-btn` | `#E2E8F0`, hover branco | texto `#E0F2FE`; hover `rgba(255,255,255,0.18)`; **active** → fundo branco, texto `#0284C7` |

---

## 6. Cartões / KPI / Tabelas / Diálogos (linhagem preservada)

- **Cartões:** mesmos raios/sombras; borda hover `#7DD3FC` (azul claro) em vez de `#93C5FD`.
- **KPI:** `kpi-icon` com fundos *azulo-claros*: `kpi-icon-blue` → `#DBEAFE`; `kpi-icon-green` →
  `#D1FAE5`; `kpi-icon-purple` → `#E0F2FE`; `kpi-icon-orange` → `#FEF3C7`. Emoji KPI trocado por glyph Ikonli.
- **Tabelas:** header `#F8FAFC`→`#EFF6FF`, label `#1E40AF`→`#075985`; zebra inalterada; cell 42px.
- **Diálogos (`SgvDialog`/`DetailDialog`):** accent bar passa a azul `#0284C7` (info/sucesso),
  `#EF4444` (erro/perigo), `#F59E0B` (aviso); header gradiente `#075985→#0284C7`; ícone Ikonli.
- **StatusBadge:** sucesso `#D1FAE5`/`#065F46`; info `#DBEAFE`/`#1E40AF`; aviso `#FEF3C7`/`#92400E`;
  perigo `#FEE2E2`/`#991B1B`.

---

## 7. Login

- Fundo: gradiente `#0C4A6E → #0284C7 → #0B1220`.
- Card raio 16, `dropshadow(rgba(0,0,0,0.6), 25)`.
- Marca `💧 BILLY WATER` + subtítulo `SISTEMA DE FACTURAÇÃO DE ÁGUA & SANEAMENTO`.
- Campos `👤 Login` / `🔒 Palavra-passe` (fundo `#082F49`, texto `#FFF`, prompt `#64748B`) — glyphs Ikonli.
- Botão `ENTRAR NO SISTEMA` (gradiente `#0284C7 → #0369A1`).
- Rodapé: `🔒 Certificado Fiscal AT` · `🇲🇿 Moçambique (CIVA)` · `© 2026 BILLY WATER Enterprise`.

---

## 8. Exemplo de comparação visual (mesma estrutura, nova pele azul-clara)

| Aspecto | SGV | BILLY WATER |
|---|---|---|
| Top bar | cobalto `#1E3A8A/#1D4ED8/#1E40AF` | azul claro `#075985/#0284C7/#38BDF8` |
| Botão primário | `#2563EB` | `#0284C7` |
| Foco de input | `#1D4ED8` + sombra azul | `#0369A1` + sombra azul |
| KPI icon blue | `#DBEAFE` | `#DBEAFE` |
| KPI icon green | `#D1FAE5` | `#D1FAE5` |
| KPI icon purple | `#EDE9FE` | `#E0F2FE` |
| Detalhe de secção (accent) | `#1D4ED8` | `#0284C7` |
| Ícones | emojis | Ikonli (vectorial, sem emoji) |
| Badge do logo | `PRO` | `AURA` |
| **Dimensões, raios, espaçamentos, cell, numpad** | — | **idênticos** |

---

## 9. Diretriz para implementação (não fugir do SGV)

1. **Copiar a geometria do SGV** (FXML/panes, raios, paddings, alturas) e **só trocar os tokens de cor**
   em `styles.css`/`dashboard.css`:
   | Token SGV | Token BILLY WATER |
   |---|---|
   | `#1E3A8A` | `#075985` |
   | `#1D4ED8` | `#0284C7` |
   | `#1E40AF` | `#075985` |
   | `#2563EB` | `#0284C7` |
   | `#93C5FD` | `#7DD3FC` |
   | `#DBEAFE` | `#DBEAFE` |
   | `#D1FAE5` | `#D1FAE5` |
   | `#EDE9FE` | `#E0F2FE` |
2. **Não alterar** larguras, alturas, paddings de componentes, nem a ordem dos campos.
3. **Trocar** apenas `Label` com emoji por `GlyphIcon` Ikonli (tamanho idêntico a 20–24–22px).
4. **Padrão:** se algo precisar de mudar, privilegiar a **cor e o ícone**, nunca a geometria.

---

## 10. Conformidade de Design (checklist)

- [ ] Dimensões/geometria 100% iguais ao SGV (58/40/42/24/48dp, raios, paddings).
- [ ] Mesma estrutura de componentes (cartões, tabelas, KPIs, diálogos, nav) — só a pele muda.
- [ ] Paleta **azul claro** coerente: `#075985/#0284C7/#38BDF8/#7DD3FC/#DBEAFE`.
- [ ] Ícones Ikonli vectoriais (sem emoji na UI operacional), tamanhos SGV.
- [ ] Estados de célula e badges mantêm a linhagem do SGV (ver §2.4).
- [ ] Mesma linhagem visual: gradiente de topo, cartões brancos, sombras suaves, foco azul.
- [ ] Login, PDV, Dashboard, Relatórios, Configurações — mesmo esqueleto, nova pele azul-clara.

---
*Identidade visual do BILLY WATER: mesmo tamanho/geometria do SGV · cores e ícones refrescados (tema
azul claro/céu) mantendo a mesma linhagem de design do SGV.*
