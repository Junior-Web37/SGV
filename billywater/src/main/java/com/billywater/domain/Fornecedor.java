package com.billywater.domain;

import java.math.BigDecimal;

/** Fornecedor de materiais. */
public class Fornecedor {
    public Long id;
    public String codigo;
    public String nome;
    public String nuit;
    public String telefone;
    public String email;
    public String endereco;
    public String cidade;
    public Boolean ativo = true;
    public BigDecimal saldo = BigDecimal.ZERO;

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getCodigo() { return codigo; }
    public String getNuit() { return nuit; }
    public String getTelefone() { return telefone; }
    public String getCidade() { return cidade; }
    public BigDecimal getSaldo() { return saldo; }
    public Boolean getAtivo() { return ativo; }
    @Override public String toString() { return nome != null ? nome : ""; }
}
