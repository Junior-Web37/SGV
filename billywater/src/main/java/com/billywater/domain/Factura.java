package com.billywater.domain;

import java.time.LocalDateTime;
import java.math.BigDecimal;

/** Factura de água (FT) ou documento associado. */
public class Factura {
    public Long id;
    public String tipoDocumento;      // FT, VD, FS, COT, REC, NC, DV, RC
    public String serie;
    public Long numero;
    public Long clienteId;
    public Long contadorId;
    public Long contratoId;
    public String periodo;            // AAAA-MM
    public BigDecimal consumo = BigDecimal.ZERO;
    public BigDecimal valorAgua = BigDecimal.ZERO;
    public BigDecimal taxaDisponibilidade = BigDecimal.ZERO;
    public BigDecimal saneamento = BigDecimal.ZERO;
    public BigDecimal subtotal = BigDecimal.ZERO;
    public BigDecimal iva = BigDecimal.ZERO;
    public BigDecimal total = BigDecimal.ZERO;
    public BigDecimal limitePagamento = BigDecimal.ZERO;
    public String estado;             // EMITIDA, PAGO, PAGO_PARCIAL, ANULADA
    public String metodoPagamento;
    public LocalDateTime criadoEm;
    public Long pagamentoId;
    public Boolean anulada = false;
    public Boolean estimada = false;

    public String getTipoDocumento() { return tipoDocumento; }
    public String getSerie() { return serie; }
    public Long getNumero() { return numero; }
    public Long getClienteId() { return clienteId; }
    public String getPeriodo() { return periodo; }
    public String getEstado() { return estado; }
    public BigDecimal getConsumo() { return consumo; }
    public BigDecimal getTotal() { return total; }
    public BigDecimal getValorAgua() { return valorAgua; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getIva() { return iva; }
    public BigDecimal getLimitePagamento() { return limitePagamento; }
}
