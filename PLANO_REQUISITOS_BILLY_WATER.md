# 💧 BILLY WATER — Plano de Requisitos Consolidado (Água + Loja + Armazém)

**Resultado da revisão:** unificação do **BILLY WATER v4.8.1** com a **estrutura de Configurações e de
Relatórios do SGV**, corrigindo a inconsistência do módulo **Loja / Armazém / Stock** e mantendo o
**mesmo tamanho/geometria do SGV** com uma **identidade visual própria (tema água-ciano)** — cores e
ícones refrescados, sem fugir da mesma linhagem de design do SGV. Ver `DESIGN_SYSTEM_BILLY_WATER.md`.

**Data:** 31/08/2026 · **Idioma:** pt-MZ · **Moeda:** MT · **Contexto:** Concessionária de Água
(Maputo / Matola / Boane — tarifário AURA / Lei n.º 9/2024).

> **Princípio adoptado:** “não deixar nada de lado” = todo o funcional do BILLY WATER + a estrutura
> completa de Configurações e Relatórios do SGV, traduzida para a água. “Não meter o que não tem a ver”
> = excluir tudo o que é irrelevante para um sistema de água (ex.: fabrico/panificação, BOM de pão,
> integração API inexistente, gestão de padaria, etc.).

---

## 1. Decisões de arquitectura

### 1.1 Base adoptada (BILLY WATER — a que funciona)

| Item | Valor |
|---|---|
| Aplicação | Desktop JavaFX |
| UI | Construída programaticamente em Java (padrão SGV); **FXML opcional** como alternativa de manutenção |
| Java | 17 como source/target (compatível com o SGV se se adoptar 21) |
| JavaFX | 21.0.2 |
| Base de TESTE | H2 2.2.224 cifrada (AES), `data/billywater.mv.db` |
| Base de REAL | PostgreSQL 42.7.3 (`real.bd.url` em `config/config.properties`; `COMECAR-REAL.bat` / `BILLYWATER_PERFIL=REAL`) |
| Build | Maven |
| PDF | OpenPDF (A5, A4) |
| Impressão Térmica | escpos-coffee (ESC/POS: talão VD, REC, fecho, teste) |
| Ícones | Ikonli Feather/Material (vectorial, **sem emojis na UI operacional**) |
| Tema | AtlantaFX 2.0.1 + CSS próprio (azul gradiente BILLY WATER preservado) |
| Offline | Sim (sem cloud/API/SMS/internet na operação normal) |

> **Compatibilidade com o prompt original:** em vez de reescrever sobre Spring Boot/MariaDB/FXML/JDK21,
> mantém-se a stack do BILLY WATER (que já está a funcionar e tem a base populada). A estrutura de
> **módulos, configuração e relatórios do SGV** é adoptada ao nível de *design de funcionalidade*, não
> obrigando a trocar de stack. (Pode-se migrar para o stack SGV depois, se for decidido.)

### 1.2 Perfis de base de dados

| Perfil | Base | Config |
|---|---|---|
| TESTE (default) | H2 cifrada | `data/billywater.mv.db` |
| REAL | PostgreSQL | `real.bd.url` em `config/config.properties` |

---

## 2. Identidade Visual / Design System (mesmo tamanho do SGV, pele refrescada)

> **Decisão combinada:** o BILLY WATER **não é totalmente idêntico ao SGV**. Mantém **o mesmo tamanho e a
> mesma linhagem de design** (cartões, tabelas, KPIs, diálogos, gradiente de topo), mas **melhora a pele**:
> **tema AZUL CLARO** (tom céu/água), **ícones Ikonli vectoriais** e alguns elementos visuais. Ver o
> documento dedicado **`DESIGN_SYSTEM_BILLY_WATER.md`**.

### 2.1 Regra de design

| Aspecto | Regra |
|---|---|
| **Tamanho/geometria** | **Idêntico ao SGV** (58px barra, 40px sub-bar, 42px cell, 24px KPI, 48dp numpad, raios, paddings) |
| **Linhagem** | Mesma estrutura de componentes (cartões, tabelas, diálogos, nav, badges) |
| **Cores** | Refrescadas para **AZUL CLARO** (tom céu/água; mantém a família azul do SGV) |
| **Ícones** | **Ikonli Feather/Material** vectoriais — **sem emojis** na UI operacional |
| **Proibido** | Mudar geometria, raios, hierarquia, ordem dos campos, comportamento das interacções |

### 2.2 Paleta “AZUL CLARO” (novos tokens)

