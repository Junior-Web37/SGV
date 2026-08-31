package com.billywater.dao;

import com.billywater.domain.MovimentoCaixa;

import java.sql.ResultSet;
import java.sql.SQLException;

public class MovimentoCaixaDAO extends BaseDAO<MovimentoCaixa> {

    @Override protected String tabela() { return "movimento_caixa"; }

    @Override protected MovimentoCaixa mapear(ResultSet rs) throws SQLException {
        MovimentoCaixa m = new MovimentoCaixa();
        m.id = rs.getLong("id");
        m.sessaoId = rs.getLong("sessao_id");
        m.tipo = rs.getString("tipo");
        m.valor = rs.getBigDecimal("valor");
        m.origem = rs.getString("origem");
        m.referencia = rs.getString("referencia");
        m.criadoEm = rs.getTimestamp("criado_em") == null ? null : rs.getTimestamp("criado_em").toLocalDateTime();
        m.operador = rs.getString("operador");
        return m;
    }

    public long inserir(MovimentoCaixa m) throws SQLException {
        return executarInsert("INSERT INTO movimento_caixa (sessao_id, tipo, valor, origem, referencia, criado_em, operador) VALUES (?,?,?,?,?,?,?)",
                ps -> { ps.setLong(1, m.sessaoId == null ? 0 : m.sessaoId); ps.setString(2, m.tipo); ps.setBigDecimal(3, m.valor); ps.setString(4, m.origem);
                        ps.setString(5, m.referencia); ps.setTimestamp(6, java.sql.Timestamp.valueOf(m.criadoEm == null ? java.time.LocalDateTime.now() : m.criadoEm)); ps.setString(7, m.operador); });
    }

    public java.math.BigDecimal somarPorSessaoETipo(Long sessaoId, String tipo) throws SQLException {
        try (var ps = conn().prepareStatement("SELECT COALESCE(SUM(valor),0) FROM movimento_caixa WHERE sessao_id = ? AND tipo = ?")) {
            ps.setLong(1, sessaoId == null ? 0 : sessaoId); ps.setString(2, tipo);
            try (var rs = ps.executeQuery()) { return rs.next() ? rs.getBigDecimal(1) : java.math.BigDecimal.ZERO; }
        }
    }
}
