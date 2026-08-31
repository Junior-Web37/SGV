package com.billywater.dao;

import com.billywater.domain.Factura;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class FacturaDAO extends BaseDAO<Factura> {

    @Override protected String tabela() { return "factura"; }

    @Override protected Factura mapear(ResultSet rs) throws SQLException {
        Factura f = new Factura();
        f.id = rs.getLong("id");
        f.tipoDocumento = rs.getString("tipo_documento");
        f.serie = rs.getString("serie");
        f.numero = rs.getLong("numero");
        f.clienteId = rs.getLong("cliente_id");
        f.contadorId = rs.getLong("contador_id");
        f.contratoId = rs.getLong("contrato_id");
        f.periodo = rs.getString("periodo");
        f.consumo = rs.getBigDecimal("consumo");
        f.valorAgua = rs.getBigDecimal("valor_agua");
        f.taxaDisponibilidade = rs.getBigDecimal("taxa_disponibilidade");
        f.saneamento = rs.getBigDecimal("saneamento");
        f.subtotal = rs.getBigDecimal("subtotal");
        f.iva = rs.getBigDecimal("iva");
        f.total = rs.getBigDecimal("total");
        f.limitePagamento = rs.getBigDecimal("limite_pagamento");
        f.estado = rs.getString("estado");
        f.metodoPagamento = rs.getString("metodo_pagamento");
        f.criadoEm = rs.getTimestamp("criado_em") == null ? null : rs.getTimestamp("criado_em").toLocalDateTime();
        f.pagamentoId = rs.getLong("pagamento_id");
        f.anulada = rs.getBoolean("anulada");
        f.estimada = rs.getBoolean("estimada");
        return f;
    }

    public Long proximoNumero(String serie, String periodo) throws SQLException {
        try (var ps = conn().prepareStatement("SELECT COALESCE(MAX(numero),0)+1 FROM factura WHERE serie = ? AND periodo = ?")) {
            ps.setString(1, serie); ps.setString(2, periodo);
            try (var rs = ps.executeQuery()) { return rs.next() ? rs.getLong(1) : 1L; }
        }
    }

    public long inserir(Factura f) throws SQLException {
        return executarInsert("INSERT INTO factura (tipo_documento, serie, numero, cliente_id, contador_id, contrato_id, periodo, consumo, valor_agua, taxa_disponibilidade, saneamento, subtotal, iva, total, limite_pagamento, estado, metodo_pagamento, criado_em, pagamento_id, anulada, estimada) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                ps -> { ps.setString(1, f.tipoDocumento); ps.setString(2, f.serie); ps.setLong(3, f.numero == null ? 0 : f.numero); ps.setLong(4, f.clienteId == null ? 0 : f.clienteId);
                        ps.setLong(5, f.contadorId == null ? 0 : f.contadorId); ps.setLong(6, f.contratoId == null ? 0 : f.contratoId); ps.setString(7, f.periodo);
                        ps.setBigDecimal(8, f.consumo); ps.setBigDecimal(9, f.valorAgua); ps.setBigDecimal(10, f.taxaDisponibilidade); ps.setBigDecimal(11, f.saneamento);
                        ps.setBigDecimal(12, f.subtotal); ps.setBigDecimal(13, f.iva); ps.setBigDecimal(14, f.total); ps.setBigDecimal(15, f.limitePagamento);
                        ps.setString(16, f.estado); ps.setString(17, f.metodoPagamento); ps.setTimestamp(18, java.sql.Timestamp.valueOf(f.criadoEm == null ? java.time.LocalDateTime.now() : f.criadoEm));
                        ps.setLong(19, f.pagamentoId == null ? 0 : f.pagamentoId); ps.setBoolean(20, f.anulada); ps.setBoolean(21, f.estimada); });
    }

    public void actualizar(Factura f) throws SQLException {
        executar("UPDATE factura SET estado=?, metodo_pagamento=?, pagamento_id=?, anulada=?, limite_pagamento=? WHERE id=?",
                ps -> { ps.setString(1, f.estado); ps.setString(2, f.metodoPagamento); ps.setLong(3, f.pagamentoId == null ? 0 : f.pagamentoId);
                        ps.setBoolean(4, f.anulada); ps.setBigDecimal(5, f.limitePagamento); ps.setLong(6, f.id); });
    }

    public List<Factura> buscarPorCliente(Long clienteId) throws SQLException {
        return consultar("SELECT * FROM factura WHERE cliente_id = ? ORDER BY criado_em DESC", ps -> ps.setLong(1, clienteId));
    }

    public List<Factura> buscarPorPeriodo(String periodo) throws SQLException {
        return consultar("SELECT * FROM factura WHERE periodo = ? ORDER BY numero", ps -> ps.setString(1, periodo));
    }

    /** Facturas em aberto (não pagas, não anuladas) — para aging/contas correntes. */
    public List<Factura> contasEmAberto() throws SQLException {
        return consultar("SELECT * FROM factura WHERE estado <> 'PAGO' AND NOT anulada ORDER BY criado_em", null);
    }

    public void actualizarFaturaCliente(Factura f) throws SQLException {
        executar("UPDATE factura SET cliente_id=? WHERE id=?", ps -> { ps.setLong(1, f.clienteId); ps.setLong(2, f.id); });
    }
}
