package com.billywater.servico;

import com.billywater.dao.ContratoDAO;
import com.billywater.dao.EscalaoTarifaDAO;
import com.billywater.dao.FacturaDAO;
import com.billywater.dao.LeituraDAO;
import com.billywater.dao.LinhaFacturaDAO;
import com.billywater.dao.ParametroDAO;
import com.billywater.dao.TarifaDAO;
import com.billywater.domain.Contrato;
import com.billywater.domain.Factura;
import com.billywater.domain.Leitura;
import com.billywater.domain.LinhaFactura;
import com.billywater.domain.Parametro;
import com.billywater.domain.Tarifa;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

/** Geração de facturas de água (FT) por lote, a partir de leituras/contratos. */
public class ServicoFacturacao {

    private final FacturaDAO facturaDAO;
    private final TarifaDAO tarifaDAO;
    private final EscalaoTarifaDAO escalaDAO;
    private final LeituraDAO leituraDAO;
    private final ContratoDAO contratoDAO = new ContratoDAO();
    private final LinhaFacturaDAO linhaDAO = new LinhaFacturaDAO();

    public ServicoFacturacao(FacturaDAO facturaDAO, TarifaDAO tarifaDAO, EscalaoTarifaDAO escalaDAO) {
        this(facturaDAO, tarifaDAO, escalaDAO, new LeituraDAO());
    }

    public ServicoFacturacao(FacturaDAO facturaDAO, TarifaDAO tarifaDAO, EscalaoTarifaDAO escalaDAO, LeituraDAO leituraDAO) {
        this.facturaDAO = facturaDAO;
        this.tarifaDAO = tarifaDAO;
        this.escalaDAO = escalaDAO;
        this.leituraDAO = leituraDAO;
    }

    public ResultadoFacturacao calcularParaCliente(BigDecimal consumoM3, Long contratoId) {
        try {
            Contrato c = contratoId == null ? null : contratoDAO.buscarPorId(contratoId);
            long tarifaId = c != null && c.tarifaId != null ? c.tarifaId : tarifaDAO.buscarDefault().id;
            Tarifa tarifa = tarifaDAO.buscarPorId(tarifaId);
            var escaloes = escalaDAO.buscarPorTarifa(tarifaId);
            return MotorTarifario.calcular(consumoM3, tarifa, escaloes, getIvaGeral());
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao calcular factura: " + e.getMessage(), e);
        }
    }

    private BigDecimal getIvaGeral() {
        try {
            Parametro p = new ParametroDAO().unico();
            return p != null && p.ivaGeral != null ? p.ivaGeral : BigDecimal.valueOf(16);
        } catch (Exception ex) { return BigDecimal.valueOf(16); }
    }

    public Factura gerarFactura(Factura f, ResultadoFacturacao r) {
        f.consumo = r.consumo;
        f.valorAgua = r.valorAgua;
        f.taxaDisponibilidade = r.taxaDisponibilidade;
        f.saneamento = r.saneamento;
        f.subtotal = r.subtotal;
        f.iva = r.iva;
        f.total = r.total;
        f.limitePagamento = r.total;
        f.estado = "EMITIDA";
        if (f.numero == null) f.numero = facturaDAO.proximoNumero(f.serie, f.periodo);
        f.id = facturaDAO.inserir(f);
        try {
            for (LinhaFactura lf : r.linhas) { lf.facturaId = f.id; linhaDAO.inserir(lf); }
        } catch (SQLException ignored) {}
        return f;
    }

    /** Emite o lote FT de um período a partir das leituras validadas ainda não facturadas. */
    public int emitirLote(String periodo) {
        try {
            List<Leitura> leituras = leituraDAO.pendentesFacturar(periodo);
            if (leituras.isEmpty()) return 0;
            BigDecimal ivaGeral = getIvaGeral();
            int n = 0;
            for (Leitura l : leituras) {
                if (l.contratoId == null) continue;
                Contrato c = contratoDAO.buscarPorId(l.contratoId);
                if (c == null) continue;
                Tarifa tarifa = c.tarifaId != null ? tarifaDAO.buscarPorId(c.tarifaId) : null;
                if (tarifa == null) tarifa = tarifaDAO.buscarPorCategoria(c.categoriaTarifaria);
                if (tarifa == null) tarifa = tarifaDAO.buscarDefault();
                var escaloes = escalaDAO.buscarPorTarifa(tarifa.id);
                BigDecimal consumo = l.consumo == null ? BigDecimal.ZERO : l.consumo;
                ResultadoFacturacao r;
                try { r = MotorTarifario.calcular(consumo, tarifa, escaloes, ivaGeral); }
                catch (Exception ex) { continue; }
                Factura f = new Factura();
                f.tipoDocumento = "FT"; f.serie = "FT";
                f.clienteId = l.clienteId; f.contadorId = l.contadorId; f.contratoId = l.contratoId;
                f.periodo = periodo; f.anulada = false; f.criadoEm = java.time.LocalDateTime.now();
                gerarFactura(f, r);
                l.facturada = true;
                leituraDAO.actualizar(l);
                n++;
            }
            return n;
        } catch (SQLException e) {
            throw new IllegalStateException("Falha ao emitir lote FT: " + e.getMessage(), e);
        }
    }

    /** Anula uma FT (sem apagar). */
    public void anular(Factura f, String motivo) {
        f.anulada = true;
        f.estado = "ANULADA";
        try { facturaDAO.actualizar(f); } catch (SQLException ignored) {}
    }
}
