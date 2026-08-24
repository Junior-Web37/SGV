package com.sgv.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Catálogo de tipos de imposto conforme SAF-T/AT Moçambique.
 */
public final class TaxTypes {

    private TaxTypes() {}

    private static final Map<String, String> TYPES = new LinkedHashMap<>();
    static {
        TYPES.put("IVA", "Imposto sobre o Valor Acrescentado (regime geral)");
        TYPES.put("IS", "Imposto sobre o Selo");
        TYPES.put("IES", "Imposto Especial sobre o Jogo");
        TYPES.put("NS", "Não sujeito a imposto");
        TYPES.put("IEX", "Imposto sobre veículos / circulação");
        TYPES.put("IEC", "Imposto Especial de Consumo (combustíveis, álcool, tabaco)");
        TYPES.put("IRPS", "Imposto sobre Rendimento das Pessoas Singulares");
        TYPES.put("IRPC", "Imposto sobre Rendimento das Pessoas Colectivas");
    }

    public static Map<String, String> all() { return TYPES; }

    public static boolean isValid(String code) { return code != null && TYPES.containsKey(code); }

    public static String description(String code) {
        if (code == null) return "";
        return TYPES.getOrDefault(code, "");
    }

    public static String defaultType() { return "IVA"; }
}