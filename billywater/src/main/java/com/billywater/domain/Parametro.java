package com.billywater.domain;

import java.math.BigDecimal;

/** Parâmetros da empresa. */
public class Parametro {
    public Long id;
    public String nomeEmpresa;
    public String nuit;
    public String alvara;
    public String endereco;
    public String cidade;
    public String telefone;
    public String email;
    public String rodapeDocumental;
    public BigDecimal ivaGeral = BigDecimal.valueOf(16);
    public BigDecimal percentagemIvaAgua = BigDecimal.valueOf(75);
    public BigDecimal taxaSaneamento = BigDecimal.ZERO;
    public BigDecimal taxaReligacao = BigDecimal.ZERO;
    public Integer diaLimitePagamento = 10;
    public String serieFt = "FT";
    public String serieVd = "VD";
    public String pastaBackups = "backups";
    public String impressoraTermica;
    public Boolean impressaoTermicaAtiva = false;

    public String getNomeEmpresa() { return nomeEmpresa; }
    public String getNuit() { return nuit; }
    public String getEndereco() { return endereco; }
    public String getCidade() { return cidade; }
    public String getRodapeDocumental() { return rodapeDocumental; }
}
