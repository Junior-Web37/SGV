package com.billywater.domain;

import java.math.BigDecimal;

/** Escalão de uma tarifa (limite inferior/superior de consumo e preço/m³). */
public class EscalaoTarifa {
    public Long id;
    public Long tarifaId;
    public BigDecimal limiteInferior = BigDecimal.ZERO;
    public BigDecimal limiteSuperior = BigDecimal.ZERO;
    public BigDecimal precoPorM3 = BigDecimal.ZERO;
    public BigDecimal taxaIva = BigDecimal.ZERO;

    public BigDecimal getLimiteInferior() { return limiteInferior; }
    public BigDecimal getLimiteSuperior() { return limiteSuperior; }
    public BigDecimal getPrecoPorM3() { return precoPorM3; }
    public BigDecimal getTaxaIva() { return taxaIva; }
}
