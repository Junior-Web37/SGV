package com.billywater.domain;

import java.math.BigDecimal;

/** Linha de uma factura (água / serviço / produto). */
public class LinhaFactura {
    public Long id;
    public Long facturaId;
    public String tipoLinha;
    public String descricao;
    public BigDecimal quantidade = BigDecimal.ZERO;
    public BigDecimal precoUnitario = BigDecimal.ZERO;
    public BigDecimal subtotal = BigDecimal.ZERO;
    public BigDecimal baseIva = BigDecimal.ZERO;
    public BigDecimal iva = BigDecimal.ZERO;
    public BigDecimal taxaIva = BigDecimal.ZERO;

    public Long getFacturaId() { return facturaId; }
    public String getDescricao() { return descricao; }
    public BigDecimal getSubtotal() { return subtotal; }
}
