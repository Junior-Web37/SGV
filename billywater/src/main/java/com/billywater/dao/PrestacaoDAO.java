package com.billywater.dao;

import com.billywater.domain.Prestacao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class PrestacaoDAO extends BaseDAO<Prestacao> {

    @Override protected String tabela() { return "prestacao"; }

    @Override protected Prestacao mapear(ResultSet rs) throws SQLException {
        Prestacao p = new Prestacao();
        p.id = rs.getLong("id");
        p.planoId = rs.getLong("plano_id");
        p.numero = rs.getInt("numero");
        p.valor = rs.getBigDecimal("valor");
        p.vencimento = rs.getObject("vencimento", java.time.LocalDate.class);
        p.pago = rs.getBigDecimal("pago");
        p.estado = rs.getString("estado");
        p.dataPagamento = rs.getObject("data_pagamento", java.time.LocalDate.class);
        return p;
    }

    public List<Prestacao> porPlano(Long planoId) throws SQLException {
        return consultar("SELECT * FROM prestacao WHERE plano_id = ? ORDER BY numero", ps -> ps.setLong(1, planoId));
    }

    public long salvar(Prestacao p) throws SQLException {
        return executarInsert("INSERT INTO prestacao (plano_id, numero, valor, vencimento, pago, estado, data_pagamento) VALUES (?,?,?,?,?,?,?)",
                ps -> { ps.setLong(1, p.planoId); ps.setInt(2, p.numero); ps.setBigDecimal(3, p.valor); ps.setObject(4, p.vencimento);
                        ps.setBigDecimal(5, p.pago); ps.setString(6, p.estado); ps.setObject(7, p.dataPagamento); });
    }
}
