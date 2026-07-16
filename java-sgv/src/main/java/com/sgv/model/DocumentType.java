package com.sgv.model;

public enum DocumentType {
    VENDA,
    COTACAO,
    ENCOMENDA,
    FACTURA,
    RECIBO,
    NC,
    ND;

    public static DocumentType fromString(String s) {
        if (s == null) return VENDA;
        try { return DocumentType.valueOf(s.toUpperCase()); }
        catch (Exception e) { return VENDA; }
    }
}