| Papel | SGV | BILLY WATER (novo) |
|---|---|---|
| Superfície | `#F8FAFC` | `#EFF8FF` |
| Primário | `#2563EB` | `#0284C7` (sky-600) |
| Primário hover | `#1D4ED8` | `#0369A1` (sky-700) |
| Primário profundo | `#1E40AF` | `#075985` (sky-800) |
| Top bar / header | `#1E3A8A/#1D4ED8/#1E40AF` | `#075985/#0284C7/#38BDF8` |
| Sucesso | `#10B981` | `#10B981` (verde, mantém) |
| Destaque INFO | `#7C3AED` | `#0EA5E9` (sky-500) |
| Azul claro | — | `#38BDF8` / `#BAE6FD` / `#7DD3FC` |

### 2.3 Estados de célula / badges (linhagem do SGV, fundos azul-claros)

| Estado | Fundo / Texto (novo) |
|---|---|
| PAGO / COMPLETED / CONCLUÍDO | `#D1FAE5` / `#065F46` |
| EMITIDA / INFO | `#DBEAFE` / `#1E40AF` |
| PENDENTE / ESTIMADA / EM CURSO | `#FEF3C7` / `#92400E` |
| ANULADA / CANCELLED / INACTIVO | `#FEE2E2` / `#991B1B` |
| RECTIFICACAO / ENCOMENDA | `#EDE9FE` / `#5B21B6` |

### 2.4 Componentes reutilizáveis (mantidos — só a paleta/ícones mudam)

`SgvDialog`, `DetailDialog`, `StatusBadge`, `ToastNotification`, `SearchBar`, `EmptyState`,
`PaginationControl`, `ConfirmDialog`, `NotificationsCenterDialog`, `LoadingOverlay`, `ExportUtil`,
`FilterPanelBuilder`, `Formatters` (`moneyMT`, `moneyMZN`), `UiUtils` (hover/press/debounce/attachSafe).

### 2.5 Dimensões-chave (mantidas — iguais ao SGV)

| Elemento | Valor |
|---|---|
| Barra superior | 58px |
| Sub-bar de título | 40px |
| Cell de tabela | 42px |
| KPI icon | 24px |
| Numpad touch | 48×48dp |
| Raio de cartão | 10–12 |
| Raio de botão | 6 |
| Tipografia | 26–28px KPI (900) · 13px título · 12px subtítulo · 11px header tabela (800/900) · 13px célula (500) · 12–13px botão/input (600) |

---

## 3. Menu superior (única mudança estrutural)

### 3.1 Barra de topo

```
[ 💧 BILLY WATER ] | [ ⌂ Painel  Água ▾  Loja ▾  Rede ▾  Relatórios ▾  Configurações ▾  🛡️ Logs ] | [○ Caixa] [🔔 Alertas] [A Utilizador / ADMIN] [⏻ Sair]
```

> Mantém a **estrutura** (dimensões 58px, cartão de utilizador, sino de alertas, badge de caixa, botão
> Sair) do SGV, mas com a **pele azul-clara** (`#075985/#0284C7/#38BDF8`) e **ícones Ikonli**.
> Logo `💧 BILLY WATER` + badge `AURA`.

### 3.2 Itens raiz e sub-navegação

| Raiz | Sub-itens |
|---|---|
| `⌂ Painel` | Dashboard |
| `Água ▾` | Clientes & Contratos · Contadores · Zonas/Rotas · Leituras · Facturação · Tarifário · Dívidas & Cortes · Caixa/Cobrança · Histórico REC |
| `Loja ▾` | Vendas (POS/VD) · Histórico VD · Stock/Produtos · Armazéns/Transferências · Cotações COT & Serviços · Facturas de Serviços FS |
| `Rede ▾` | (opcional consolidar com Loja/Armazém — ver §14) |
| `Relatórios ▾` | Mapa de Facturação · Maiores Consumos · Apuramento IVA · Contas Correntes · Consumo por Contador · Facturação por Zona · NRW/Perdas · Vendas da Loja · Stock Valorizado · Mapa Fiscal FT/NC/VD · Leituras Estimadas · Pag. Fornecedores (ver §13) |
| `Configurações ▾` | ⚙️ Parâmetros da Empresa · 👤 Utilizadores & Permissões · 💾 Backups · 🎓 Treinamento · 🔑 Licenciamento · 🔒 Segurança |
| `🛡️ Logs` | Auditoria & Segurança |

> **Requisitos de visibilidade (preservados):** nome das operações no botão (ícone é complementar),
> *FlowPane* com quebra automática de linha, barra lateral com scroll, históricos com entrada própria no
> menu, abas internas com nomes explícitos (ex.: `Histórico VD`), pesquisa e paginação visíveis junto à lista.

---

## 4. Módulo Geral — Dashboard

### 4.1 KPIs e widgets

