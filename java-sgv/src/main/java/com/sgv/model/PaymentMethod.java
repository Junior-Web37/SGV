package com.sgv.model;

public enum PaymentMethod {
    DINHEIRO,
    DEBITO,
    CREDITO,
    CHEQUE,
    TRANSFERENCIA,
    MULTICAIXA;

    public static PaymentMethod fromString(String s) {
        if (s == null) return DINHEIRO;
        try { return PaymentMethod.valueOf(s.toUpperCase()); }
        catch (Exception e) { return DINHEIRO; }
    }
}
