package com.billywater.ui;

import java.math.BigDecimal;

/** Validações pequenas e reutilizáveis. */
public final class Validacao {
    private Validacao() {}

    public static boolean naoVazio(String s) { return s != null && !s.isBlank(); }
    public static boolean maiorQueZero(BigDecimal v) { return v != null && v.signum() > 0; }
    public static Long parseLong(String s, Long def) {
        try { return s == null || s.isBlank() ? def : Long.parseLong(s.trim()); } catch (NumberFormatException e) { return def; }
    }
}
