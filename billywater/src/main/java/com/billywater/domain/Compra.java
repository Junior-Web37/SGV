package com.billywater.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Guia de compra/entrada de materiais. */
public class Compra {
    public Long id;
    public String serie;           // GC
    public Long numero;
    public Long fornecedorId;
    public Long armazemId;
    public BigDecimal valorTotal = BigDecimal.ZERO;
    public String referencia;
    public LocalDate data;
    public String estado;          // ABERTA, CONCLUIDA, CANCELADA
    public String operador;

    public String getSerie() { return serie; }
    public Long getNumero() { return numero; }
    public Long getFornecedorId() { return fornecedorId; }
    public Long getArmazemId() { return armazemId; }
    public BigDecimal getValorTotal() { return valorTotal; }
    public LocalDate getData() { return data; }
    public String getEstado() { return estado; }
}
