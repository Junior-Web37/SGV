package com.billywater.dao;

import com.billywater.domain.MovimentoStock;

import java.sql.ResultSet;
import java.sql.SQLException;

public class MovimentoStockDAO extends BaseDAO<MovimentoStock> {

    @Override protected String tabela() { return "movimento_stock"; }

    @Override protected MovimentoStock mapear(ResultSet rs) throws SQLException {
        MovimentoStock m = new MovimentoStock();
        m.id = rs.getLong("id");
        m.produtoId = rs.getLong("produto_id");
        m.armazemId = rs.getLong("armazem_id");
        m.tipo = rs.getString("tipo");
        m.quantidade = rs.getBigDecimal("quantidade");
        m.stockAntes = rs.getBigDecimal("stock_antes");
        m.stockDepois = rs.getBigDecimal("stock_depois");
        m.referencia = rs.getString("referencia");
        m.criadoEm = rs.getTimestamp("criado_em") == null ? null : rs.getTimestamp("criado_em").toLocalDateTime();
        m.operador = rs.getString("operador");
        return m;
    }

    public long inserir(MovimentoStock m) throws SQLException {
        return executarInsert("INSERT INTO movimento_stock (produto_id, armazem_id, tipo, quantidade, stock_antes, stock_depois, referencia, criado_em, operador) VALUES (?,?,?,?,?,?,?,?,?)",
                ps -> { ps.setLong(1, m.produtoId); ps.setLong(2, m.armazemId); ps.setString(3, m.tipo); ps.setBigDecimal(4, m.quantidade);
                        ps.setBigDecimal(5, m.stockAntes); ps.setBigDecimal(6, m.stockDepois); ps.setString(7, m.referencia);
                        ps.setTimestamp(8, java.sql.Timestamp.valueOf(m.criadoEm == null ? java.time.LocalDateTime.now() : m.criadoEm)); ps.setString(9, m.operador); });
    }
}
