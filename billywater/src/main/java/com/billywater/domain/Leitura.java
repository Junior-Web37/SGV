package com.billywater.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Leitura de contador num período. */
public class Leitura {
    public Long id;
    public Long contadorId;
    public Long contratoId;
    public Long clienteId;
    public Long rotaId;
    public String periodo;            // AAAA-MM
    public BigDecimal leituraAnterior = BigDecimal.ZERO;
    public BigDecimal leituraActual = BigDecimal.ZERO;
    public BigDecimal consumo = BigDecimal.ZERO;
    public String tipo;               // MEDIDA, ESTIMADA
    public String estado;             // RASCUNHO, SUBMETIDA, VALIDADA
    public Boolean estimada = false;
    public String anomalia;
    public LocalDate dataLeitura;
    public String leitor;
    public Boolean facturada = false;
    public LocalDateTime criadoEm;

    public BigDecimal getConsumo() { return consumo; }
    public BigDecimal getLeituraAnterior() { return leituraAnterior; }
    public BigDecimal getLeituraActual() { return leituraActual; }
    public String getTipo() { return tipo; }
    public String getEstado() { return estado; }
    public String getAnomalia() { return anomalia; }
    public String getPeriodo() { return periodo; }
    public Boolean getFacturada() { return facturada; }
}
