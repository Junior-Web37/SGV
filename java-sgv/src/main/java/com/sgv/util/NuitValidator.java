package com.sgv.util;

/**
 * Validador de NUIT (Número Único de Identificação Tributária) de Moçambique.
 *
 * Algoritmo oficial (módulo 11, pesos descendentes 9-2):
 * - 9 dígitos (ou 9 dígitos + 1 letra para formato mais recente)
 * - Primeiros 8 dígitos × pesos [9,8,7,6,5,4,3,2]
 * - Soma módulo 11 → resto 0 → digito 0; resto 1 → inválido; senão 11-resto
 * - NUIT 999999999 é "Consumidor Final" (sempre válido)
 */
public final class NuitValidator {

    private NuitValidator() {}

    public static boolean isWellFormed(String nuit) {
        if (nuit == null) return false;
        String n = nuit.trim().toUpperCase();
        return n.matches("\\d{9}[A-Z]?");
    }

    public static boolean isValid(String nuit) {
        if (!isWellFormed(nuit)) return false;
        String n = nuit.trim().toUpperCase();
        if (n.startsWith("999999999")) return true;
        if (n.length() == 10) return true;
        int[] weights = {9, 8, 7, 6, 5, 4, 3, 2};
        int sum = 0;
        for (int i = 0; i < 8; i++) {
            sum += Character.getNumericValue(n.charAt(i)) * weights[i];
        }
        int mod = sum % 11;
        if (mod == 0) return Character.getNumericValue(n.charAt(8)) == 0;
        if (mod == 1) return false;
        int checkDigit = 11 - mod;
        return checkDigit == Character.getNumericValue(n.charAt(8));
    }

    public static String mask(String nuit) {
        if (nuit == null || nuit.length() < 9) return nuit;
        return nuit.substring(0, 3) + " " + nuit.substring(3, 6) + " " + nuit.substring(6);
    }
}