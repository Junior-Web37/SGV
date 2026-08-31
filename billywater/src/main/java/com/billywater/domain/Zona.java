package com.billywater.domain;

import java.math.BigDecimal;

/** Zona / rota de distribuição. */
public class Zona {
    public Long id;
    public String codigo;
    public String nome;
    public String municipio;
    public String bairro;
    public String rota;
    public String descricao;
    public Boolean ativo = true;
    public Boolean ativa = true;
    public Integer totalClientes = 0;
    public BigDecimal mediaHistorica = BigDecimal.ZERO;

    public String getNome() { return nome; }
    public String getCodigo() { return codigo; }
    public String getRota() { return rota; }
    public Boolean getAtivo() { return ativo; }
    @Override public String toString() { return nome != null ? nome : ""; }
}
