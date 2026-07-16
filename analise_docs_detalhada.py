import sqlite3
import json

conn = sqlite3.connect(r'C:\xampp\htdocs\SGV\DOCS\sistema_vendas.db')
cursor = conn.cursor()

print("=" * 70)
print("ANÁLISE DETALHADA DO SISTEMA DOCS")
print("=" * 70)

# ===================================================================
# 1. ANÁLISE DE VENDAS
# ===================================================================
print("\n" + "=" * 70)
print("📊 1. VENDAS - Análise Detalhada")
print("=" * 70)

cursor.execute("SELECT id, numero_documento, serie, tipo_documento, cliente_nome, total, data_emissao, estado FROM vendas ORDER BY id DESC LIMIT 10")
vendas = cursor.fetchall()
print(f"\n📋 ÚLTIMAS 10 VENDAS:")
print("-" * 70)
for v in vendas:
    print(f"  #{v[0]:4d} | {v[1]:>12s} | {v[2]:>6s} | {v[3]:>8s} | {v[4][:30]:30s} | {v[5]:>10.2f} | {str(v[6])[:10]} | {v[7]}")

cursor.execute("SELECT COUNT(*), SUM(total), AVG(total) FROM vendas WHERE estado = 'FECHADA'")
total_stats = cursor.fetchall()[0]
print(f"\n📈 ESTATÍSTICAS GERAIS:")
print(f"   Total de vendas: {total_stats[0]}")
print(f"   Volume total: {total_stats[1]:,.2f} MT")
print(f"   Ticket médio: {total_stats[2]:,.2f} MT")

cursor.execute("SELECT tipo_documento, COUNT(*), SUM(total) FROM vendas GROUP BY tipo_documento")
tipos = cursor.fetchall()
print(f"\n📋 VENDAS POR TIPO DE DOCUMENTO:")
for t in tipos:
    print(f"   {t[0]}: {t[1]} vendas, {t[2]:,.2f} MT")

# ===================================================================
# 2. ANÁLISE DE ITENS DE VENDA
# ===================================================================
print("\n" + "=" * 70)
print("📦 2. ITENS DE VENDA - Análise")
print("=" * 70)

cursor.execute("""
    SELECT iv.venda_id, p.nome, iv.quantidade, iv.preco_unitario, iv.desconto, iv.subtotal
    FROM itens_venda iv
    JOIN produtos p ON iv.produto_id = p.id
    ORDER BY iv.venda_id DESC
    LIMIT 15
""")
itens = cursor.fetchall()
print(f"\n📋 PRINCIPAIS PRODUTOS VENDIDOS:")
for i in itens[:10]:
    print(f"   Venda #{i[0]:4d} | {i[1][:25]:25s} | Qtd: {i[2]:6.2f} | Preço: {i[3]:8.2f} | Desc: {i[4]:5.1f}% | Total: {i[5]:8.2f}")

# ===================================================================
# 3. ANÁLISE DE CLIENTES
# ===================================================================
print("\n" + "=" * 70)
print("👥 3. CLIENTES - Análise")
print("=" * 70)

cursor.execute("SELECT COUNT(*) FROM clientes")
total_clientes = cursor.fetchone()[0]
print(f"\n📊 Total de clientes: {total_clientes}")

cursor.execute("SELECT tipo, COUNT(*) FROM clientes GROUP BY tipo")
tipos_cli = cursor.fetchall()
print(f"\n📋 CLIENTES POR TIPO:")
for t in tipos_cli:
    print(f"   {t[0]}: {t[1]}")

cursor.execute("""
    SELECT c.nome, c.nuit, c.tipo, COUNT(v.id) as num_compras, SUM(v.total) as total_gasto
    FROM clientes c
    LEFT JOIN vendas v ON c.id = v.cliente_id AND v.estado = 'FECHADA'
    GROUP BY c.id
    ORDER BY total_gasto DESC NULLS LAST
    LIMIT 10
""")
top_clientes = cursor.fetchall()
print(f"\n🏆 TOP 10 CLIENTES (por volume):")
for c in top_clientes:
    print(f"   {c[0][:35]:35s} | NUIT: {c[1]:>12s} | {c[2]:>10s} | {c[3]:3d} compras | {c[4]:>12.2f} MT" if c[4] else f"   {c[0][:35]:35s} | Sem compras")

