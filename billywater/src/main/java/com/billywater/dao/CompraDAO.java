package com.billywater.dao;

import com.billywater.domain.Compra;
import com.billywater.domain.LinhaCompra;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class CompraDAO extends BaseDAO<Compra> {

    @Override protected String tabela() { return "compra"; }

    @Override protected Compra mapear(ResultSet rs) throws SQLException {
        Compra c = new Compra();
        c.id = rs.getLong("id");
        c.serie = rs.getString("serie");
        c.numero = rs.getLong("numero");
        c.fornecedorId = rs.getLong("fornecedor_id");
        c.armazemId = rs.getLong("armazem_id");
        c.valorTotal = rs.getBigDecimal("valor_total");
        c.referencia = rs.getString("referencia");
        c.data = rs.getObject("data_", java.time.LocalDate.class);
        c.estado = rs.getString("estado");
        c.operador = rs.getString("operador");
        return c;
    }

    public Long proximoNumero(String serie) throws SQLException {
        try (var ps = conn().prepareStatement("SELECT COALESCE(MAX(numero),0)+1 FROM compra WHERE serie = ?")) {
            ps.setString(1, serie);
            try (var rs = ps.executeQuery()) { return rs.next() ? rs.getLong(1) : 1L; }
        }
    }

    public long inserir(Compra c) throws SQLException {
        return executarInsert("INSERT INTO compra (serie, numero, fornecedor_id, armazem_id, valor_total, referencia, data_, estado, operador) VALUES (?,?,?,?,?,?,?,?,?)",
                ps -> { ps.setString(1, c.serie); ps.setLong(2, c.numero); ps.setLong(3, c.fornecedorId == null ? 0 : c.fornecedorId); ps.setLong(4, c.armazemId == null ? 0 : c.armazemId);
                        ps.setBigDecimal(5, c.valorTotal); ps.setString(6, c.referencia); ps.setObject(7, c.data == null ? java.time.LocalDate.now() : c.data); ps.setString(8, c.estado); ps.setString(9, c.operador); });
    }

    public List<Compra> todos() throws SQLException {
        return consultar("SELECT * FROM compra ORDER BY id DESC", null);
    }
}
