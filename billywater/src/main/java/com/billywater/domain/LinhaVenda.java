package com.billywater.domain;

import java.math.BigDecimal;

/** Linha de uma venda (item/material). */
public class LinhaVenda {
    public Long id;
    public Long vendaId;
    public Long produtoId;
    public BigDecimal quantidade = BigDecimal.ZERO;
    public BigDecimal preco = BigDecimal.ZERO;
    public BigDecimal subtotal = BigDecimal.ZERO;
    public BigDecimal devolvido = BigDecimal.ZERO;

    public Long getProdutoId() { return produtoId; }
    public BigDecimal getQuantidade() { return quantidade; }
    public BigDecimal getSubtotal() { return subtotal; }
}
