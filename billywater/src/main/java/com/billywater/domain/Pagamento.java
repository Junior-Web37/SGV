package com.billywater.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Pagamento (REC) — cobrança de uma factura. */
public class Pagamento {
    public Long id;
    public Long sessaoCaixaId;
    public Long clienteId;
    public Long facturaId;
    public Long recNumero;
    public String metodo;                 // DINHEIRO, MPESA, EMOLA, MKESH, POS, BANCO
    public BigDecimal valor = BigDecimal.ZERO;
    public String referencia;
    public BigDecimal valorRecebido = BigDecimal.ZERO;
    public BigDecimal troco = BigDecimal.ZERO;
    public LocalDateTime criadoEm;
    public String operador;
    public Boolean estornado = false;

    public BigDecimal getValor() { return valor; }
    public String getMetodo() { return metodo; }
    public Long getRecNumero() { return recNumero; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public BigDecimal getValorRecebido() { return valorRecebido; }
    public BigDecimal getTroco() { return troco; }
    public Long getClienteId() { return clienteId; }
    public Boolean getEstornado() { return estornado; }
}
