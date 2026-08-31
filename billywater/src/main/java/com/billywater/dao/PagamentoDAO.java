package com.billywater.dao;

import com.billywater.domain.Pagamento;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PagamentoDAO extends BaseDAO<Pagamento> {

    @Override protected String tabela() { return "pagamento"; }

    @Override protected Pagamento mapear(ResultSet rs) throws SQLException {
        Pagamento p = new Pagamento();
        p.id = rs.getLong("id");
        p.facturaId = rs.getLong("factura_id");
        p.clienteId = rs.getLong("cliente_id");
        p.metodo = rs.getString("metodo");
        p.valor = rs.getBigDecimal("valor");
        p.valorRecebido = rs.getBigDecimal("valor_recebido");
        p.troco = rs.getBigDecimal("troco");
        p.recNumero = rs.getLong("rec_numero");
        p.operador = rs.getString("operador");
        p.sessaoCaixaId = rs.getLong("sessao_caixa_id");
        p.criadoEm = rs.getTimestamp("criado_em") == null ? null : rs.getTimestamp("criado_em").toLocalDateTime();
        p.estornado = rs.getObject("estornado") != null && rs.getBoolean("estornado");
        return p;
    }

    public Long proximoRecNumero() throws SQLException {
        try (var ps = conn().prepareStatement("SELECT COALESCE(MAX(rec_numero),0)+1 FROM pagamento")) {
            try (var rs = ps.executeQuery()) { return rs.next() ? rs.getLong(1) : 1L; }
        }
    }

    public long inserir(Pagamento p) throws SQLException {
        return executarInsert("INSERT INTO pagamento (factura_id, cliente_id, metodo, valor, valor_recebido, troco, rec_numero, operador, sessao_caixa_id, criado_em, estornado) VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                ps -> { ps.setLong(1, p.facturaId == null ? 0 : p.facturaId); ps.setLong(2, p.clienteId == null ? 0 : p.clienteId); ps.setString(3, p.metodo);
                        ps.setBigDecimal(4, p.valor); ps.setBigDecimal(5, p.valorRecebido); ps.setBigDecimal(6, p.troco); ps.setLong(7, p.recNumero == null ? 0 : p.recNumero);
                        ps.setString(8, p.operador); ps.setLong(9, p.sessaoCaixaId == null ? 0 : p.sessaoCaixaId);
                        ps.setTimestamp(10, java.sql.Timestamp.valueOf(p.criadoEm == null ? java.time.LocalDateTime.now() : p.criadoEm));
                        ps.setBoolean(11, p.estornado != null && p.estornado); });
    }

    public void marcarEstornado(Long id) throws SQLException {
        executar("UPDATE pagamento SET estornado = TRUE WHERE id = ?", ps -> ps.setLong(1, id));
    }

    public Map<String, java.math.BigDecimal> resumoPorMetodo(Long sessaoId) throws SQLException {
        Map<String, java.math.BigDecimal> out = new LinkedHashMap<>();
        try (var ps = conn().prepareStatement("SELECT metodo, SUM(valor) AS total FROM pagamento WHERE sessao_caixa_id = ? AND (estornado IS NULL OR estornado = FALSE) GROUP BY metodo")) {
            ps.setLong(1, sessaoId == null ? 0 : sessaoId);
            try (var rs = ps.executeQuery()) { while (rs.next()) out.put(rs.getString("metodo"), rs.getBigDecimal("total")); }
        }
        return out;
    }

    public java.math.BigDecimal totalPorClientePeriodo(Long clienteId, String periodo) throws SQLException {
        try (var ps = conn().prepareStatement("SELECT SUM(valor) FROM pagamento WHERE cliente_id = ? AND criado_em BETWEEN ? AND ?")) {
            ps.setLong(1, clienteId); ps.setString(2, periodo + "-01 00:00:00"); ps.setString(3, periodo + "-31 23:59:59");
            try (var rs = ps.executeQuery()) { return rs.next() ? rs.getBigDecimal(1) : java.math.BigDecimal.ZERO; }
        }
    }

    public java.math.BigDecimal totalPeriodo(String periodo) throws SQLException {
        try (var ps = conn().prepareStatement("SELECT COALESCE(SUM(valor),0) FROM pagamento WHERE criado_em BETWEEN ? AND ?")) {
            ps.setString(1, periodo + "-01 00:00:00"); ps.setString(2, periodo + "-31 23:59:59");
            try (var rs = ps.executeQuery()) { return rs.next() ? rs.getBigDecimal(1) : java.math.BigDecimal.ZERO; }
        }
    }

    public List<Pagamento> porSessao(Long sessaoId) throws SQLException {
        return consultar("SELECT * FROM pagamento WHERE sessao_caixa_id = ? ORDER BY criado_em", ps -> ps.setLong(1, sessaoId));
    }
}
