import re

path = r"C:\xampp\htdocs\SGV\java-sgv\src\main\java\com\sgv\repository\ProductRepository.java"

with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# Fix: p.category -> p.category.name, and remove the extra closing paren
# The broken line has: "%')))") — should be: "%'))")
content = content.replace(" LIKE LOWER(CONCAT('%', :category, '%')))", " LIKE LOWER(CONCAT('%', :category, '%'))")
# Also fix p.category to p.category.name
content = content.replace("LOWER(p.category) LIKE", "LOWER(p.category.name) LIKE")

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

print("Fixed. Content now:")
with open(path, 'r', encoding='utf-8') as f:
    print(f.read())
