package com.sgv.model;

public enum SaleState {
    EMITIDA,
    PAGO,
    ANULADA,
    COTACAO_ABERTA,
    ENCOMENDA_ABERTA;

    public static SaleState fromString(String s) {
        if (s == null) return EMITIDA;
        try { return SaleState.valueOf(s.toUpperCase()); }
        catch (Exception e) { return EMITIDA; }
    }
}
