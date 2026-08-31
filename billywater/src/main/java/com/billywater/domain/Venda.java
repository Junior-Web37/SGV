package com.billywater.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Venda (POS/VD) da loja. */
public class Venda {
    public Long id;
    public String serie;
    public Long numero;
    public Long clienteId;
    public Long facturaAguaId;
    public BigDecimal subtotal = BigDecimal.ZERO;
    public BigDecimal desconto = BigDecimal.ZERO;
    public BigDecimal iva = BigDecimal.ZERO;
    public BigDecimal total = BigDecimal.ZERO;
    public String formaPagamento;
    public Boolean devolvida = false;
    public Boolean anulada = false;
    public LocalDateTime criadoEm;
    public String operador;

    public String getSerie() { return serie; }
    public Long getNumero() { return numero; }
    public BigDecimal getTotal() { return total; }
    public BigDecimal getSubtotal() { return subtotal; }
    public String getFormaPagamento() { return formaPagamento; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public Boolean getDevolvida() { return devolvida; }
}