| KPI SGV | KPI BILLY WATER |
|---|---|
| Vendas de Hoje | **Facturado de Água do Período** |
| Artigos em Rutura | **Cobrado no Mês** |
| Contas a Receber | **Dívida Total** |
| Caixa / Gaveta | **Vendas da Loja** |

Extras: nº de clientes, taxa de cobrança, série dos últimos 6 meses, **alertas de leituras em falta**,
**ordens de serviço pendentes**, **alertas de stock**, **top de produtos da loja**, **backup manual** e
**último backup**.

### 4.2 Gráficos

- `LineChart` “Facturação Diária — Últimos 7 dias” / “Série 6 meses”.
- `PieChart` “Meios de Pagamento — Hoje”.

### 4.3 Técnico

- Consultas do Dashboard **fora da thread JavaFX** (`Ui.emSegundoPlano` — não bloquear o login).
- **Não carregar listas completas** no Dashboard; usar totais/agregados.
- Quick actions: `Fazer Factura` (secundário), `Nova Leitura (Emissão)` (primário).

---

## 5. Módulo Água — Clientes & Contratos

### 5.1 Dados

Nome completo, **NUIT**, telefone, bairro, quarteirão, casa, estado, **limite de crédito**,
**contrato**, **conta do contrato**, **categoria tarifária**, **rota de leitura**, **contador associado**.

### 5.2 Operações

- Criar **cliente + contrato + contador** numa **operação transaccional**.
- Editar, definir limite de crédito, activar/inactivar.
- Pesquisar por nome, conta, telefone ou bairro · paginação.
- **Conta-corrente** (débitos FT/VD/FS/COT, créditos REC, NC, transferências) com paginação.
- **Transferência de documentos** para outro cliente (FT, VD, FS, COT selecionáveis separadamente).
- **Mudança de titularidade** do contrato com motivo e `aceite_por` · **termo de transferência** ·
  auditoria.
- **Regras:** proibida transferência para si; destinatário obrigatório; motivo obrigatório; aceitação
  confirmada; dívida pode seguir para o novo titular ou ficar no anterior.
- **CAIXA pode consultar** clientes, mas não editar sem permissão.

---

## 6. Módulo Água — Contadores / Medidores

### 6.1 Dados

Número de série, marca, calibre, leitura inicial, data de instalação, estado (**BOM, AVARIADO, RETIRADO**),
cliente e conta associados, zona/rota.

### 6.2 Operações

- Lista paginada · pesquisa por série/cliente/conta/marca/zona · filtro por estado.
- Criar contador associado a contrato; marcar avariado/normal; retirar.
- **Histórico de leituras** paginado (anterior/actual/consumo). Exportar página para CSV.

### 6.3 Melhorias a prever (SGV-estrutura)

- Permitir **contador livre** (cliente/contrato nulo) para stock de contadores.
- Estados operacionais mapeados para a UI (Activo / Suspenso / Cortado) mantendo os existentes BOM/AVARIADO/RETIRADO.

---

## 7. Módulo Água — Zonas / Rotas

- Registrar rota/zona; associar bairros; pesquisar; paginar.
- Mostrar **nº de clientes** e **média histórica de consumo** da rota.
- Usar a rota para carregar a **pauta de leituras**; editar rota.
- A tabela `rota` funciona como zona/rota (mantido); sem entidade `zone` separada (decisão mantida).

---

## 8. Módulo Água — Leituras de Consumo

### 8.1 Fluxo

1. `ÁGUA → Leituras` → escolher **rota** → **período `AAAA-MM`** → carregar **pauta**.
2. Pesquisar cliente/conta/contador.
3. Introduzir **leitura actual** na coluna `Actual`; sistema calcula consumo.
4. Clicar **Registar leituras desta página** (etiqueta explícita — melhoria).
5. Auditoria + anomalias. Depois, gerar o **lote de facturas**.

### 8.2 Pauta

- Paginação mín. 10 linhas · pesquisa cliente/conta/contador · leitura anterior visível ·
  leitura actual editável · consumo = actual − anterior · tipo **MEDIDA/ESTIMADA** · estado do contador ·
  **média dos 6 consumos anteriores** · crítica visual de anomalias · gravar só a página actual ·
  **importar/exportar CSV** · utilizar o HTML offline do leitor.

### 8.3 Critérios AURA

- Leitura < anterior → anomalia `Leitura < anterior!`.
- Consumo zero → alerta de verificação.
- Consumo ≥ 3× média → possível fuga/erro.
- Contador avariado / sem leitura → **estimativa**.
- Com 6 consumos → média arredondada; sem histórico → **consumo mínimo da tarifa (≈5 m³)**.

### 8.4 Melhorias a prever

- Ciclo de leituras com estado **RASCUNHO → SUBMETIDA → VALIDADA** e aprovação de supervisor (opcional,
  a decidir operacionalmente — ponto em aberto na secção 23 do BILLY).

