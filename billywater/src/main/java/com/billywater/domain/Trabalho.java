package com.billywater.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Trabalho/Ordem de serviço (instalação com factura de serviços FS). */
public class Trabalho {
    public Long id;
    public String codigo;
    public Long clienteId;
    public String descricao;
    public String tecnico;
    public String estado;
    public BigDecimal maoObra = BigDecimal.ZERO;
    public BigDecimal materiais = BigDecimal.ZERO;
    public BigDecimal subtotal = BigDecimal.ZERO;
    public BigDecimal iva = BigDecimal.ZERO;
    public BigDecimal total = BigDecimal.ZERO;
    public LocalDateTime criadoEm;
    public Long facturaServicoId;

    public String getCodigo() { return codigo; }
    public String getDescricao() { return descricao; }
    public String getEstado() { return estado; }
    public BigDecimal getTotal() { return total; }
}
