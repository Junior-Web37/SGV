package com.billywater.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Sessão de caixa (abertura/fecho). */
public class SessaoCaixa {
    public Long id;
    public LocalDateTime abertura;
    public LocalDateTime fecho;
    public String operador;
    public BigDecimal fundoManeio = BigDecimal.ZERO;
    public BigDecimal valorEsperado = BigDecimal.ZERO;
    public BigDecimal valorContado = BigDecimal.ZERO;
    public BigDecimal diferenca = BigDecimal.ZERO;
    public String estado;             // ABERTA, FECHADA
    public String operadorFecho;
    public LocalDateTime criadaEm;

    public java.math.BigDecimal getFundoManeio() { return fundoManeio; }
    public java.math.BigDecimal getValorEsperado() { return valorEsperado; }
    public java.math.BigDecimal getValorContado() { return valorContado; }
    public java.math.BigDecimal getDiferenca() { return diferenca; }
    public String getOperador() { return operador; }
    public String getEstado() { return estado; }
}
