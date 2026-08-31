package com.billywater.dao;

import com.billywater.domain.Trabalho;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class TrabalhoDAO extends BaseDAO<Trabalho> {

    @Override protected String tabela() { return "trabalho"; }

    @Override protected Trabalho mapear(ResultSet rs) throws SQLException {
        Trabalho t = new Trabalho();
        t.id = rs.getLong("id");
        t.codigo = rs.getString("codigo");
        t.clienteId = rs.getLong("cliente_id");
        t.descricao = rs.getString("descricao");
        t.tecnico = rs.getString("tecnico");
        t.estado = rs.getString("estado");
        t.maoObra = rs.getBigDecimal("mao_obra");
        t.materiais = rs.getBigDecimal("materiais");
        t.subtotal = rs.getBigDecimal("subtotal");
        t.iva = rs.getBigDecimal("iva");
        t.total = rs.getBigDecimal("total");
        t.criadoEm = rs.getTimestamp("criado_em") == null ? null : rs.getTimestamp("criado_em").toLocalDateTime();
        t.facturaServicoId = rs.getLong("factura_servico_id");
        return t;
    }

    public List<Trabalho> todos() throws SQLException {
        return consultar("SELECT * FROM trabalho ORDER BY criado_em DESC", null);
    }

    public long salvar(Trabalho t) throws SQLException {
        return executarInsert("INSERT INTO trabalho (codigo, cliente_id, descricao, tecnico, estado, mao_obra, materiais, subtotal, iva, total, criado_em) VALUES (?,?,?,?,?,?,?,?,?,?,NOW())",
                ps -> { ps.setString(1, t.codigo); ps.setLong(2, t.clienteId == null ? 0 : t.clienteId); ps.setString(3, t.descricao); ps.setString(4, t.tecnico);
                        ps.setString(5, t.estado); ps.setBigDecimal(6, t.maoObra); ps.setBigDecimal(7, t.materiais); ps.setBigDecimal(8, t.subtotal); ps.setBigDecimal(9, t.iva); ps.setBigDecimal(10, t.total); });
    }
}
