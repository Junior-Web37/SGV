package com.sgv.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Catálogo oficial AT de motivos de inexistência de imposto.
 * Documento de referência: Decreto 7/2024 (Facturação Electrónica).
 */
public final class TaxExemptionReasons {

    private TaxExemptionReasons() {}

    private static final Map<String, String> REASONS = new LinkedHashMap<>();
    static {
        REASONS.put("M01", "Artigo 10º do CIVA — Isenção subjectiva");
        REASONS.put("M02", "Artigo 9º do CIVA — Isenção objectiva");
        REASONS.put("M03", "Isento por diploma legal específico");
        REASONS.put("M04", "Isento por tratado internacional");
        REASONS.put("M05", "Outras isenções (legislação avulsa)");
        REASONS.put("M06", "Não sujeito / não tributado (regime especial)");
        REASONS.put("M07", "Inversão do sujeito passivo (autoliquidação)");
        REASONS.put("M08", "Operação isenta por regime transitório");
        REASONS.put("M09", "Isento por regra de funcionamento / Isenção genérica");
        REASONS.put("M10", "Factura de fornecedor residente fora do país");
        REASONS.put("M11", "Imposto apurado por estimativa");
        REASONS.put("M12", "Auto-facturação (cliente emite)");
        REASONS.put("M13", "Operação sem retenção na fonte (isenção específica)");
        REASONS.put("M14", "Factura simplificada — sem direito à dedução");
        REASONS.put("M15", "Regularização de imposto (nota de crédito/débito)");
        REASONS.put("M16", "Operação de mercado regulado");
        REASONS.put("M17", "Operação de leasing isenta");
        REASONS.put("M18", "Transmissão gratuita isenta");
        REASONS.put("M19", "Outros (especificar na descrição)");
    }

    public static Map<String, String> all() { return REASONS; }

    public static boolean isValid(String code) { return code != null && REASONS.containsKey(code); }

    public static String description(String code) {
        if (code == null) return "";
        return REASONS.getOrDefault(code, "");
    }
}