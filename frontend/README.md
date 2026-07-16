# SGV Frontend

Interface gráfica do SGV (Sistema de Gestão de Vendas) — Facturação electrónica conforme AT de Moçambique.

## Stack

- **React 18** + **TypeScript**
- **Vite** (build tool)
- **Tailwind CSS** + **shadcn/ui** (componentes)
- **React Router** (navegação)
- **TanStack Query** (data fetching/cache)
- **Axios** (HTTP client)
- **Sonner** (toasts)
- **Lucide React** (ícones)

## Setup

```bash
cd frontend
npm install
npm run dev
```

Abre em `http://localhost:5173`. O Vite faz proxy para a API em `http://localhost:8080`.

## Build de produção

```bash
npm run build
```

Gera `dist/` — servir pelo Spring Boot em `src/main/resources/static/` ou via Nginx.

## Estrutura

```
src/
├── components/
│   ├── Layout.tsx          # Sidebar + header
│   └── ui/                 # Componentes base (shadcn-style)
├── lib/
│   ├── api.ts              # Axios + auth header
│   ├── auth.tsx            # AuthContext + login
│   ├── types.ts            # Tipos TypeScript do domínio
│   └── utils.ts            # cn(), formatMZN(), formatNuit()
├── pages/
│   ├── LoginPage.tsx
│   ├── SetupWizardPage.tsx # Wizard 1ª vez
│   ├── DashboardPage.tsx   # Conformidade AT
│   ├── PosPage.tsx         # PDV (Ponto de Venda)
│   ├── SalesListPage.tsx   # Histórico + reimprimir/anular
│   ├── ReconciliationPage.tsx
│   ├── ReportsPage.tsx
│   ├── AuditPage.tsx       # Hash AT
│   └── SettingsPage.tsx
├── App.tsx                 # Router
├── main.tsx                # Entry
└── index.css               # Tailwind + CSS variables
```

## Páginas

| Rota | Página | Acesso |
|---|---|---|
| `/login` | Login | público |
| `/setup` | Wizard inicial | público |
| `/` | Dashboard AT | ADMIN, GESTOR |
| `/pos` | PDV | todos |
| `/sales` | Histórico facturas | todos |
| `/reconciliation` | Reconciliação diária | ADMIN, GESTOR |
| `/reports` | Relatórios | ADMIN, GESTOR |
| `/audit` | Auditoria hashes | ADMIN |
| `/settings` | Configurações | ADMIN |

## Conformidade AT

Todas as facturas emitidas via esta UI são:
- ✅ Validadas com NUIT módulo 11
- ✅ Assinadas com SHA-256
- ✅ Hash MD5 AT (hashHash)
- ✅ Sequência controlada (hashControl)
- ✅ Modo demo isolado (não contam para relatórios)
- ✅ Detecção de duplicação (janela configurável)
- ✅ Compatíveis com SAF-T/AO

## Demonstração rápida

1. Iniciar backend: `cd java-sgv && mvn spring-boot:run`
2. Iniciar frontend: `cd frontend && npm run dev`
3. Abrir `http://localhost:5173`
4. Wizard inicial pede dados da empresa
5. Login → Dashboard → PDV → Emitir factura