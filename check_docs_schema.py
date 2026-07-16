import sqlite3
conn = sqlite3.connect(r'C:\xampp\htdocs\SGV\DOCS\sistema_vendas.db')
cursor = conn.cursor()
cursor.execute("SELECT name FROM sqlite_master WHERE type='table'")
tables = cursor.fetchall()
print("=== TABELAS DO DOCS ===")
for t in tables:
    print(f"  - {t[0]}")
print(f"\nTotal: {len(tables)} tabelas")
