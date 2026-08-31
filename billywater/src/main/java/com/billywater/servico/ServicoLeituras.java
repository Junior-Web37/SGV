package com.billywater.servico;

import com.billywater.dao.LeituraDAO;
import com.billywater.domain.Leitura;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Pauta de leituras e críticas AURA. */
public class ServicoLeituras {

    private final LeituraDAO leituraDAO;

    public ServicoLeituras(LeituraDAO leituraDAO) { this.leituraDAO = leituraDAO; }

    /** Calcula consumo = actual - anterior e aplica critérios AURA. */
    public Leitura validar(Leitura l) {
        BigDecimal anterior = nz(l.leituraAnterior);
        BigDecimal actual = nz(l.leituraActual);
        BigDecimal consumo = actual.subtract(anterior).max(BigDecimal.ZERO);
        l.consumo = consumo;
        StringBuilder anomalia = new StringBuilder();
        if (actual.compareTo(anterior) < 0) anomalia.append("Leitura < anterior! ");
        if (consumo.signum() == 0) anomalia.append("Consumo zero. ");
        try {
            List<BigDecimal> historico = leituraDAO.consumosAnteriores(l.contadorId, 3);
            if (historico.size() >= 2) {
                BigDecimal media = historico.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(historico.size()), 2, RoundingMode.HALF_UP);
                if (media.signum() > 0 && consumo.compareTo(media.multiply(BigDecimal.valueOf(3))) > 0)
                    anomalia.append(">=3x média. ");
            }
        } catch (Exception ignored) {}
        l.anomalia = anomalia.length() == 0 ? null : anomalia.toString().trim();
        return l;
    }

    private BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
}
