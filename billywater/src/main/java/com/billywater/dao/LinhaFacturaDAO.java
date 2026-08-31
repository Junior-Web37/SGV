package com.billywater.dao;

import com.billywater.domain.LinhaFactura;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class LinhaFacturaDAO extends BaseDAO<LinhaFactura> {

    @Override protected String tabela() { return "linha_factura"; }

    @Override protected LinhaFactura mapear(ResultSet rs) throws SQLException {
        LinhaFactura l = new LinhaFactura();
        l.id = rs.getLong("id");
        l.facturaId = rs.getLong("factura_id");
        l.tipoLinha = rs.getString("tipo_linha");
        l.descricao = rs.getString("descricao");
        l.quantidade = rs.getBigDecimal("quantidade");
        l.precoUnitario = rs.getBigDecimal("preco_unitario");
        l.subtotal = rs.getBigDecimal("subtotal");
        l.baseIva = rs.getBigDecimal("base_iva");
        l.iva = rs.getBigDecimal("iva");
        l.taxaIva = rs.getBigDecimal("taxa_iva");
        return l;
    }

    public List<LinhaFactura> porFactura(Long facturaId) throws SQLException {
        return consultar("SELECT * FROM linha_factura WHERE factura_id = ? ORDER BY id", ps -> ps.setLong(1, facturaId));
    }

    public long inserir(LinhaFactura l) throws SQLException {
        return executarInsert("INSERT INTO linha_factura (factura_id, tipo_linha, descricao, quantidade, preco_unitario, subtotal, base_iva, iva, taxa_iva) VALUES (?,?,?,?,?,?,?,?,?)",
                ps -> { ps.setLong(1, l.facturaId); ps.setString(2, l.tipoLinha); ps.setString(3, l.descricao); ps.setBigDecimal(4, l.quantidade);
                        ps.setBigDecimal(5, l.precoUnitario); ps.setBigDecimal(6, l.subtotal); ps.setBigDecimal(7, l.baseIva); ps.setBigDecimal(8, l.iva); ps.setBigDecimal(9, l.taxaIva); });
    }
}
