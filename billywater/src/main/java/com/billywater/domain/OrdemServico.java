package com.billywater.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Ordem de serviço (corte, religação, deslocação). */
public class OrdemServico {
    public Long id;
    public String tipo;         // CORTE, RELIGACAO, DESLOCACAO, REPARACAO
    public Long clienteId;
    public Long contadorId;
    public String estado;
    public LocalDate criadaEm;
    public String tecnico;
    public BigDecimal taxaCobrada = BigDecimal.ZERO;
    public String motivo;

    public String getTipo() { return tipo; }
    public String getEstado() { return estado; }
    public Long getClienteId() { return clienteId; }
    public String getMotivo() { return motivo; }
}
