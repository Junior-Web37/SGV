package com.sgv.desktop;

public class SupplierPaymentRow {
    private final String data, fornecedor, compra, valor, metodo, referencia;
    public SupplierPaymentRow(String data, String fornecedor, String compra, String valor, String metodo, String referencia) {
        this.data = data; this.fornecedor = fornecedor; this.compra = compra;
        this.valor = valor; this.metodo = metodo; this.referencia = referencia;
    }
    public String getData()       { return data; }
    public String getFornecedor() { return fornecedor; }
    public String getCompra()     { return compra; }
    public String getValor()      { return valor; }
    public String getMetodo()     { return metodo; }
    public String getReferencia() { return referencia; }
}
