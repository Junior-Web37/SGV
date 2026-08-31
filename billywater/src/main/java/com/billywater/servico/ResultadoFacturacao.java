package com.billywater.servico;

import com.billywater.domain.LinhaFactura;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Resultado do cálculo de uma factura de água (linhas + totais). */
public class ResultadoFacturacao {
    public BigDecimal consumo = BigDecimal.ZERO;
    public BigDecimal consumoMinimo = BigDecimal.ZERO;
    public BigDecimal valorAgua = BigDecimal.ZERO;
    public BigDecimal taxaDisponibilidade = BigDecimal.ZERO;
    public BigDecimal saneamento = BigDecimal.ZERO;
    public BigDecimal subtotal = BigDecimal.ZERO;
    public BigDecimal baseIva = BigDecimal.ZERO;
    public BigDecimal iva = BigDecimal.ZERO;
    public BigDecimal total = BigDecimal.ZERO;
    public List<LinhaFactura> linhas = new ArrayList<>();

    public BigDecimal getConsumo() { return consumo; }
    public BigDecimal getValorAgua() { return valorAgua; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getIva() { return iva; }
    public BigDecimal getTotal() { return total; }
    public BigDecimal getTaxaDisponibilidade() { return taxaDisponibilidade; }
    public BigDecimal getSaneamento() { return saneamento; }
    public List<LinhaFactura> getLinhas() { return linhas; }
}
