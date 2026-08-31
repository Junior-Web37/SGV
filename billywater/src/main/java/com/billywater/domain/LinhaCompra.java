package com.billywater.domain;

import java.math.BigDecimal;

/** Linha de uma compra (produto + quantidade). */
public class LinhaCompra {
    public Long id;
    public Long compraId;
    public Long produtoId;
    public BigDecimal quantidade = BigDecimal.ZERO;
    public BigDecimal custoUnitario = BigDecimal.ZERO;
    public BigDecimal subtotal = BigDecimal.ZERO;

    public Long getProdutoId() { return produtoId; }
    public BigDecimal getQuantidade() { return quantidade; }
    public BigDecimal getSubtotal() { return subtotal; }
}
