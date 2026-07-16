import pymysql

conn = pymysql.connect(host='localhost', user='root', password='', database='sgv', charset='utf8mb4')
cursor = conn.cursor()

# Get all tables
cursor.execute("SHOW TABLES")
tables = [row[0] for row in cursor.fetchall()]

out = []
out.append("-- SGV Baseline Schema")
out.append("-- Extracted from MariaDB sgv database\n")

for table in tables:
    cursor.execute(f"SHOW CREATE TABLE `{table}`")
    row = cursor.fetchone()
    out.append(f"\n-- Table: {table}")
    out.append(f"{row[1]};")

conn.close()
print('\n'.join(out))
