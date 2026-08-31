package com.billywater.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Produção/captação e cálculo de NRW (Água Não Facturada). */
public class Producao {
    public Long id;
    public LocalDate data;
    public String periodo;         // AAAA-MM
    public BigDecimal volumeProduzido = BigDecimal.ZERO;
    public BigDecimal volumeFacturado = BigDecimal.ZERO;
    public BigDecimal perdas = BigDecimal.ZERO;
    public BigDecimal nrwPercent = BigDecimal.ZERO;
    public String fonte;

    public String getPeriodo() { return periodo; }
    public LocalDate getData() { return data; }
    public BigDecimal getVolumeProduzido() { return volumeProduzido; }
    public BigDecimal getVolumeFacturado() { return volumeFacturado; }
    public BigDecimal getPerdas() { return perdas; }
    public BigDecimal getNrwPercent() { return nrwPercent; }
}
