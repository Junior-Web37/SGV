import os
import xml.etree.ElementTree as ET

fxml_dir = r"c:\xampp\htdocs\SGV\java-sgv\src\main\resources\fxml"

def check_xml(file_path):
    try:
        ET.parse(file_path)
    except ET.ParseError as e:
        print(f"Error in {file_path}:\n  {e}")

for root, _, files in os.walk(fxml_dir):
    for f in files:
        if f.endswith('.fxml'):
            check_xml(os.path.join(root, f))
