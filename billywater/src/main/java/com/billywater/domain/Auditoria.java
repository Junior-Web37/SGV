package com.billywater.domain;

import java.time.LocalDateTime;

/** Registo de auditoria/log. */
public class Auditoria {
    public Long id;
    public LocalDateTime data;
    public String utilizador;
    public String categoria;    // LOGIN, SEGURANCA, CLIENTE, FACTURACAO, COBRANCA, LOJA, STOCK, ARMAZEM, SISTEMA
    public String acao;
    public String entidade;
    public String detalhe;

    public LocalDateTime getData() { return data; }
    public String getUtilizador() { return utilizador; }
    public String getCategoria() { return categoria; }
    public String getAcao() { return acao; }
    public String getEntidade() { return entidade; }
    public String getDetalhe() { return detalhe; }
}
