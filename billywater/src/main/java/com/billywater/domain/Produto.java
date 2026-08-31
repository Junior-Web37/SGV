package com.billywater.domain;

import java.math.BigDecimal;

/** Produto/material da loja (ou serviço). */
public class Produto {
    public Long id;
    public String codigo;
    public String codigoBarras;
    public String nome;
    public String categoria;
    public String unidade;
    public BigDecimal precoCompra = BigDecimal.ZERO;
    public BigDecimal precoVenda = BigDecimal.ZERO;
    public BigDecimal iva = BigDecimal.ZERO;
    public BigDecimal stockActual = BigDecimal.ZERO;
    public BigDecimal stockMinimo = BigDecimal.ZERO;
    public Boolean servico = false;
    public Boolean ativo = true;

    public String getNome() { return nome; }
    public String getCodigo() { return codigo; }
    public String getCodigoBarras() { return codigoBarras; }
    public String getCategoria() { return categoria; }
    public String getUnidade() { return unidade; }
    public BigDecimal getPrecoVenda() { return precoVenda; }
    public BigDecimal getStockActual() { return stockActual; }
    public BigDecimal getStockMinimo() { return stockMinimo; }
    public Boolean getAtivo() { return ativo; }
    public Long getId() { return id; }
    @Override public String toString() { return nome != null ? nome : ""; }
}
