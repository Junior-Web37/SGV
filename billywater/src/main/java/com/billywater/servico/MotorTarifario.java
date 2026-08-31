package com.billywater.servico;

import com.billywater.domain.EscalaoTarifa;
import com.billywater.domain.LinhaFactura;
import com.billywater.domain.Tarifa;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

/** Motor de cálculo de escalões AURA (mínimo, disponibilidade, saneamento, IVA água). */
public final class MotorTarifario {

    private MotorTarifario() {}

    public static ResultadoFacturacao calcular(BigDecimal consumoM3, Tarifa tarifa, List<EscalaoTarifa> escaloes, BigDecimal ivaGeral) {
        ResultadoFacturacao r = new ResultadoFacturacao();
        if (tarifa == null) throw new IllegalArgumentException("Tarifa em falta para o cálculo.");
        if (ivaGeral == null) ivaGeral = BigDecimal.valueOf(16);

        BigDecimal consumo = consumoM3 == null ? BigDecimal.ZERO : consumoM3.max(BigDecimal.ZERO);
        BigDecimal minimo = tarifa.minimoFacturavel == null ? BigDecimal.ZERO : tarifa.minimoFacturavel;
        BigDecimal consumoFacturavel = consumo.compareTo(minimo) < 0 ? minimo : consumo;
        r.consumo = consumoFacturavel;
        r.consumoMinimo = minimo;

        if (escaloes == null || escaloes.isEmpty()) {
            throw new IllegalStateException("Tarifa sem escalões definidos: " + tarifa.codigo);
        }

        List<EscalaoTarifa> lista = escaloes.stream()
                .sorted(Comparator.comparing(EscalaoTarifa::getLimiteInferior))
                .toList();

        BigDecimal resto = consumoFacturavel;
        BigDecimal valorAgua = BigDecimal.ZERO;
        BigDecimal baseIva = BigDecimal.ZERO;

        for (EscalaoTarifa e : lista) {
            if (resto.signum() <= 0) break;
            BigDecimal inf = nz(e.limiteInferior);
            BigDecimal sup = e.limiteSuperior == null || e.limiteSuperior.signum() <= 0 ? null : e.limiteSuperior;
            BigDecimal faixa;
            if (sup == null) faixa = resto;
            else faixa = resto.min(sup.subtract(inf).max(BigDecimal.ZERO));
            if (faixa.signum() <= 0) continue;

            BigDecimal preco = nz(e.precoPorM3);
            BigDecimal valorLinha = faixa.multiply(preco).setScale(2, RoundingMode.HALF_UP);
            valorAgua = valorAgua.add(valorLinha);
            baseIva = baseIva.add(valorLinha);

            LinhaFactura lf = new LinhaFactura();
            lf.tipoLinha = "ESCALAO";
            lf.descricao = "Consumo " + faixa.stripTrailingZeros().toPlainString() + " m³ a " + preco.setScale(2, RoundingMode.HALF_UP) + " MT/m³";
            lf.quantidade = faixa;
            lf.precoUnitario = preco;
            lf.subtotal = valorLinha;
            lf.baseIva = valorLinha;
            lf.taxaIva = e.taxaIva == null ? ivaGeral : e.taxaIva;
            r.linhas.add(lf);
            resto = resto.subtract(faixa);
        }

        // IVA sobre a água: percentagem_iva_base da tarifa aplicada ao consumo sujeito a IVA
        BigDecimal pctIva = tarifa.percentagemIvaBase == null ? BigDecimal.ZERO : tarifa.percentagemIvaBase;
        // Quando isento de IVA no mínimo (consumo <= mínimo), a base é o que ultrapassa o mínimo
        BigDecimal baseTributavel = valorAgua;
        if (Boolean.TRUE.equals(tarifa.isentoIvaMinimo) && consumoFacturavel.compareTo(minimo) <= 0) {
            baseTributavel = BigDecimal.ZERO;
        }
        BigDecimal ivaAgua = baseTributavel.multiply(pctIva).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        r.valorAgua = valorAgua;
        r.baseIva = baseTributavel;
        r.iva = ivaAgua;

        BigDecimal taxaDisp = nz(tarifa.taxaDisponibilidade);
        BigDecimal sane = nz(tarifa.taxaSaneamento);
        r.taxaDisponibilidade = taxaDisp;
        r.saneamento = sane;

        // Linha de taxa de disponibilidade
        if (taxaDisp.signum() > 0) {
            LinhaFactura lf = new LinhaFactura();
            lf.tipoLinha = "TAXA"; lf.descricao = "Taxa de disponibilidade";
            lf.quantidade = BigDecimal.ONE; lf.precoUnitario = taxaDisp; lf.subtotal = taxaDisp;
            lf.baseIva = BigDecimal.ZERO; lf.taxaIva = BigDecimal.ZERO;
            r.linhas.add(lf);
        }
        // Linha de saneamento
        if (sane.signum() > 0) {
            LinhaFactura lf = new LinhaFactura();
            lf.tipoLinha = "TAXA"; lf.descricao = "Taxa de saneamento";
            lf.quantidade = BigDecimal.ONE; lf.precoUnitario = sane; lf.subtotal = sane;
            lf.baseIva = BigDecimal.ZERO; lf.taxaIva = BigDecimal.ZERO;
            r.linhas.add(lf);
        }

        r.subtotal = valorAgua.add(taxaDisp).add(sane);
        r.total = r.subtotal.add(ivaAgua).setScale(2, RoundingMode.HALF_UP);
        return r;
    }

    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
}
