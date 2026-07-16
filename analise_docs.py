import sqlite3
conn = sqlite3.connect(r'C:\xampp\htdocs\SGV\DOCS\sistema_vendas.db')
cursor = conn.cursor()

cursor.execute("SELECT name FROM sqlite_master WHERE type='table'")
tables = [t[0] for t in cursor.fetchall()]

print("=" * 60)
print("ANÁLISE COMPARATIVA: DOCS vs SGV")
print("=" * 60)

print("\n📦 TABELAS DO DOCS (30 tabelas - sistema completo):")
print("-" * 60)

categorias = {
    "Gestão": ["sucursais", "usuarios", "grupos_usuarios", "role_permissions"],
    "Produtos": ["categorias", "produtos", "unidades_medida", "produtos_codigos_barras", "fichas_tecnicas"],
    "Estoque": ["estoque_sucursal", "movimentos_estoque", "movimentos_stock"],
    "Vendas": ["vendas", "itens_venda", "pagamentos", "sessoes_caixa", "movimentos_caixa"],
    "Compras": ["compras", "itens_compra"],
    "Clientes": ["clientes", "movimentos_cliente"],
    "Produção": ["producao_realidades", "ordens_producao", "notificacoes_producao", "mesas"],
    "Fornecedores": ["fornecedores"],
    "Despesas": ["despesas"],
    "Config": ["configuracoes", "sys_custom_scripts"],
    "Auditoria": ["audit_logs"]
}

for cat, tabelas in categorias.items():
    print(f"\n  📁 {cat}:")
    for t in tabelas:
        if t in tables:
            cursor.execute(f"PRAGMA table_info({t})")
            cols = cursor.fetchall()
            print(f"     ├── {t} ({len(cols)} colunas)")
        else:
            print(f"     ├── {t} ❌")

print("\n" + "=" * 60)
print("COMPARAÇÃO COM SGV (java-sgv)")
print("=" * 60)

print("""
┌─────────────────────────────────────────────────────────────┐
│                    DOCS (Supermercado)                      │
│  30 tabelas - Sistema COMPLEXO                             │
│  • Mesas/Ordens (restauração)                              │
│  • Fichas técnicas/Produção (manufactura)                 │
│  • Múltiplas sessões caixa                                 │
│  • Stock por sucursal avançado                             │
│  • Scripts customizáveis                                   │
└─────────────────────────────────────────────────────────────┘
                              ↓ Simplificado
┌─────────────────────────────────────────────────────────────┐
│                    SGV (PMEs)                               │
│  ~15 tabelas - Sistema SIMPLES                            │
│  • Gestão básica de vendas                                  │
│  • Documentos fiscais                                      │
│  • Stock simples por filial                                │
│  • Clientes e fornecedores                                  │
└─────────────────────────────────────────────────────────────┘
""")

# Detalhar algumas tabelas importantes
print("\n📋 ESTRUTURA DAS TABELAS PRINCIPAIS DO DOCS:")
print("-" * 60)

tabelas_detalhe = ["vendas", "produtos", "clientes", "movimentos_estoque", "sessoes_caixa"]
for t in tabelas_detalhe:
    cursor.execute(f"PRAGMA table_info({t})")
    cols = cursor.fetchall()
    print(f"\n  {t.upper()}:")
    for c in cols[:6]:  # Primeiras 6 colunas
        print(f"    • {c[1]} ({c[2]})")
    if len(cols) > 6:
        print(f"    ... e mais {len(cols)-6} colunas")
