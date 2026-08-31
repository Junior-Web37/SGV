package com.billywater.domain;

import java.time.LocalDateTime;

/** Transferência entre locais (armazém↔loja). */
public class TransferenciaArmazem {
    public Long id;
    public String serie;
    public Long numero;
    public Long origemId;
    public Long destinoId;
    public String estado;       // COMPLETED, PENDENTE
    public String referencia;
    public LocalDateTime criadoEm;
    public String operador;

    public Long getOrigemId() { return origemId; }
    public Long getDestinoId() { return destinoId; }
    public String getEstado() { return estado; }
    public String getReferencia() { return referencia; }
}
