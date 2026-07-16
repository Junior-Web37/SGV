import zipfile, xml.etree.ElementTree as ET, re, pathlib
from pathlib import Path
sources = [r'C:\xampp\htdocs\SGV\Requisitos_Sistema_Facturacao_Mocambique.docx', r'C:\xampp\htdocs\SGV\Requisitos_Sistema_Facturacao_Mocambique - Copy.docx']
out_dir = Path('DOCS')
out_dir.mkdir(exist_ok=True)
combined = []
for s in sources:
    p = Path(s)
    if not p.exists():
        combined.append({'file': str(p), 'text': ''})
        continue
    with zipfile.ZipFile(p) as z:
        xml = z.read('word/document.xml')
    root = ET.fromstring(xml)
    ns = {'w':'http://schemas.openxmlformats.org/wordprocessingml/2006/main'}
    paras = []
    for pnode in root.findall('.//w:p', ns):
        texts = [t.text for t in pnode.findall('.//w:t', ns) if t.text]
        if texts:
            paras.append(''.join(texts))
    text = '\n\n'.join(paras)
    combined.append({'file': str(p), 'text': text})

for item in combined:
    fname = out_dir / (Path(item['file']).stem + '.txt')
    try:
        fname.write_text(item['text'], encoding='utf-8')
    except Exception as e:
        print('WRITE ERROR', fname, e)

keywords = ['requisit','funcional','regra','fluxo','processo','relatório','recibo','pagamento','cliente','produto','venda']
summary_lines = []
for item in combined:
    summary_lines.append('## Source: ' + Path(item['file']).name)
    text = item['text']
    found = []
    for line in text.splitlines():
        low = line.lower()
        if any(k in low for k in keywords):
            line = re.sub('\s+',' ',line).strip()
            if line and line not in found:
                found.append(line)
    if found:
        summary_lines.append('\n'.join(['- '+l for l in found]))
    else:
        head = '\n'.join(item['text'].splitlines()[:10])
        summary_lines.append('Preview:\n'+head)
    summary_lines.append('\n')

summary = '\n'.join(summary_lines)
summary_path = out_dir / 'requirements_from_docx.md'
summary_path.write_text(summary, encoding='utf-8')
print('WROTE', summary_path)