---

## 9. Módulo Água — Tarifário AURA & Cálculo

### 9.1 Parâmetros (configurados; lidos da base, não fixos na UI)

**Doméstico (Maputo/Matola/Boane):**

| Parâmetro | Valor |
|---|---|
| Minimo 5 m³ | 143,00 MT |
| Isenção de IVA no mínimo | sim (parametrizada) |
| Taxa de disponibilidade | 99,00 MT |
| Escalão 5–10 m³ | 55,55 MT/m³ |
| Escalão 10–25 m³ | 72,83 MT/m³ |
| Acima de 25 m³ | 188,65 MT/m³ |

**Outras categorias:** Comércio mín. 15 m³ = 1.085,59 MT · Indústria mín. 25 m³ = 1.809,32 MT ·
Fontanário 10 MT/m³ (isento) · Acima do mínimo geral 72,37 MT/m³.

**Impostos/taxas:** IVA geral 16% · IVA da água sobre 75% da base · Saneamento por factura ·
Taxa de religação 750 MT (antes de IVA).

### 9.2 Regras

- Tarifa escolhida por **categoria + data de vigência**.
- Tarifas e escalões lidos da base; o motor **não fixa valores na UI**.
- Resultado discrimina **linhas, subtotal, IVA (água), saneamento, total**.
- **Simulador** não emite documento.
- Consultar tarifas/escalões paginados; pesquisar por categoria/código; **simular consumo**.

### 9.3 A adicionar (estrutura SGV)

- **Editor completo** de tarifas/escalões com vigência, criação, edição, remoção lógica e validação de
  intervalos pela interface (equivalente ao editor de Categorias/Unidades do SGV).

---

## 10. Módulo Água — Facturação FT

### 10.1 Fluxo

1. Leituras registadas → `ÁGUA → Facturação` → período `AAAA-MM`.
2. Sistema identifica contratos activos com leitura.
3. Motor calcula a tarifa → gera série FT + nº sequencial.
4. Lista FT paginada; consultar, imprimir, **PDF A5**, **segunda via**, **lote PDF** (1 factura/página).
5. **Anulação segundo as regras**; emissão de **nota de crédito NC**.

### 10.2 Regras de documentos

- **Uma FT permanece FT**; nunca substituída por REC.
- **REC** só é criado quando há pagamento.
- Factura **não é apagada; é anulada**.
- Conteúdo FT: cliente, conta, categoria, período, consumo, linhas tarifárias, subtotal, IVA,
  saneamento, total, **limite de pagamento**.

### 10.3 Limitação de decisão

A FT de água é gerada por **lote** a partir de leituras/contrato (não há formulário de FT individual
digitada — mantido como opção operacional). Se necessário, criar FT manual como melhoria futura.

---

## 11. Documentos & Séries

| Código | Documento | Regra |
|---|---|---|
| FT | Factura de água | gerada a partir de leitura/contrato; não muda de tipo |
| VD | Venda a dinheiro | talão da Loja POS |
| FS | Factura de serviços | separada da COT |
| COT | Cotação | não fiscal; pode ser paga sem virar factura |
| REC | Recibo | comprovativo do pagamento |
| DV | Devolução | devolução da Loja |
| NC | Nota de crédito | correcção da FT |
| RC | Recarga pré-paga | documento de recarga |
| TRB | Referência interna | legado; apresentação usa COT |

**Regra COT:** `COT → pagamento → REC`. A COT não se torna FT nem FS; permanece arquivada; pagamento pode
ser total ou parcial.

---

## 12. Módulo Água — Caixa, Cobranças & REC

- Seleccionar cliente por pesquisa; consultar dívida FT/VD/FS/COT; paginação dos pendentes.
- **Pagamento FIFO** por defeito; pagamento dirigido a uma FT; pagamento **parcial**.
- Métodos: numerário, M-Pesa, e-Mola, mKesh, POS, banco; referência para métodos não numerários.
- **Geração de REC sequencial**; visualizar; PDF; impressão térmica; actualização de saldo/estado.
- **Sessão de caixa:** abrir com fundo de maneio; entradas/saídas manuais; cobranças automáticas à sessão;
  movimentos paginados; fechar com valor contado; **valor esperado**; **diferença**; histórico de sessões;
  **Fecho Z / relatório diário por operador**.
- **Histórico REC** paginado por REC/cliente/método/operador; ver REC; PDF.
- **Regras corrigidas (lição SGV):** só DINHEIRO move a gaveta física; troco persistido; sem estorno geral
  de REC como operação independente (a Loja tem devolução/reembolso próprio); sem crédito automático acima
  da dívida (valor superior é rejeitado).

---

