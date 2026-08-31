package com.billywater.domain;

/** Local (armazém ou loja) que detém stock. */
public class Armazem {
    public Long id;
    public String codigo;
    public String nome;
    public String localizacao;
    public String tipo;   // ARMAZEM, LOJA
    public Boolean ativo = true;

    public String getNome() { return nome; }
    public String getCodigo() { return codigo; }
    public String getTipo() { return tipo; }
    public Boolean getAtivo() { return ativo; }
    @Override public String toString() { return nome != null ? nome : ""; }
}
