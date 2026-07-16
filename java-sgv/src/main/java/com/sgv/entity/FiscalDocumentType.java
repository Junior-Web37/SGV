package com.sgv.entity;

public enum FiscalDocumentType {
    FACTURA("FA", "Factura"),
    NOTA_CREDITO("NC", "Nota de Crédito"),
    NOTA_DEBITO("ND", "Nota de Débito"),
    RECIBO("RC", "Recibo"),
    TALAO("TV", "Talão");

    private final String code;
    private final String description;

    FiscalDocumentType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public boolean isCreditNote() {
        return this == NOTA_CREDITO;
    }

    public boolean isDebitNote() {
        return this == NOTA_DEBITO;
    }

    public boolean requiresOriginSale() {
        return isCreditNote() || isDebitNote();
    }

    public static FiscalDocumentType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (FiscalDocumentType type : values()) {
            if (type.code.equalsIgnoreCase(code) || type.name().equalsIgnoreCase(code)) {
                return type;
            }
        }
        return null;
    }
}