## 13. Módulo Relatórios (estrutura completa SGV)

> **Estrutura SGV adoptada na totalidade** — um relatório por “botão” de menu (sem abas escondidas),
> com cabeçalho de página, filtros e exportação (Excel/PDF/CSV). Todo o universo reportável usa um único
> **predicado** para garantir que **dashboard = mapa = apuramento = caixa** (paridade de números).

| # | Relatório (BILLY WATER) | Conteúdo | Formato |
|---|---|---|---|
| 1 | **Mapa de Facturação de Água** | Facturado por período/zona/categoria | PDF/Excel |
| 2 | **Maiores Consumos** | Top consumidores/contadores por m³ e valor | PDF/Excel |
| 3 | **Apuramento de IVA** (água + loja) | Base tributável/isenta **por linha** (não por doc); CIVA; NC abate | PDF/Excel |
| 4 | **Contas Correntes (devedores)** | Aging por faixas; só FT/VD/FS (não COT não paga) | PDF/Excel |
| 5 | **Consumo por Contador / Kardex** | Histórico de leituras (anterior/actual/consumo) | PDF/Excel |
| 6 | **Facturação por Zona** | Agregação por rota/zona | PDF/Excel |
| 7 | **NRW / Água Não Facturada (Perdas)** | Produção vs facturada | PDF/Excel |
| 8 | **Leituras Estimadas** | Contadores sem leitura física facturados por estimativa | PDF/Excel |
| 9 | **Vendas da Loja por Produto** | Top/vendas VD | PDF/Excel |
| 10 | **Stock Valorizado** | Materiais/produtos por armazém, custo | Excel/PDF |
| 11 | **Mapa Fiscal FT/NC/VD** | Universo fiscal | PDF/CSV/Excel |
| 12 | **Pagamentos a Fornecedores** | Compras + pagamentos (Rede) | PDF/Excel |
| 13 | **Resumo Facturação vs Cobrança** | Série mensal facturado vs cobrado | Excel |
| 14 | **Cobrança por Operador e Método** | Recebimentos por operador/método | Excel |
| 15 | **SAF-T MZ** | Exportação XML oficial (opcional) | XML |

**Exportação:** `ExportUtil` (Excel/POI), PDFBox/OpenPDF (A4/A5), CSV (leituras/logs/página).
**Paridade:** usar `isReportable()` (estado ≠ ANULADA, não demo/training, tipo FT/VD/FS/NC) em todos.

---

## 14. Módulo Loja / Armazém / Stock — CORRECÇÃO DE CONSISTÊNCIA

> **Problema identificado no BILLY WATER:** stock, armazéns e loja apareciam desconectados (o produto
> comprado entrava num “armazém”, a venda consumia outro “stock”; não havia um fluxo único e visível).
> **Esta secção reescreve esse módulo para ficar consistente.** É a correcção central pedida.

### 14.1 Modelo único e visível de stock/locais

Cria-se **um único conceito de “local de stock”** com um tipo:

| Tipo de local | Função |
|---|---|
| **ARMAZÉM** (central) | Recebe compras de materiais e de produtos |
| **LOJA / PONTO DE VENDA** | Onde se vende e se faz POS/VD |

- Produto/material pode existir em N locais; o total = soma dos locais.
- O PDV/loja **só vende** o stock do seu local (loja). O stock do armazém não é vendável directamente.
- **Ponte obrigatória e visível:** compra → armazém → **guia de transferência** → loja. Sem transferência,
  o artigo não é vendável no PDV (e isso é informado com um painel “Por transferir”).

### 14.2 Produtos / Materiais

- Código, código de barras, nome, categoria, unidade, preço compra, preço venda, IVA, stock actual,
  stock mínimo, estado.
- Pesquisa + paginação; **entrada de mercadoria**; auditoria; **movimentos de stock (kardex)**.

### 14.3 Armazéns

- Armazém Central criado na migração; criação de novos armazéns (código, nome, localização).
- **Stock por armazém** com pesquisa.
- **Transferência origem→destino** com regras: origens ≠ destinos; quantidade > 0; verificação de stock
  disponível; **operação atómica** (sai de um, entra no outro, ou nada); referência/guia; operador;
  data/hora; auditoria; **histórico paginado**.

### 14.4 Loja / POS — Vendas VD

- Pesquisa produto por nome/código/código de barras · categorias · lista paginada.
- Carrinho; alteração de quantidades; Consumidor Final ou cliente identificado; **venda por conta**
  (com limite de crédito); associação opcional a FT de água; desconto com limite por perfil.
- **Validação de stock** no local da loja; **saída automática** de stock; documento **VD**; impressão
  térmica.
