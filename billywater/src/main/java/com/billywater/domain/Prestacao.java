package com.billywater.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Prestação de um plano de pagamento (dívidas). */
public class Prestacao {
    public Long id;
    public Long planoId;
    public Integer numero;
    public BigDecimal valor = BigDecimal.ZERO;
    public LocalDate vencimento;
    public BigDecimal pago = BigDecimal.ZERO;
    public String estado;       // PENDENTE, PAGO
    public LocalDate dataPagamento;

    public BigDecimal getValor() { return valor; }
    public LocalDate getVencimento() { return vencimento; }
    public String getEstado() { return estado; }
    public Integer getNumero() { return numero; }
}
