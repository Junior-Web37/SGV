package com.billywater.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Contador/medidor de água. */
public class Contador {
    public Long id;
    public String numero;
    public String numeroSerie;
    public String marca;
    public String modelo;
    public String calibre;
    public BigDecimal leituraInstalacao = BigDecimal.ZERO;
    public BigDecimal leituraInicial = BigDecimal.ZERO;
    public LocalDate dataInstalacao;
    public LocalDate dataUltimaLeitura;
    public BigDecimal ultimoConsumo = BigDecimal.ZERO;
    public BigDecimal ultimaLeitura = BigDecimal.ZERO;
    public String estado;           // ACTIVO, AVARIADO, SUBSTITUIDO
    public Long clienteId;
    public Long contratoId;
    public Long rotaId;
    public Long zonaId;
    public Integer multiplicador = 1;
    public String localizacao;

    public String getNumero() { return numero; }
    public String getNumeroSerie() { return numeroSerie; }
    public String getMarca() { return marca; }
    public String getEstado() { return estado; }
    public BigDecimal getUltimaLeitura() { return ultimaLeitura; }
    @Override public String toString() { return numero != null ? numero : ""; }
}
