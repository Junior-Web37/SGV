package com.billywater.domain;

import java.math.BigDecimal;

/** Material utilizado num trabalho. */
public class TrabalhoMaterial {
    public Long id;
    public Long trabalhoId;
    public Long produtoId;
    public BigDecimal quantidade = BigDecimal.ZERO;
    public BigDecimal preco = BigDecimal.ZERO;
    public BigDecimal subtotal = BigDecimal.ZERO;

    public Long getProdutoId() { return produtoId; }
    public BigDecimal getQuantidade() { return quantidade; }
    public BigDecimal getSubtotal() { return subtotal; }
}
