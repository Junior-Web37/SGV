package com.sgv.desktop;

import com.sgv.entity.Warehouse;
import com.sgv.repository.WarehouseRepository;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

@Component
public class WarehouseFormController {

    @FXML private TextField codeField;
    @FXML private TextField nameField;
    @FXML private TextField nuitField;
    @FXML private TextField addressField;
    @FXML private TextField contactField;
    @FXML private TextArea  notesArea;
    @FXML private CheckBox   activeCheck;
    @FXML private Label      errorLabel;
    @FXML private Button     saveButton;
    @FXML private Button     cancelButton;

    private final WarehouseRepository repository;
    private Warehouse editing;
    private Runnable onSaved;

    public WarehouseFormController(WarehouseRepository repository) {
        this.repository = repository;
    }

    @FXML
    public void initialize() {
        saveButton.setOnAction(e -> doSave());
        cancelButton.setOnAction(e -> doCancel());
        activeCheck.setSelected(true);
    }

    public void setWarehouse(Warehouse w) {
        this.editing = w;
        if (w != null) {
            codeField.setText(w.getCode());
            nameField.setText(w.getName());
            nuitField.setText(w.getNuit());
            addressField.setText(w.getAddress());
            contactField.setText(w.getContact());
            notesArea.setText(w.getNotes());
            activeCheck.setSelected(w.isActive());
        }
    }

    public void setOnSaved(Runnable r) { this.onSaved = r; }

    private void doSave() {
        errorLabel.setVisible(false);
        String code = codeField.getText() == null ? "" : codeField.getText().trim();
        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        if (code.isBlank() || name.isBlank()) {
            showError("Código e Nome são obrigatórios");
            return;
        }

        java.util.Optional<Warehouse> existing = repository.findByCode(code);
        if (existing.isPresent() && (editing == null || !existing.get().getId().equals(editing.getId()))) {
            showError("Já existe um armazém com este código.");
            return;
        }
        try {
            Warehouse w = (editing != null) ? editing : new Warehouse();
            w.setCode(code);
            w.setName(name);
            w.setNuit(blankToNull(nuitField.getText()));
            w.setAddress(blankToNull(addressField.getText()));
            w.setContact(blankToNull(contactField.getText()));
            w.setNotes(blankToNull(notesArea.getText()));
            w.setActive(activeCheck.isSelected());
            repository.save(w);
            if (onSaved != null) onSaved.run();
            doCancel();
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Erro: " + ex.getMessage());
        }
    }

    private void doCancel() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
