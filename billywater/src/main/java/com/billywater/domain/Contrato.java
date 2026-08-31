package com.billywater.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Contrato de fornecimento de água. */
public class Contrato {
    public Long id;
    public String codigo;
    public String numeroConta;
    public Long clienteId;
    public Long contadorId;
    public String tipo;                 // AGUA, LIGACAO
    public String categoria;            // DOMESTICO, COMERCIO, INDUSTRIA, FONTANARIO, PUBLICO
    public String categoriaTarifaria;
    public Long zonaId;
    public Long tarifaId;
    public Long rotaId;
    public LocalDate dataInicio;
    public LocalDate dataFim;
    public String estado;               // ACTIVO, SUSPENSO, CORTADO
    public BigDecimal limiteCredito = BigDecimal.ZERO;
    public String numeroCliente;
    public String cicloFacturacao;
    public Integer diaLeitura = 1;

    public String getNumeroConta() { return numeroConta; }
    public String getCodigo() { return codigo; }
    public String getCategoriaTarifaria() { return categoriaTarifaria; }
    public String getEstado() { return estado; }
    public Long getClienteId() { return clienteId; }
    @Override public String toString() { return numeroConta != null ? numeroConta : ""; }
}
