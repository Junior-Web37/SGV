package com.sgv.model;

public enum PaymentMethod {
    DINHEIRO("Numerário / Dinheiro", "NU"),
    MPESA("M-Pesa (Vodacom)", "MB"),
    EMOLA("e-Mola (Movitel)", "MB"),
    MKESH("mKesh (Tmcel)", "MB"),
    POS("Cartão / POS Bancário", "CD"),
    DEBITO("Cartão de Débito", "CD"),
    TRANSFERENCIA("Transferência Bancária", "TB"),
    CREDITO("Crédito / Conta Corrente", "CR"),
    CHEQUE("Cheque Bancário", "CH"),
    MULTICAIXA("Multicaixa", "CD");

    private final String description;
    private final String saftCode;

    PaymentMethod(String description, String saftCode) {
        this.description = description;
        this.saftCode = saftCode;
    }

    public String getDescription() {
        return description;
    }

    public String getSaftCode() {
        return saftCode;
    }

    public static PaymentMethod fromString(String s) {
        if (s == null || s.isBlank()) return DINHEIRO;
        String clean = s.trim().toUpperCase()
                .replace("-", "")
                .replace(" ", "")
                .replace("Á", "A")
                .replace("Ã", "A")
                .replace("É", "E");
        if (clean.contains("MPESA")) return MPESA;
        if (clean.contains("EMOLA")) return EMOLA;
        if (clean.contains("MKESH")) return MKESH;
        if (clean.contains("POS") || clean.contains("CARTAO")) return POS;
        if (clean.contains("NUMERARIO") || clean.contains("DINHEIRO") || clean.contains("CASH")) return DINHEIRO;
        if (clean.contains("TRANSF")) return TRANSFERENCIA;
        if (clean.contains("CREDIT")) return CREDITO;
        if (clean.contains("CHEQUE")) return CHEQUE;
        if (clean.contains("DEBIT")) return DEBITO;
        if (clean.contains("MULTI")) return MULTICAIXA;

        try {
            return PaymentMethod.valueOf(clean);
        } catch (Exception e) {
            return DINHEIRO;
        }
    }
}

