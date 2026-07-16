import os
import glob

replacements = {
    '#0B1120': '#ffffff',
    '#0b1120': '#ffffff',
    '#162032': '#2563EB',
    '#1c2541': '#ffffff',
    '#e8edf5': '#1E293B',
    '#8a9cb0': '#64748B',
    '#0f1e30': '#F8FAFC',
    '#2b3a52': '#E2E8F0',
    '#3d1a1a': '#FEF2F2', # error bg
    '#ff6b6b': '#EF4444', # error text
}

# Apply to all _form.fxml files except sale_form.fxml
target_files = glob.glob('src/main/resources/fxml/*_form.fxml')

for file_path in target_files:
    if 'sale_form.fxml' in file_path:
        continue
    
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original_content = content
    
    for old, new in replacements.items():
        content = content.replace(old, new)
        
    if content != original_content:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"Updated {file_path}")
