package com.sgv;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FixContrast {
    public static void main(String[] args) throws Exception {
        String[] filesToFix = {
            "sale_form.fxml",
            "purchase_form.fxml",
            "dashboard.fxml"
        };
        
        String basePath = "c:/xampp/htdocs/SGV/java-sgv/src/main/resources/fxml/";
        
        for (String fName : filesToFix) {
            Path path = Paths.get(basePath + fName);
            if (!Files.exists(path)) continue;
            
            String content = new String(Files.readAllBytes(path));
            
            // For dashboard: replace medium grey with dark grey, light grey with medium grey
            if (fName.equals("dashboard.fxml")) {
                content = content.replace("text-fill:#64748B", "text-fill:#1E293B");
                content = content.replace("text-fill:#94A3B8", "text-fill:#475569");
            } else {
                // For forms: replace only the ones that we know are on white background
                // e.g. text-fill:#64748B to #1E293B
                content = content.replace("text-fill:#64748B", "text-fill:#1E293B");
                // For the placeholder italic text
                content = content.replace("text-fill:#94A3B8; -fx-font-style:italic", "text-fill:#475569; -fx-font-style:italic");
            }
            
            Files.write(path, content.getBytes());
            System.out.println("Fixed contrast in " + fName);
        }
    }
}
