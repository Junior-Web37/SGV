package com.billywater.domain;

import java.math.BigDecimal;

/** Stock de um produto num local (armazém/loja). */
public class StockItem {
    public Long id;
    public Long produtoId;
    public Long armazemId;
    public BigDecimal quantidade = BigDecimal.ZERO;
    public BigDecimal stockMinimo = BigDecimal.ZERO;
    public BigDecimal stockMaximo = BigDecimal.ZERO;
    public String estado;

    public Long getProdutoId() { return produtoId; }
    public Long getArmazemId() { return armazemId; }
    public BigDecimal getQuantidade() { return quantidade; }
    public String getEstado() { return estado; }
}
