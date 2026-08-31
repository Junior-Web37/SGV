# 🤖 BILLY WATER — Protocolo de Entrega para a IA que constrói o sistema

> **O que é este ficheiro:** instruções de execução para a IA que vai *construir* o BILLY WATER.
> Serve de "contrato" para ela **não se perder** (falta de direcção) e **não se empolgar** (scope creep /
> inventar funções). Leia antes de entregar o projecto a uma IA.

---

## ⚠️ Resposta directa à pergunta: "Este doc já serve para eu dar a uma IA?"

**Serve como base sólida, mas NÃO basta sozinho.** Para a IA fazer *tudo como deve ser*, sem se perder e
sem se empolgar, precisa de **3 coisas** em conjunto:

| # | O que falta / o que precisa | Porquê |
|---|---|---|
| 1 | **O código-fonte do BILLY WATER actual** (o sistema que já funciona) | O plano menciona classes reais (`LeiturasView`, `MotorTarifario`, `TarifarioView`, `ServicoLoja`, `ServicoArmazens`, `PagamentoDAO`, …). Uma IA que não tenha **este** código à frente vai "inventar" uma versão própria e o resultado fica inconsistente com o que você já tem. O repositório atual tem o **SGV**, não o BILLY WATER. |
| 2 | **Este protocolo** + um **"Master Prompt"** curto (abaixo) | Dão à IA o papel, o escopo e as regras. Sem isto, a IA pode tentar implementar os 8 módulos de uma vez, ou adicionar features que ninguém pediu. |
| 3 | **Entrega por fases, com "go" a cada fase** | Evita que ela rode descontrolada. Ela implementa uma fase, mostra, e só avança com a sua aprovação. |

> **Conclusão:** entregue à IA **o código-fonte do BILLY WATER** + **este protocolo** + o
> `PLANO_REQUISITOS_BILLY_WATER.md` + o `DESIGN_SYSTEM_BILLY_WATER.md`. O documento de requisitos é a
> **fonte de verdade das funcionalidades**; este protocolo é o **freio** que a mantém no caminho.

---

## 1. Papel da IA (definir logo no início)

```
És o programador responsável por evoluir o BILLY WATER (sistema desktop de facturação de água, caixa,
loja, armazéns e serviços) que JÁ EXISTE e FUNCIONA. NÃO estás a criar do zero: estás a EVOLUIR o código
que te será fornecido. A tua missão é implementar as funcionalidades descritas em
PLANO_REQUISITOS_BILLY_WATER.md e o visual em DESIGN_SYSTEM_BILLY_WATER.md, SEM nunca quebrar o que já
funciona e SEM inventar funcionalidades que não estão pedidas.
```

---

## 2. Regras SIM / NÃO (ler antes de mexer)

### ✅ SIM (obrigatório)
- Manter a **stack actual** do BILLY WATER (JavaFX + Java 17 + H2 cifrada + PostgreSQL + Maven).
- Manter a **base de teste populada** (nunca apagar/resetar `data/billywater.mv.db`).
- Seguir **a geometria/dimensões do SGV** (58px barra, 40px sub-bar, 42px cell, 24px KPI, 48dp numpad).
- Aplicar o **tema AZUL CLARO** e os **ícones Ikonli** do `DESIGN_SYSTEM_BILLY_WATER.md`.
- Aplicar **paginação (mín. 10)**, **filtros na base**, **debounce 180ms**, **consultas assíncronas**,
  **transacções** em operações financeiras/stock, **auditoria** nas operações sensíveis.
- **RBAC por serviço** (não só na UI) em todas as escritas.
- **Perguntar** quando uma instrução for ambígua ou conflituar com o código actual.

### ❌ NÃO (proibido)
- **NÃO reescrever do zero** entidades/fluxos que já existem e funcionam.
- **NÃO trocar a stack** (não migrar para Spring/MariaDB/HTML) a menos que eu peça explicitamente.
- **NÃO implementar os itens da secção 25 do plano** (pendências que exigem decisão técnica/legal). Estas
  são para **perguntar**, nunca para implementar por iniciativa própria.
- **NÃO adicionar** funcionalidades fora do plano (sem módulo de `backup`, `RFID`, `app móvel`, etc.).
- **NÃO alterar a geometria** do layout; só a **cor e o ícone**.
- **NÃO apagar dados** nem "limpar" a base para "simplificar".
- **NÃO trabalhar em vários módulos ao mesmo tempo**; seguir a ordem de fases (§4).

---

## 3. Fonte de verdade e hierarquia

| Documento | O que define | Se houver conflito |
|---|---|---|
| **Plano de Requisitos** (`PLANO_REQUISITOS_BILLY_WATER.md`) | O QUE o sistema faz (funcionalidades, regras de negócio, telas) | **Ganha** sobre o resto |
| **Design System** (`DESIGN_SYSTEM_BILLY_WATER.md`) | COMO se parece (cores azul-claro, ícones, dimensões) | Segue-se o design |
| **Este protocolo** | COMO trabalhar (fases, limites, perguntas) | Governa o processo |
| **Código BILLY WATER actual** | Como as coisas já estão feitas | Só se altera para cumprir o plano |

---

## 4. Ordem de trabalho por FASES (implementar COMPLETA antes de avançar)

