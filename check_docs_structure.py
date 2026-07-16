import sqlite3
conn = sqlite3.connect(r'C:\xampp\htdocs\SGV\DOCS\sistema_vendas.db')
cursor = conn.cursor()

# Verificar estrutura de todas as tabelas principais
tabelas = ["vendas", "itens_venda", "clientes", "produtos", "sessoes_caixa", "configuracoes"]

print("=" * 70)
print("ESTRUTURA DAS TABELAS PRINCIPAIS DO DOCS")
print("=" * 70)

for tabela in tabelas:
    print(f"\n📋 {tabela.upper()}:")
    cursor.execute(f"PRAGMA table_info({tabela})")
    cols = cursor.fetchall()
    for c in cols:
        print(f"   • {c[1]:30s} ({c[2]})")

# Verificar dados existentes
print("\n" + "=" * 70)
print("DADOS EXISTENTES")
print("=" * 70)

for tabela in tabelas:
    cursor.execute(f"SELECT COUNT(*) FROM {tabela}")
    count = cursor.fetchone()[0]
    print(f"   {tabela}: {count} registos")

# Ver um exemplo de venda
print("\n" + "=" * 70)
print("EXEMPLO DE REGISTO - VENDAS")
print("=" * 70)
cursor.execute("SELECT * FROM vendas LIMIT 1")
venda = cursor.fetchone()
cursor.execute("PRAGMA table_info(vendas)")
cols = [c[1] for c in cursor.fetchall()]
print("\nPrimeira venda:")
for i, col in enumerate(cols):
    print(f"   {col}: {venda[i]}")

conn.close()