- **Histórico VD** paginado · pesquisa por VD/cliente · visualização do talão · **devolução parcial**
  (motivo obrigatório) · reposição de stock · abate ao saldo por conta ou reembolso em dinheiro ·
  documento **DV** · anulação de VD (sem apagar) · reposição só da quantidade ainda não devolvida.

### 14.5 Fluxo unificado (front-to-back)

```
Compra → entra no ARMAZÉM (kardex ENTRADA_ARMAZEM)
  → Guia de Transferência ARMAZÉM→LOJA (PENDING → IN_TRANSIT → COMPLETED)
  → entra no stock da LOJA (kardex ENTRADA_LOJA)
  → Venda (POS/VD) sai do stock da LOJA (kardex SAIDA_LOJA)
  → Devolução volta ao stock da LOJA (kardex ENTRADA_DEVOLUCAO)
```

- **Kardex unificado** por (local, produto) com stock antes/depois, referência e operador.
- **Painel “Por transferir”** no armazém alerta para artigos no armazém ainda não transferidos.

### 14.6 Cotações / Serviços (canalização) — COT & FS

- Catálogo de serviços (código, nome, preço, por hora ou fixo) · pesquisa/paginação · criação/validação.
- **COT/trabalho:** descrição obrigatória; técnico; mão-de-obra; materiais da loja; total + IVA;
  aprovar; concluir (retirar materiais do stock ao concluir); pagar directamente (gera REC); emitir **FS**
  separada; histórico FS; transferência FS/COT com aceitação.
- Estados: `ORCAMENTO → APROVADO → CONCLUIDO`; depois `PAGA/PARCIAL` (COT + REC) ou `FS` separada.

---

## 15. Módulo Dívidas, Cortes & Prestações

- **Aging por faixas** de atraso; pesquisa por devedor/nome/conta/bairro; lista paginada; nº de facturas
  vencidas; valor da dívida; estado do contrato.
- **Operações:** emitir aviso de corte (PDF); criar **ordem de corte**; **ordem de religação**; emitir
  **taxa de religação**; alterar estado do contrato quando a ordem concluída.
- **Plano de prestações:** 2–12 prestações; registar entrada; pagar prestação; consultar paginado.
- **Ordens de serviço** paginadas; requisitar material para uma O.S.

---

## 16. Módulo Sistema / Configuração (estrutura completa SGV)

> **Estrutura SGV adoptada na totalidade.** Área exclusiva do ADMIN.

### 16.1 ⚙️ Parâmetros da Empresa

- Nome, NUIT, alvará/licença, endereço, cidade/província, telefone, e-mail, **rodapé documental**.
- **IVA geral** · **percentagem da base IVA da água** · **taxa de saneamento** · **taxa de religação** ·
  **dia limite de pagamento** · **séries FT/VD** · **pasta de backups** · **logótipo** ·
  **impressora térmica** + activação.
- Gravar · validar nome/NUIT/séries/percentagens/taxas/dia limite · carregar logótipo · **testar
  impressora** · **restaurar backup** (confirmação dupla + fecho da app).

### 16.2 👤 Utilizadores & Permissões

- Perfis: **ADMIN**, **GESTOR**, **CAIXA**, **LEITOR**, **LOJA**.
- Login por utilizador/senha com hash; credenciais iniciais `admin/admin123`; **mudança obrigatória de
  senha no 1.º acesso**; mínimo 6 caracteres; **5 tentativas erradas bloqueiam**; desbloqueio por admin;
  activar/inactivar; **não permitir desactivar a própria conta**; erros de login na própria janela.
- **Matrix de permissões por módulo** (estrutura SGV `PAGE:ACTION`) com editor configurável (melhoria a
  prever — hoje é `Permissoes.java` em código).
- RBAC aplicado a **nível de serviço** (não só na UI) em todas as escritas.

### 16.3 💾 Backups & Restauro

- Base TESTE H2 cifrada (AES) com chave em `config/chave.local` e marca `config/bd.cifrada`.
- Backup SQL local; **retenção** dos últimos; backup automático 30s após arranque; thread de prioridade
  mínima; backup manual no Dashboard/Parâmetros; PostgreSQL usa `pg_dump`/`psql`.
- **Restauro** com salvaguarda do estado anterior; dupla confirmação; fecho da app.
- Verdadeiro (dump recuperável + checksum SHA-256) — corrige a falha do SGV.

### 16.4 🎓 Treinamento

- Modo de formação separado com dados simulados, sem afectar a produção (marca `isTraining`/`demoFlag`
  para não contaminar relatórios).

### 16.5 🔑 Licenciamento

- Estado da licença, activação, dados da instalação; **binding por machineId**; bloqueio em expirado
  (corrige a falha do SGV).

### 16.6 🔒 Segurança

- Alteração de palavra-passe (mín. 6, verificação da actual, confirmação) e boas práticas.

