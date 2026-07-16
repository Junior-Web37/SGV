import pymysql

conn = pymysql.connect(host='localhost', user='root', password='', charset='utf8mb4')
cur = conn.cursor()

cur.execute('DESCRIBE sgv.products')
print('products columns:')
for row in cur.fetchall():
    print(f'  {row[0]:30s} {row[1]:30s} Null:{row[2]} Default:{row[4]}')

conn.close()
