package com.sgv.desktop;

import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

public final class Formatters {

    private Formatters() {
    }

    public static void applyCurrencyFormatter(TextField field, Locale locale) {
        if (field == null) return;
        DecimalFormat format = (DecimalFormat) NumberFormat.getCurrencyInstance(locale);
        format.setParseBigDecimal(true);
        field.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.isBlank()) {
                return change;
            }
            try {
                format.parse(newText.replaceAll("[^0-9,.-]", ""));
                return change;
            } catch (ParseException ex) {
                return null;
            }
        }));
    }

    public static void applyDecimalFormatter(TextField field) {
        if (field == null) return;
        field.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.matches("^[0-9]*[.,]?[0-9]*$")) {
                return change;
            }
            return null;
        }));
    }
}
