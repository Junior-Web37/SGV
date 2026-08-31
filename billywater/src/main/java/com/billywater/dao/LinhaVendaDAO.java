package com.billywater.dao;

import com.billywater.domain.LinhaVenda;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class LinhaVendaDAO extends BaseDAO<LinhaVenda> {

    @Override protected String tabela() { return "linha_venda"; }

    @Override protected LinhaVenda mapear(ResultSet rs) throws SQLException {
        LinhaVenda l = new LinhaVenda();
        l.id = rs.getLong("id");
        l.vendaId = rs.getLong("venda_id");
        l.produtoId = rs.getLong("produto_id");
        l.quantidade = rs.getBigDecimal("quantidade");
        l.preco = rs.getBigDecimal("preco");
        l.subtotal = rs.getBigDecimal("subtotal");
        l.devolvido = rs.getBigDecimal("devolvido");
        return l;
    }

    public List<LinhaVenda> porVenda(Long vendaId) throws SQLException {
        return consultar("SELECT * FROM linha_venda WHERE venda_id = ? ORDER BY id", ps -> ps.setLong(1, vendaId));
    }

    public long inserir(LinhaVenda l) throws SQLException {
        return executarInsert("INSERT INTO linha_venda (venda_id, produto_id, quantidade, preco, subtotal, devolvido) VALUES (?,?,?,?,?,?)",
                ps -> { ps.setLong(1, l.vendaId); ps.setLong(2, l.produtoId); ps.setBigDecimal(3, l.quantidade); ps.setBigDecimal(4, l.preco); ps.setBigDecimal(5, l.subtotal); ps.setBigDecimal(6, l.devolvido); });
    }
}