---

## 17. Módulo Auditoria & Logs (estrutura SGV)

- Registos: login/alterar senha; criação/edição de clientes; transferências; emissão/anulação FT;
  pagamentos e REC; vendas VD; devoluções/anulações loja; alterações de stock; transferências de armazém;
  criação COT/emissão FS; alterações de contadores; parâmetros e logótipo; exportações e operações admin.
- **LogsView** com filtro por **categoria, utilizador, acção, detalhe**, **paginação** e exportação CSV.
- Estrutura SGV com tabs (Todos/Erros/Segurança/Utilizadores/Sistema) e stacktrace, mantendo os filtros
  e paginação do BILLY.

---

## 18. Modelo de Dados

### 18.1 Tabelas operacionais (compõem o núcleo actual)

```
utilizador  parametro  auditoria  categoria_tarifaria  rota  cliente  contrato  contador
tarifa  escalao_tarifa  leitura  factura  linha_factura  pagamento  pagamento_factura
ordem_servico  produto  venda  linha_venda  movimento_stock  plano_prestacao  prestacao
plano_factura  nota_credito  conta_prepaga  recarga
```

### 18.2 Tabelas acrescentadas pelas migrações

```
transferencia  transferencia_factura  transferencia_venda  transferencia_trabalho
pagamento_venda  servico  trabalho  trabalho_servico  trabalho_material  pagamento_trabalho
factura_servico  linha_factura_servico  pagamento_factura_servico  transferencia_factura_servico
armazem  stock_armazem  transferencia_armazem  sessao_caixa  movimento_caixa  versao_bd
```

### 18.3 Ajustes para a correcção Loja/Armazém

- Unificar stock por **local** com tipo (`armazem`/`loja`) — sem duplicar `stock_armazem` + outro stock.
- `movimento_stock` passa a registar **local**, stock antes/depois, referência, operador (kardex unificado).
- `transferencia_armazem` estendida para qualquer par de locais (armazém↔loja), com estados.
- Painel “Por transferir” agregado num query por local=armazém sem stock em loja.

---

## 19. Migrações (versões)

| Versão | Conteúdo |
|---|---|
| 1–8 | Esquema, segurança, índices, auditoria |
| 9 | Transferências entre clientes |
| 10 | Vendas por conta + limite de crédito |
| 11 | Serviços e trabalhos |
| 12 | Índices para dados massivos |
| 13 | Devoluções parciais |
| 14 | Transferências explícitas VD/serviços |
| 15 | FS separado |
| 16 | Armazéns |
| 17 | COT documental/pagável |
| 18 | Sessões e movimentos de caixa |
| 19+ (novas) | Unificação stock por local (correcção Loja/Armazém), ciclo de leituras (opcional), editor de permissões, configurações SGV completas |

---

## 20. Segurança / Permissões

| Perfil | Acesso |
|---|---|
| ADMIN | Todos os módulos e operações administrativas |
| GESTOR | Operação geral, excepto Parâmetros e Utilizadores |
| CAIXA | Dashboard, clientes em consulta, contadores, zonas, caixa e REC |
| LEITOR | Dashboard, contadores, zonas e leituras |
| LOJA | Dashboard, POS/VD, stock, armazéns e serviços |

> **Nota:** adoptar matriz `PAGE:ACTION` do SGV (com editor configurável) para granularidade real, e
> validar permissões **no serviço** (não só na UI) para todas as escritas.

---

## 21. Requisitos Não Funcionais

- 100% offline na operação normal.
- Windows 10/11 (JDK/Maven ou empacotado); comportamento visual a validar em ecrã real.
- Português moçambicano; valores em MT; datas no formato local; períodos `AAAA-MM`.
- **Sem emojis na UI operacional** (ícones vectoriais Ikonli); azul gradiente BILLY WATER preservado.
- Tabelas com estados visuais; estados vazios com explicação + acção sugerida.
- Auditoria de operações sensíveis; transacções para financeiro/stock.
- **Não apagar** a base de teste populada; separar configuração/dados REAL da base TESTE.
- Não expor a chave H2; backup antes de operações destrutivas.
- Paginação (mín. 10), filtros na base, debounce 180ms, selectores de cliente até 80 resultados,
  executor daemon 4 tarefas, cache H2 64MB, dashboard assíncrono, backup atrasado.

---

## 22. Volumes da base preservada (homologação)

- 10.000 clientes · 480.000 facturas · 480.000 leituras · 160.001 pagamentos · 10.000 vendas ·
  1.500 trabalhos/COT · 12 FS · 2 armazéns · stock por armazém · transferências · logs e auxiliares.

---

## 23. Matriz de requisitos confirmados (estado actual)

