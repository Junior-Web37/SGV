#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Script para consolidar análise de DOCS em ficheiro DOCX
Lê markdown files e cria documento Word estruturado
"""

import os
import sys
from pathlib import Path

try:
    from docx import Document
    from docx.shared import Pt, RGBColor, Inches
    from docx.enum.text import WD_ALIGN_PARAGRAPH
    from docx.enum.style import WD_STYLE_TYPE
except ImportError:
    print("Instalando python-docx...")
    os.system("pip install python-docx -q")
    from docx import Document
    from docx.shared import Pt, RGBColor, Inches
    from docx.enum.text import WD_ALIGN_PARAGRAPH
    from docx.enum.style import WD_STYLE_TYPE

# Paths
DOCS_DIR = Path(__file__).parent.parent / "DOCS"
OUTPUT_FILE = DOCS_DIR / "SGV_Consolidacao_DOCS.docx"

# Files to read
FILES_TO_CONSOLIDATE = [
    ("Requisitos Extraídos", DOCS_DIR / "requirements_from_docx.md"),
    ("Mapeamento Requisitos-Código", DOCS_DIR / "SGV_requirements_and_mapping.md"),
    ("Levantamento de Funcionalidades", DOCS_DIR / "SGV_funcionalidades_implementadas.md"),
]

def read_markdown(filepath):
    """Lê ficheiro markdown e retorna conteúdo."""
    if not filepath.exists():
        print(f"⚠️ Ficheiro não encontrado: {filepath}")
        return None
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            return f.read()
    except Exception as e:
        print(f"❌ Erro ao ler {filepath}: {e}")
        return None

def create_docx(content_dict):
    """Cria documento DOCX com conteúdo consolidado."""
    doc = Document()
    
    # Titulo
    title = doc.add_heading("SGV — Consolidação de Requisitos e Funcionalidades", 0)
    title.alignment = WD_ALIGN_PARAGRAPH.CENTER
    
    # Subtitle
    subtitle = doc.add_paragraph("Sistema de Gestão de Vendas — Análise Completa")
    subtitle.alignment = WD_ALIGN_PARAGRAPH.CENTER
    subtitle_format = subtitle.runs[0]
    subtitle_format.font.size = Pt(12)
    subtitle_format.font.italic = True
    
    # Data e status
    metadata = doc.add_paragraph()
    metadata.add_run("Data: ").bold = True
    metadata.add_run("2026-06-10\n")
    metadata.add_run("Status: ").bold = True
    metadata.add_run("Levantamento Completo de Funcionalidades e Requisitos\n")
    metadata.add_run("Fonte: ").bold = True
    metadata.add_run("Análise automática de ficheiros DOCS em Markdown")
    
    doc.add_paragraph()  # Espaço
    
    # Índice (Table of Contents seria melhor, mas manualmente simples)
    toc_title = doc.add_heading("Índice", 2)
    for section_title, _ in content_dict:
        doc.add_paragraph(f"• {section_title}", style='List Bullet')
    
    doc.add_page_break()
    
    # Secções de conteúdo
    for section_title, filepath in content_dict:
        content = read_markdown(filepath)
        if not content:
            doc.add_heading(section_title, 1)
            doc.add_paragraph("❌ Ficheiro não disponível")
            continue
        
        # Título da secção
        section_heading = doc.add_heading(section_title, 1)
        
        # Processar conteúdo markdown
        lines = content.split('\n')
        i = 0
        while i < len(lines):
            line = lines[i].rstrip()
            
            # Skip empty lines
            if not line.strip():
                i += 1
                continue
            
            # Headings
            if line.startswith('# '):
                doc.add_heading(line[2:], 1)
            elif line.startswith('## '):
                doc.add_heading(line[3:], 2)
            elif line.startswith('### '):
                doc.add_heading(line[4:], 3)
            elif line.startswith('#### '):
                doc.add_heading(line[5:], 4)
            
            # Bold and italic
            elif line.startswith('**') or line.startswith('__'):
                # Skip special markers
                if line.startswith('**---**'):
                    doc.add_paragraph()
                else:
                    p = doc.add_paragraph(line)
                    if p.runs:
                        p.runs[0].bold = True
            
            # Lists
            elif line.startswith('- '):
                doc.add_paragraph(line[2:], style='List Bullet')
            elif line.startswith('  - '):
                doc.add_paragraph(line[4:], style='List Bullet 2')
            elif line.startswith('| '):
                # Skip table markers (simple tables not fully supported)
                p = doc.add_paragraph(line)
                p.paragraph_format.left_indent = Inches(0.25)
            
            # Code blocks
            elif line.startswith('```'):
                # Collecte todo o bloco de código
                code_lines = []
                i += 1
                while i < len(lines) and not lines[i].startswith('```'):
                    code_lines.append(lines[i])
                    i += 1
                if code_lines:
                    code_block = '\n'.join(code_lines)
                    p = doc.add_paragraph(code_block, style='Intense Quote')
                    for run in p.runs:
                        run.font.name = 'Courier New'
                        run.font.size = Pt(9)
            
            # Normal paragraphs
            else:
                p = doc.add_paragraph(line)
            
            i += 1
        
        doc.add_page_break()
    
    # Rodapé informativo
    doc.add_paragraph()
    footer = doc.add_paragraph()
    footer_text = footer.add_run(
        "Este documento foi gerado automaticamente pela consolidação "
        "de ficheiros Markdown extraídos de análise do código-fonte SGV. "
        "Para actualizações e correções, consulte os ficheiros originais em DOCS/."
    )
    footer_text.font.size = Pt(9)
    footer_text.font.italic = True
    footer_text.font.color.rgb = RGBColor(128, 128, 128)
    
    return doc

def main():
    """Função principal."""
    print("📄 Consolidando DOCS em ficheiro DOCX...")
    
    # Verificar que os ficheiros existem
    missing = []
    for title, filepath in FILES_TO_CONSOLIDATE:
        if not filepath.exists():
            missing.append(str(filepath))
    
    if missing:
        print(f"⚠️ Ficheiros em falta: {missing}")
        print("Continuando com os que estão disponíveis...")
    
    # Criar documento
    doc = create_docx(FILES_TO_CONSOLIDATE)
    
    # Guardar
    try:
        doc.save(OUTPUT_FILE)
        print(f"✅ Sucesso! Ficheiro guardado em:")
        print(f"   {OUTPUT_FILE}")
        print(f"   Tamanho: {OUTPUT_FILE.stat().st_size / 1024:.1f} KB")
        return 0
    except Exception as e:
        print(f"❌ Erro ao guardar: {e}")
        return 1

if __name__ == "__main__":
    sys.exit(main())
