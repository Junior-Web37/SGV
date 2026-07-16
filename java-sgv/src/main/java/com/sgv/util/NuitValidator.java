package com.sgv.util;

/**
 * Validador de NUIT (Número Único de Identificação Tributária) de Moçambique.
 *
 * NUIT válido: 9 dígitos + 1 letra (ex: 123456789A) ou 9 dígitos (alguns).
 * O algoritmo público verifica os primeiros 8 dígitos + dígito de controlo.
 *
 * Para o nosso caso, validamos:
 * - Comprimento: 9 dígitos ou 9 dígitos + 1 letra
 * - NUIT 999999999 é reservado para "Consumidor Final"
 * - Para dígitos: aplicar algoritmo módulo 11 simplificado (pesos 2-9)
 */
public final class NuitValidator {

    private NuitValidator() {}

    /** Regex básica — exactamente 9 dígitos ou 9 dígitos + 1 letra maiúscula. */
    public static boolean isWellFormed(String nuit) {
        if (nuit == null) return false;
        String n = nuit.trim().toUpperCase();
        return n.matches("\\d{9}[A-Z]?");
    }

    /** Verifica o dígito de controlo módulo 11 (apenas NUITs de 9 dígitos). */
    public static boolean isValid(String nuit) {
        if (!isWellFormed(nuit)) return false;
        String n = nuit.trim().toUpperCase();
        // NUIT 999999999 é o consumidor final — sempre válido.
        if (n.startsWith("999999999")) return true;
        // Se tem letra no fim (formato novo), confiamos no regex (sem algoritmo público oficial publicado).
        if (n.length() == 10) return true;
        // Módulo 11 simplificado sobre os primeiros 8 dígitos.
        int[] weights = {2, 3, 4, 5, 6, 7, 8, 9};
        int sum = 0;
        for (int i = 0; i < 8; i++) {
            sum += Character.getNumericValue(n.charAt(i)) * weights[i];
        }
        int mod = sum % 11;
        int checkDigit = mod < 2 ? 0 : 11 - mod;
        int provided = Character.getNumericValue(n.charAt(8));
        return checkDigit == provided;
    }

    /** Máscara visual: 123 456 789 */
    public static String mask(String nuit) {
        if (nuit == null || nuit.length() < 9) return nuit;
        return nuit.substring(0, 3) + " " + nuit.substring(3, 6) + " " + nuit.substring(6);
    }
}