# ===================================================================
# 4. ANÁLISE DE PRODUTOS
# ===================================================================
print("\n" + "=" * 70)
print("🛒 4. PRODUTOS - Análise")
print("=" * 70)

cursor.execute("SELECT COUNT(*) FROM produtos")
total_produtos = cursor.fetchone()[0]
print(f"\n📊 Total de produtos: {total_produtos}")

cursor.execute("SELECT c.nome, COUNT(p.id) FROM categorias c LEFT JOIN produtos p ON c.id = p.categoria_id GROUP BY c.id")
cats = cursor.fetchall()
print(f"\n📋 PRODUTOS POR CATEGORIA:")
for c in cats:
    print(f"   {c[0]:30s}: {c[1]:3d} produtos")

cursor.execute("""
    SELECT p.nome, p.preco_venda, p.estoque_atual, SUM(iv.quantidade) as total_vendido
    FROM produtos p
    LEFT JOIN itens_venda iv ON p.id = iv.produto_id
    GROUP BY p.id
    ORDER BY total_vendido DESC NULLS LAST
    LIMIT 10
""")
top_produtos = cursor.fetchall()
print(f"\n🏆 TOP 10 PRODUTOS MAIS VENDIDOS:")
for p in top_produtos:
    print(f"   {p[0][:35]:35s} | Preço: {p[1]:8.2f} | Stock: {p[2]:6.0f} | Vendido: {p[3] or 0:6.0f}")

# ===================================================================
# 5. ANÁLISE DE ESTOQUE
# ===================================================================
print("\n" + "=" * 70)
print("📦 5. ESTOQUE - Análise")
print("=" * 70)

cursor.execute("SELECT COUNT(*) FROM produtos WHERE estoque_atual <= estoque_minimo")
produtos_alerta = cursor.fetchone()[0]
print(f"\n⚠️ PRODUTOS COM STOCK BAIXO: {produtos_alerta}")

cursor.execute("""
    SELECT p.nome, p.estoque_atual, p.estoque_minimo, p.estoque_maximo
    FROM produtos p
    WHERE p.estoque_atual <= p.estoque_minimo
    ORDER BY p.estoque_atual
    LIMIT 10
""")
alertas = cursor.fetchall()
print(f"\n🔴 PRODUTOS EM ALERTA DE STOCK:")
for a in alertas:
    print(f"   {a[0][:35]:35s} | Atual: {a[1]:6.0f} | Mín: {a[2]:5.0f} | Máx: {a[3]:5.0f}")

cursor.execute("""
    SELECT tipo, COUNT(*), SUM(quantidade)
    FROM movimentos_estoque
    GROUP BY tipo
""")
movs = cursor.fetchall()
print(f"\n📋 MOVIMENTOS DE ESTOQUE:")
for m in movs:
    print(f"   {m[0]:15s}: {m[1]:4d} movimentos, {m[2]:10.2f} unidades")

# ===================================================================
# 6. ANÁLISE DE CAIXA
# ===================================================================
print("\n" + "=" * 70)
print("💰 6. CAIXA - Análise")
print("=" * 70)

cursor.execute("SELECT COUNT(*), SUM(valor_final - valor_inicial) FROM sessoes_caixa WHERE estado = 'FECHADA'")
caixa_stats = cursor.fetchall()[0]
print(f"\n📊 Total de caixas fechados: {caixa_stats[0]}")
print(f"📈 Volume total movimentado: {caixa_stats[1] or 0:,.2f} MT")

cursor.execute("""
    SELECT sc.usuario_id, u.nome, COUNT(*), SUM(sc.valor_final - sc.valor_inicial) as total
    FROM sessoes_caixa sc
    JOIN usuarios u ON sc.usuario_id = u.id
    WHERE sc.estado = 'FECHADA'
    GROUP BY sc.usuario_id
    ORDER BY total DESC
""")
caixas_users = cursor.fetchall()
print(f"\n👤 CAIXAS POR UTILIZADOR:")
for c in caixas_users:
    print(f"   {c[1]:30s}: {c[2]:3d} caixas, {c[3]:,.2f} MT")

# ===================================================================
# 7. ANÁLISE DE CONFIGURAÇÕES
# ===================================================================
print("\n" + "=" * 70)
print("⚙️ 7. CONFIGURAÇÕES DO SISTEMA")
print("=" * 70)

