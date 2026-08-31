package com.billywater.dao;

import com.billywater.domain.LinhaCompra;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class LinhaCompraDAO extends BaseDAO<LinhaCompra> {

    @Override protected String tabela() { return "linha_compra"; }

    @Override protected LinhaCompra mapear(ResultSet rs) throws SQLException {
        LinhaCompra l = new LinhaCompra();
        l.id = rs.getLong("id");
        l.compraId = rs.getLong("compra_id");
        l.produtoId = rs.getLong("produto_id");
        l.quantidade = rs.getBigDecimal("quantidade");
        l.custoUnitario = rs.getBigDecimal("custo_unitario");
        l.subtotal = rs.getBigDecimal("subtotal");
        return l;
    }

    public List<LinhaCompra> porCompra(Long compraId) throws SQLException {
        return consultar("SELECT * FROM linha_compra WHERE compra_id = ? ORDER BY id", ps -> ps.setLong(1, compraId));
    }

    public long inserir(LinhaCompra l) throws SQLException {
        return executarInsert("INSERT INTO linha_compra (compra_id, produto_id, quantidade, custo_unitario, subtotal) VALUES (?,?,?,?,?)",
                ps -> { ps.setLong(1, l.compraId); ps.setLong(2, l.produtoId); ps.setBigDecimal(3, l.quantidade); ps.setBigDecimal(4, l.custoUnitario); ps.setBigDecimal(5, l.subtotal); });
    }
}
