import sqlite3
from pathlib import Path

path = Path('docs') / 'sistema_vendas.db'
conn = sqlite3.connect(path)
c = conn.cursor()
print('TABLES:')
c.execute("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name")
for row in c.fetchall():
    print(row[0])
print('\nSCHEMA:')
c.execute("SELECT sql FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name")
for row in c.fetchall():
    print(row[0])
conn.close()
