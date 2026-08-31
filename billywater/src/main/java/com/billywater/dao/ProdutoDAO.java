package com.billywater.dao;

import com.billywater.domain.Produto;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class ProdutoDAO extends BaseDAO<Produto> {

    @Override protected String tabela() { return "produto"; }

    @Override protected Produto mapear(ResultSet rs) throws SQLException {
        Produto p = new Produto();
        p.id = rs.getLong("id");
        p.codigo = rs.getString("codigo");
        p.codigoBarras = rs.getString("codigo_barras");
        p.nome = rs.getString("nome");
        p.categoria = rs.getString("categoria");
        p.unidade = rs.getString("unidade");
        p.precoCompra = rs.getBigDecimal("preco_compra");
        p.precoVenda = rs.getBigDecimal("preco_venda");
        p.iva = rs.getBigDecimal("iva");
        p.stockActual = rs.getBigDecimal("stock_actual");
        p.stockMinimo = rs.getBigDecimal("stock_minimo");
        p.servico = rs.getBoolean("servico");
        p.ativo = rs.getBoolean("ativo");
        return p;
    }

    public List<Produto> todos() throws SQLException {
        return consultar("SELECT * FROM produto WHERE ativo = TRUE ORDER BY nome", null);
    }

    public List<Produto> pesquisar(String termo) throws SQLException {
        return consultar("SELECT * FROM produto WHERE (nome LIKE ? OR codigo LIKE ? OR codigo_barras LIKE ?) AND ativo = TRUE ORDER BY nome", ps -> {
            String t = "%" + termo + "%"; ps.setString(1, t); ps.setString(2, t); ps.setString(3, t);
        });
    }

    public Produto buscarPorCodigoBarras(String cb) throws SQLException {
        return consultarUm("SELECT * FROM produto WHERE codigo_barras = ?", ps -> ps.setString(1, cb));
    }

    public long salvar(Produto p) throws SQLException {
        if (p.id == null) {
            return executarInsert("INSERT INTO produto (codigo, codigo_barras, nome, categoria, unidade, preco_compra, preco_venda, iva, stock_actual, stock_minimo, servico, ativo) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                    ps -> { ps.setString(1, p.codigo); ps.setString(2, p.codigoBarras); ps.setString(3, p.nome); ps.setString(4, p.categoria); ps.setString(5, p.unidade);
                            ps.setBigDecimal(6, p.precoCompra); ps.setBigDecimal(7, p.precoVenda); ps.setBigDecimal(8, p.iva); ps.setBigDecimal(9, p.stockActual);
                            ps.setBigDecimal(10, p.stockMinimo); ps.setBoolean(11, p.servico); ps.setBoolean(12, p.ativo); });
        } else {
            executar("UPDATE produto SET nome=?, categoria=?, preco_compra=?, preco_venda=?, stock_minimo=?, servico=?, ativo=? WHERE id=?",
                    ps -> { ps.setString(1, p.nome); ps.setString(2, p.categoria); ps.setBigDecimal(3, p.precoCompra); ps.setBigDecimal(4, p.precoVenda);
                            ps.setBigDecimal(5, p.stockMinimo); ps.setBoolean(6, p.servico); ps.setBoolean(7, p.ativo); ps.setLong(8, p.id); });
            return p.id;
        }
    }
}
