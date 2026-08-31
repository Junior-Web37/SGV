package com.billywater.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Tarifa (AURA) por categoria. */
public class Tarifa {
    public Long id;
    public String codigo;
    public String nome;
    public String categoria;
    public LocalDate vigencia;
    public BigDecimal minimoFacturavel = BigDecimal.ZERO;
    public BigDecimal taxaDisponibilidade = BigDecimal.ZERO;
    public BigDecimal taxaSaneamento = BigDecimal.ZERO;
    public BigDecimal percentagemIvaBase = BigDecimal.ZERO;
    public Boolean isentoIvaMinimo = true;
    public LocalDate vigenciaInicio;
    public LocalDate vigenciaFim;
    public Boolean ativo = true;

    public String getCodigo() { return codigo; }
    public String getNome() { return nome; }
    public String getCategoria() { return categoria; }
    public BigDecimal getMinimoFacturavel() { return minimoFacturavel; }
    public BigDecimal getTaxaDisponibilidade() { return taxaDisponibilidade; }
    public BigDecimal getTaxaSaneamento() { return taxaSaneamento; }
    public Boolean getAtivo() { return ativo; }
    @Override public String toString() { return nome != null ? nome : ""; }
}
