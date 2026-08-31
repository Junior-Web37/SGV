package com.billywater.servico;

import com.billywater.config.Licenca;
import com.billywater.ui.Ui;
import javafx.stage.Window;

import java.nio.file.Files;
import java.nio.file.Path;

/** Gestão da licença do SGF (verificação RSA-SHA256 via chave pública). */
public final class ServicoLicenca {

    private static Licenca atual;

    private ServicoLicenca() {}

    public static Licenca atual() {
        if (atual == null) {
            try { if (Files.exists(Licenca.caminhoPadrao())) atual = carregar(Licenca.caminhoPadrao()); }
            catch (Exception ignored) {}
        }
        return atual;
    }

    public static Licenca carregar(Path f) throws Exception {
        Licenca l = Licenca.carregar(f);
        if (!l.valida()) throw new IllegalArgumentException("Licença inválida/forjada/expirada.");
        atual = l;
        return l;
    }

    public static boolean autorizada() {
        Licenca l = atual();
        return l != null && l.valida();
    }

    /** Diálogo de activação (Configurações → Licença → Activar). */
    public static void activar(Window janela) {
        try {
            var file = new javafx.stage.FileChooser();
            file.setTitle("Seleccionar ficheiro de licença (.lic)");
            file.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Licença", "*.lic", "*.bin", "*.txt"));
            java.io.File f = file.showOpenDialog(janela);
            if (f == null) return;
            Licenca l = carregar(f.toPath());
            // persistir
            Files.createDirectories(Licenca.caminhoPadrao().getParent());
            Files.copy(f.toPath(), Licenca.caminhoPadrao(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            Ui.info("Licença", "Licença activada: " + l.resumo());
        } catch (Exception ex) { Ui.erro("Licença", ex.getMessage()); }
    }
}
