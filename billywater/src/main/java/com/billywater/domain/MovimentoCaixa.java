package com.billywater.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Movimento de caixa (IN/OUT) dentro de uma sessão. */
public class MovimentoCaixa {
    public Long id;
    public Long sessaoId;
    public String tipo;          // IN, OUT
    public BigDecimal valor = BigDecimal.ZERO;
    public String origem;        // COBRANCA, VD, SANGRIA, REFORCO, DESPESA, ESTORNO
    public String referencia;
    public LocalDateTime criadoEm;
    public String operador;

    public String getTipo() { return tipo; }
    public BigDecimal getValor() { return valor; }
    public String getOrigem() { return origem; }
}
