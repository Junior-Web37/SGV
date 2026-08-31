package com.billywater.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Cliente (consumidor de água). */
public class Cliente {
    public Long id;
    public String codigo;
    public String nomeCompleto;
    public String nuit;
    public String nif;
    public String email;
    public String telefone;
    public String endereco;
    public String cidade;
    public String bairro;
    public String quarteirao;
    public String casa;
    public String tipo;             // SINGULAR, COOPERATIVA, EMPRESA...
    public Boolean ativo = true;
    public BigDecimal limiteCredito = BigDecimal.ZERO;
    public BigDecimal saldo = BigDecimal.ZERO;
    public Long contratoId;
    public Long rotaId;
    public Long contadorId;
    public String categoriaTarifaria;
    public LocalDateTime criadoEm;

    public String getNomeCompleto() { return nomeCompleto; }
    public String getCodigo() { return codigo; }
    public String getTelefone() { return telefone; }
    public String getCidade() { return cidade; }
    public String getBairro() { return bairro; }
    public Boolean getAtivo() { return ativo; }
    public Long getId() { return id; }
    @Override public String toString() { return nomeCompleto != null ? nomeCompleto : ""; }
}
