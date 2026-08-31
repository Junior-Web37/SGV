package com.billywater.ui;

/** Item genérico para ComboBox (id + rótulo). */
public record ComboItem(Long id, String rotulo) {
    @Override public String toString() { return rotulo; }
}