| Requisito | Estado | Evidência |
|---|---|---|
| Paginação em todas as listas/históricos | Implementado | `Paginacao`, `pagina*` |
| Mínimo 10 registos | Implementado | `Math.max(10, limite)` |
| Filtros de pesquisa | Implementado | `ComponentesPesquisa` |
| FT separada de REC | Implementado | `FacturaDAO`, `ServicoCaixa` |
| VD venda a dinheiro | Implementado | `ServicoLoja` |
| FS separado de COT | Implementado | `factura_servico` |
| COT paga gera REC sem virar factura | Implementado | migração 17 |
| Transferências cliente→cliente (FT/VD/FS/COT) | Implementado | `ServicoTransferencias` |
| Transferência armazém↔armazém atómica + histórico | Implementado | `ServicoArmazens` |
| Base TESTE / REAL separadas | Implementado | H2 + PostgreSQL |
| Menu/históricos visíveis | Implementado | `MainView`, FlowPane, sidebar |
| **Correcção Loja/Armazém (stock por local + ponte obrigatória)** | **Pendente — a implementar** | §14 |

---

## 24. Estado de aceitação

- Testes headless: Fase 1 (11 OK), Fase 21 (13 OK), Fase 22 (15 OK), Fase 23 (15 OK),
  Fase 26 (32 OK), Fase 27 (14 OK).
- `mvn clean package`: OK com Java 17 · integridade ZIP OK · download OK · base populada OK ·
  PostgreSQL configurada (servidor não disponível para homologação final).
- **A validar em Windows:** instalador, comportamento visual em resolução baixa, tempos de abertura de
  módulos grandes, pesquisa com acentos.

---

## 25. Pendências a decidir (não implementadas — carecem de decisão técnica/legal/operacional)

1. Spring Boot / MariaDB / FXML / JDK 21 obrigatório (decisão de stack).
2. Integração automática M-Pesa/e-Mola (hoje só registo manual).
3. Hash fiscal AT / QR Code / comunicação oficial.
4. SMS / WhatsApp.
5. Multi-filial / multi-distrito.
6. Licenciamento anual + modo de formação separado.
7. NRW formal / registo de perdas.
8. Fotografia/GPS na leitura.
9. Entidade formal de ciclo de facturação (OPEN/CLOSED/INVOICED).
10. Validação obrigatória de cada leitura por supervisor antes da FT.
11. Editor configurável de permissões por módulo (SGV `PAGE:ACTION`).
12. Pagamentos acima da dívida convertidos em crédito.
13. Estorno geral de REC como operação independente.
14. Contadores livres sem contrato associado (stock de contadores).
15. FT manual individual (hoje só por lote).

> **Excluído por irrelevante (sem relação com água):** fabrico/panificação, BOM de pão, produção
> industrial de alimentos, gestão de padaria, aulas/treino de cursos, qualquer módulo estranho à
> concessionária de água.

---

## 26. Checklist de aceitação final

- [ ] **Mesmo tamanho/geometria do SGV**; identidade propia (tema água-ciano) coerente (ver `DESIGN_SYSTEM_BILLY_WATER.md`).
- [ ] Menu superior adaptado à água (55px estrutura preservada, mesma linhagem).
- [ ] Dashboard com todos os KPIs e widgets (§4) + consultas assíncronas.
- [ ] Água: clientes/contratos/contadores, zonas/rotas, leituras, tarifário AURA, facturação FT,
      caixa/cobrança/REC, dívidas/cortes/prestações.
- [ ] Loja/Armazém **consistentes**: stock por local, ponte compra→armazém→loja visível, kardex unificado,
      painel “Por transferir”, POS/VD, devoluções, armazéns/transferências atómicas, COT/FS.
- [ ] **Configurações completas SGV** (Parâmetros, Utilizadores & Permissões, Backups, Treinamento,
      Licenciamento, Segurança).
- [ ] **Relatórios completos SGV** (estrutura de 15 relatórios + paridade de números + exportação).
- [ ] Auditoria/Logs com filtros, paginação e exportação CSV.
- [ ] Segurança por serviço, bloqueio de conta, mudança de senha obrigatória.
- [ ] Backup/restauro reais + retenção + restauro seguro.
- [ ] Paginação (mín. 10), filtros na base, debounce 180ms, assíncrono, cache — em todas as listas.
- [ ] Base TESTE populada preservada; perfil REAL PostgreSQL configurado.

---
*Consolidado com base no documento BILLY WATER v4.8.1 e na estrutura de Configurações/Relatórios do SGV,
corrigindo a consistência do módulo Loja/Armazém/Stock, mantendo o mesmo tamanho/geometria do SGV e dando
uma identidade visual própria (água-ciano) sem fugir da linhagem do SGV.*
