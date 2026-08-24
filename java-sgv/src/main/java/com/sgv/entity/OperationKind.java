package com.sgv.entity;

public enum OperationKind {
    SALE("Venda"),
    QUOTE("Cotação"),
    REFUND("Devolução"),
    TRANSFER("Transferência"),
    OTHER("Outro");

    private final String label;

    OperationKind(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