> Regra: **terminar uma fase e pedir aprovação/mostrar** antes de começar a seguinte. Isto impede a IA de
> "empolgar-se" e fazer tudo de uma vez.

**FASE 0 — Auditoria & alinhamento.** A IA **não altera código**. Só lê o código BILLY WATER e confirma
que entendeu a estrutura actual (onde estão as views, DAOs, serviços, séries de documentos). Entrega um
resumo de 1 página do que já existe vs o que falta. **Pára para eu validar.**

**FASE 1 — Design System.** Aplicar o tema azul-claro + ícones Ikonli + dimensões nas telas principais
(Login, Dashboard, menu). **Sem lógica de negócio nova.** Validação visual.

**FASE 2 — Consistência Loja / Armazém / Stock** (a correcção principal). Unificar stock por local,
ponte compra→armazém→loja, kardex unificado, painel "Por transferir". Testar sem quebrar o que existe.

**FASE 3 — Água.** Clientes/contratos/contadores, zonas/rotas, leituras, tarifário, facturação FT.

**FASE 4 — Caixa, cobranças e REC.** Sessões, movimentos, pagamentos FIFO/parciais, REC, histórico.

**FASE 5 — Dívidas, cortes e prestações.**

**FASE 6 — Configurações (estrutura SGV).** Parâmetros, Utilizadores & Permissões, Backups, Treinamento,
Licenciamento, Segurança.

**FASE 7 — Relatórios (estrutura SGV).** Os 15 relatórios com paridade de números + exportação.

**FASE 8 — Auditoria/Logs + passagem de testes.** Validar os testes headless e `mvn clean package`.

---

## 5. Master Prompt (colar no início da conversa com a IA)

> Vou desenvolver o **BILLY WATER** (sistema desktop JavaFX de facturação de água, caixa, loja, armazéns
> e serviços). O código-fonte actual do *BILLY WATER* será fornecido. Deves examiná-lo primeiro e não
> reescrever do zero.
>
> **Fontes de verdade** (nesta ordem): `PLANO_REQUISITOS_BILLY_WATER.md` (o que fazer) >
> `DESIGN_SYSTEM_BILLY_WATER.md` (como parece) > código actual.
>
> **Regras:** não trocas a stack, não apagas a base de teste populada, não implementas os itens da secção
> 25 do plano (só perguntas), não adicionas funcionalidades fora do plano, não alteras a geometria (só cor
> e ícone), aplicas paginação mín. 10 / filtros na base / debounce 180ms / assíncrono / transacções /
> auditoria / RBAC por serviço.
>
> **Trabalhas por fases** (Fase 0 → 8), terminando cada uma e pedindo o meu "go" antes da seguinte. Quando
> algo for ambíguo ou conflituar com o código actual, **perguntas** e não assumes.
>
> Começa pela **Fase 0 (auditoria)** e entrega só o resumo do que já existe vs o que falta, sem alterar
> código.

---

## 6. Como responder (definições de "feito")

Cada funcionalidade só é **concluída** quando:
1. **Existe o fluxo** (não só uma classe ou botão) — a acção completa o seu efeito na base.
2. **Está persistido** na base correcta e reflectido onde deve (saldo, stock, estado, caixa).
3. **Tem permissões** aplicadas no serviço (RBAC).
4. **Tem auditoria** se for operação sensível.
5. **Está paginado/filtrado** se listar muitos registos.
6. **Tem paridade de números** (dashboard = mapa = apuramento = caixa) onde aplicável.
7. Tem **estado vazio** com acção sugerida e **estados visuais**.
8. Compila (`mvn clean package`) e passa os testes headless existentes.

---

## 7. O que a IA DEVE fazer se tiver dúvida

| Situação | Acção da IA |
|---|---|
| Instrução ambígua | Fazer a pergunta e esperar a resposta. **Não assumir.** |
| Conflito entre plano e código actual | Avisar e perguntar como proceder. |
| Item da secção 25 do plano | **Não implementar.** Registrar como "pendente, requer decisão". |
| Funcionalidade não prevista no plano | **Não implementar.** Sugerir e esperar aprovação. |
| Risco de quebrar algo existente | Avisar antes de mexer; propor alternativa mais segura. |
| Dúvida sobre valor de negócio (ex.: tarifa, IVA, taxas) | Usar SEMPRE os valores vindos da **base de dados**, nunca hardcodar. |

---

## 8. Ponto crítico para ti, dono do projecto (leia)

- O **BILLY WATER é um sistema que já existe**. A entrega à IA só será fiável se a IA receber **o código
  actual**; caso contrário ela "recriará" à sua maneira e o resultado **não** será o que você já tem.
- **Não entregue tudo de uma vez.** Use a Fase 0 para a IA provar que **compreendeu o que já existe** antes
  de ela escrever código. Só avance quando você validar.
- **Os itens da secção 25 não estão implementados** por decisão (dependem de tecnologia/legislação).
  A IA **não** deve implementá-los sozinha. Quando quiser, decidimos um por um.

---
*Protocolo de execução para a IA: papel, limites, fases, fonte de verdade e definições de "feito". Use em
conjunto com `PLANO_REQUISITOS_BILLY_WATER.md` e `DESIGN_SYSTEM_BILLY_WATER.md`.*
