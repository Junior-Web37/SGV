import os, re, glob

fxml_dir = r"c:\xampp\htdocs\SGV\java-sgv\src\main\resources\fxml"

for file in glob.glob(os.path.join(fxml_dir, "*.fxml")):
    with open(file, 'r', encoding='utf-8') as f:
        content = f.read()
    
    modified = False
    
    # Find StackPane root and remove prefWidth/prefHeight
    if "<StackPane" in content:
        new_content = re.sub(r'(<StackPane[^>]*?)(\s+prefWidth="\d+")', r'\1', content)
        if new_content != content:
            content = new_content
            modified = True
            
        new_content = re.sub(r'(<StackPane[^>]*?)(\s+prefHeight="\d+")', r'\1', content)
        if new_content != content:
            content = new_content
            modified = True
            
    # Also strip prefWidth from any main VBox/HBox containers that might cause overflow
    # For example: prefWidth="800" or prefWidth="900"
    for size in ["700", "750", "800", "860", "900", "1000"]:
        pattern = r'\s+prefWidth="' + size + r'"'
        if re.search(pattern, content):
            content = re.sub(pattern, '', content)
            modified = True
            
        pattern = r'\s+maxWidth="' + size + r'"'
        if re.search(pattern, content):
            # Replace fixed maxWidth with a responsive one or remove it
            # Actually, removing it makes it fill the screen, which is fine for forms if they are inside a StackPane with padding
            content = re.sub(pattern, r' maxWidth="Infinity" styleClass="card-pane"', content)
            modified = True

    if modified:
        with open(file, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"Updated {os.path.basename(file)}")

print("Done.")
