package com.sgv.desktop;

import com.sgv.entity.Warehouse;
import com.sgv.repository.WarehouseRepository;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class WarehouseFormController extends BaseFormController {

    private static final Logger log = LoggerFactory.getLogger(WarehouseFormController.class);

    @FXML private TextField codeField;
    @FXML private TextField nameField;
    @FXML private TextField nuitField;
    @FXML private TextField addressField;
    @FXML private TextField contactField;
    @FXML private TextArea  notesArea;
    @FXML private CheckBox   activeCheck;

    private final WarehouseRepository repository;
    private Warehouse editing;
    private Runnable onSaved;

    public WarehouseFormController(WarehouseRepository repository) {
        this.repository = repository;
    }

    @FXML
    public void initialize() {
        initCommonFields();
        UiUtils.attachSafe(saveButton, this::doSave, null, "WAREHOUSE_SAVE");
        UiUtils.attachSafe(cancelButton, this::doCancel, null, "WAREHOUSE_CANCEL");
        activeCheck.setSelected(true);
        codeField.textProperty().addListener((obs, o, n) -> validateRealTime());
        nameField.textProperty().addListener((obs, o, n) -> validateRealTime());
        javafx.application.Platform.runLater(this::validateRealTime);
    }

    @Override
    protected void validateRealTime() {
        String code = codeField.getText() == null ? "" : codeField.getText().trim();
        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        formValidProperty.set(!code.isBlank() && !name.isBlank());
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

    @Override
    protected void doSave() {
        if (checkTrainingBlock()) return;
        if (!formValidProperty.get()) return;
        errorLabel.setVisible(false);

        String code = codeField.getText() == null ? "" : codeField.getText().trim();
        String name = nameField.getText() == null ? "" : nameField.getText().trim();
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
            log.error("Erro ao salvar armazém", ex);
            showError("Erro: " + ex.getMessage());
        }
    }

    private String blankToNull(String s) { return (s == null || s.isBlank()) ? null : s.trim(); }
}
