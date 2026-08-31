package com.billywater.dao;

import com.billywater.domain.StockItem;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class StockDAO extends BaseDAO<StockItem> {

    @Override protected String tabela() { return "stock_item"; }

    @Override protected StockItem mapear(ResultSet rs) throws SQLException {
        StockItem s = new StockItem();
        s.id = rs.getLong("id");
        s.produtoId = rs.getLong("produto_id");
        s.armazemId = rs.getLong("armazem_id");
        s.quantidade = rs.getBigDecimal("quantidade");
        s.stockMinimo = rs.getBigDecimal("stock_minimo");
        s.stockMaximo = rs.getBigDecimal("stock_maximo");
        s.estado = rs.getString("estado");
        return s;
    }

    public StockItem buscarPorProdutoELocal(Long produtoId, Long localId) throws SQLException {
        return consultarUm("SELECT * FROM stock_item WHERE produto_id = ? AND armazem_id = ?", ps -> { ps.setLong(1, produtoId); ps.setLong(2, localId); });
    }

    public long salvar(StockItem s) throws SQLException {
        if (s.id == null) {
            return executarInsert("INSERT INTO stock_item (produto_id, armazem_id, quantidade, stock_minimo, stock_maximo, estado) VALUES (?,?,?,?,?,?)",
                    ps -> { ps.setLong(1, s.produtoId); ps.setLong(2, s.armazemId); ps.setBigDecimal(3, s.quantidade); ps.setBigDecimal(4, s.stockMinimo); ps.setBigDecimal(5, s.stockMaximo); ps.setString(6, s.estado); });
        } else {
            executar("UPDATE stock_item SET quantidade=?, stock_minimo=?, estado=? WHERE id=?", ps -> { ps.setBigDecimal(1, s.quantidade); ps.setBigDecimal(2, s.stockMinimo); ps.setString(3, s.estado); ps.setLong(4, s.id); });
            return s.id;
        }
    }

    public List<Object[]> listarPorLocal(Long localId) throws SQLException {
        return consultarObjectos("SELECT p.nome AS nome, s.quantidade AS quantidade, s.stock_minimo AS minimo FROM stock_item s JOIN produto p ON p.id = s.produto_id WHERE s.armazem_id = ? ORDER BY p.nome", ps -> ps.setLong(1, localId));
    }

    public List<Object[]> porTransferir(Long armazemId, Long lojaId) throws SQLException {
        return consultarObjectos("SELECT p.nome AS nome, sa.quantidade AS no_armazem, COALESCE(sl.quantidade,0) AS na_loja " +
                "FROM stock_item sa JOIN produto p ON p.id = sa.produto_id LEFT JOIN stock_item sl ON sl.produto_id = sa.produto_id AND sl.armazem_id = ? " +
                "WHERE sa.armazem_id = ? AND COALESCE(sl.quantidade,0) < sa.quantidade ORDER BY p.nome",
                ps -> { ps.setLong(1, lojaId); ps.setLong(2, armazemId); });
    }

    private List<Object[]> consultarObjectos(String sql, BaseDAO.SqlBinder binder) throws SQLException {
        List<Object[]> out = new java.util.ArrayList<>();
        try (var ps = conn().prepareStatement(sql)) {
            binder.bind(ps);
            try (var rs = ps.executeQuery()) {
                int n = rs.getMetaData().getColumnCount();
                while (rs.next()) { Object[] row = new Object[n]; for (int i = 1; i <= n; i++) row[i-1] = rs.getObject(i); out.add(row); }
            }
        }
        return out;
    }
}
