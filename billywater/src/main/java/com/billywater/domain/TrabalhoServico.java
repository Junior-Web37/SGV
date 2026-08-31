package com.billywater.domain;

import java.math.BigDecimal;

/** Serviço/línea de mão-de-obra num trabalho. */
public class TrabalhoServico {
    public Long id;
    public Long trabalhoId;
    public String descricao;
    public BigDecimal quantidade = BigDecimal.ZERO;
    public BigDecimal preco = BigDecimal.ZERO;
    public BigDecimal subtotal = BigDecimal.ZERO;

    public String getDescricao() { return descricao; }
    public BigDecimal getSubtotal() { return subtotal; }
}
