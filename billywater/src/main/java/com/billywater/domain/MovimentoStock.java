package com.billywater.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Movimento de stock (kardex) num local. */
public class MovimentoStock {
    public Long id;
    public Long produtoId;
    public Long armazemId;
    public String tipo;              // ENTRADA_ARMAZEM, ENTRADA_LOJA, SAIDA_LOJA, TRANSFERENCIA_SAIDA, TRANSFERENCIA_ENTRADA
    public BigDecimal quantidade = BigDecimal.ZERO;
    public BigDecimal stockAntes = BigDecimal.ZERO;
    public BigDecimal stockDepois = BigDecimal.ZERO;
    public String referencia;
    public LocalDateTime criadoEm;
    public String operador;

    public String getTipo() { return tipo; }
    public BigDecimal getQuantidade() { return quantidade; }
    public BigDecimal getStockAntes() { return stockAntes; }
    public BigDecimal getStockDepois() { return stockDepois; }
    public String getReferencia() { return referencia; }
}
