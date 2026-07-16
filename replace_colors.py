import os
import re

fxml_dir = r"c:\xampp\htdocs\SGV\java-sgv\src\main\resources\fxml"

# Dictionary of replacements
replacements = [
    # Replace white backgrounds with card background
    (re.compile(r'-fx-background-color:\s*white;?'), r'-fx-background-color:#1c2541;'),
    
    # Replace dark text colors with light text colors
    (re.compile(r'-fx-text-fill:\s*#1a1a1a;?'), r'-fx-text-fill:#ffffff;'),
    (re.compile(r'-fx-text-fill:\s*#34495e;?'), r'-fx-text-fill:#e0e6ed;'),
    (re.compile(r'-fx-text-fill:\s*#999999;?'), r'-fx-text-fill:#8a9cb0;'),
    (re.compile(r'-fx-text-fill:\s*#999;?'), r'-fx-text-fill:#8a9cb0;'),
    
    # Shadows: reduce opacity or make them match dark theme
    (re.compile(r'rgba\(0,0,0,0\.0[0-9]\)'), r'rgba(0,0,0,0.4)'),
    (re.compile(r'rgba\(0,0,0,0\.1\)'), r'rgba(0,0,0,0.5)'),
    
    # Borders
    (re.compile(r'-fx-border-color:\s*#e8edf2;?'), r'-fx-border-color:#2b3656;'),
    (re.compile(r'-fx-border-color:\s*#dde3ea;?'), r'-fx-border-color:#3a4a73;'),
    
    # Light gray backgrounds to dark
    (re.compile(r'-fx-background-color:\s*#f8f9fa;?'), r'-fx-background-color:#0b132b;'),
    (re.compile(r'-fx-background-color:\s*#f5f5f7;?'), r'-fx-background-color:#0b132b;'),
]

for filename in os.listdir(fxml_dir):
    if filename.endswith(".fxml") and filename != "login.fxml":
        filepath = os.path.join(fxml_dir, filename)
        with open(filepath, "r", encoding="utf-8") as f:
            content = f.read()
            
        original_content = content
        for pattern, replacement in replacements:
            content = pattern.sub(replacement, content)
            
        if content != original_content:
            with open(filepath, "w", encoding="utf-8") as f:
                f.write(content)
            print(f"Updated {filename}")

print("Done.")
