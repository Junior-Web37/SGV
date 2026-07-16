import pymysql

conn = pymysql.connect(host='localhost', user='root', password='', charset='utf8mb4')
cur = conn.cursor()

cur.execute('SHOW DATABASES LIKE "sgv"')
r = cur.fetchone()
print('DB exists:', r)

cur.execute('SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = "sgv"')
count = cur.fetchone()[0]
print('Tables in sgv:', count)

if count > 0:
    cur.execute('SELECT table_name FROM information_schema.tables WHERE table_schema = "sgv" ORDER BY table_name')
    tables = [row[0] for row in cur.fetchall()]
    print('Tables:', ', '.join(tables[:10]))
    if len(tables) > 10:
        print(f'  ... and {len(tables)-10} more')

conn.close()