cursor.execute("SELECT chave, valor FROM configuracoes")
configs = cursor.fetchall()
print(f"\n📋 CONFIGURAÇÕES ATUAIS:")
for c in configs:
    print(f"   {c[0]:30s}: {c[1][:50]}")

# ===================================================================
# 8. ESTRUTURA DE OPERAÇÕES
# ===================================================================
print("\n" + "=" * 70)
print("🔄 8. FLUXO DE OPERAÇÕES DO DOCS")
print("=" * 70)

print("""
┌─────────────────────────────────────────────────────────────────────┐
│                     FLUXO PRINCIPAL DE OPERAÇÕES                     │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  1️⃣ ABRIR CAIXA                                                     │
│     ├── Criar sessão_cxa (estado: ABERTA)                           │
│     ├── Definir valor inicial                                       │
│     └── Registar utilizador                                         │
│                                                                      │
│  2️⃣ REGISTAR VENDA                                                 │
│     ├── Selecionar/vincular cliente                                 │
│     ├── Adicionar produtos (itens_venda)                          │
│     ├── Aplicar descontos                                           │
│     ├── Calcular totais (subtotal, impostos, total)                 │
│     └── Processar pagamento (pagamentos)                           │
│                                                                      │
│  3️⃣ FECHAR CAIXA                                                   │
│     ├── Somar total de vendas                                       │
│     ├── Registrar movimentos_caixa (entradas)                        │
│     ├── Calcular diferença (sistema vs declarado)                   │
│     └── Fechar sessao_caixa (estado: FECHADA)                      │
│                                                                      │
│  4️⃣ GESTÃO DE ESTOQUE                                              │
│     ├── Entradas: compras → estoque_sucursal                       │
│     ├── Saídas: vendas → decrementa estoque                        │
│     ├── Ajustes manuais: movimentos_estoque                         │
│     └── Alertas: stock_minimo atingido                              │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
""")

# ===================================================================
# 9. RESUMO DE FUNCIONALIDADES POR MÓDULO
# ===================================================================
print("\n" + "=" * 70)
print("📋 9. FUNCIONALIDADES IMPLEMENTADAS POR MÓDULO")
print("=" * 70)

modulos = {
    "GESTÃO": [
        "✅ Cadastro de sucursais",
        "✅ Gestão de utilizadores",
        "✅ Perfis/Grupos de utilizadores",
        "✅ Permissões por perfil",
        "✅ Login/Logout com controle de sessão"
    ],
    "PRODUTOS": [
        "✅ Cadastro de produtos (32 campos)",
        "✅ Gestão de categorias",
        "✅ Unidades de medida",
        "✅ Códigos de barras múltiplos",
        "✅ Fichas técnicas (receitas)",
        "✅ Preços: compra, venda, atacado",
        "✅ Control de stock (min/max)"
    ],
    "ESTOQUE": [
        "✅ Stock por sucursal",
        "✅ Movimentos automáticos (vendas/compras)",
        "✅ Movimentos manuais",
        "✅ Alertas de stock mínimo",
        "✅ Histórico completo de movimentos"
    ],
    "VENDAS": [
        "✅ Vendas com múltiplos itens",
        "✅ Suporte a faktura/recibo",
        "✅ Descontos por item e global",
        "✅ Múltiplos pagamentos por venda",
        "✅ Sessões de caixa",
        "✅ Reimpressão de documentos",
        "✅ Anulação com NC"
    ],
    "COMPRAS": [
        "✅ Cadastro de fornecedores",
        "✅ Registo de compras",
        "✅ Itens de compra",
        "✅ Atualização automática de estoque"
    ],
    "CLIENTES": [
        "✅ Cadastro com NUIT",
        "✅ Tipos (B2B/B2C)",
        "✅ Histórico de compras",
        "✅ Movimento financeiros"
    ],
    "CAIXA": [
        "✅ Abertura/Fecho de caixa",
        "✅ Movimentos de entrada/saída",
        "✅ Controle de valores",
        "✅ Relatório de diferença"
    ],
    "AUDITORIA": [
        "✅ Log de todas operações",
        "✅ Registro de utilizador/data",
        "✅ Detalhes da operação"
    ]
}

for modulo, funcs in modulos.items():
    print(f"\n📁 {modulo}:")
    for f in funcs:
        print(f"   {f}")

conn.close()
print("\n" + "=" * 70)
print("✅ ANÁLISE CONCLUÍDA")
print("=" * 70)
