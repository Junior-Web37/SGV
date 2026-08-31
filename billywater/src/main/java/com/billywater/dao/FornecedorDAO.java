package com.billywater.dao;

import com.billywater.domain.Fornecedor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class FornecedorDAO extends BaseDAO<Fornecedor> {

    @Override protected String tabela() { return "fornecedor"; }

    @Override protected Fornecedor mapear(ResultSet rs) throws SQLException {
        Fornecedor f = new Fornecedor();
        f.id = rs.getLong("id");
        f.codigo = rs.getString("codigo");
        f.nome = rs.getString("nome");
        f.nuit = rs.getString("nuit");
        f.telefone = rs.getString("telefone");
        f.email = rs.getString("email");
        f.endereco = rs.getString("endereco");
        f.cidade = rs.getString("cidade");
        f.ativo = rs.getBoolean("ativo");
        f.saldo = rs.getBigDecimal("saldo");
        return f;
    }

    public List<Fornecedor> pesquisar(String termo) throws SQLException {
        return consultar("SELECT * FROM fornecedor WHERE (nome LIKE ? OR codigo LIKE ?) ORDER BY nome", ps -> {
            String t = "%" + termo + "%"; ps.setString(1, t); ps.setString(2, t);
        });
    }

    public long salvar(Fornecedor f) throws SQLException {
        if (f.id == null) {
            return executarInsert("INSERT INTO fornecedor (codigo, nome, nuit, telefone, email, endereco, cidade, ativo, saldo) VALUES (?,?,?,?,?,?,?,?,?)",
                    ps -> { ps.setString(1, f.codigo); ps.setString(2, f.nome); ps.setString(3, f.nuit); ps.setString(4, f.telefone);
                            ps.setString(5, f.email); ps.setString(6, f.endereco); ps.setString(7, f.cidade); ps.setBoolean(8, f.ativo); ps.setBigDecimal(9, f.saldo); });
        } else {
            executar("UPDATE fornecedor SET codigo=?, nome=?, nuit=?, telefone=?, email=?, endereco=?, cidade=?, ativo=?, saldo=? WHERE id=?",
                    ps -> { ps.setString(1, f.codigo); ps.setString(2, f.nome); ps.setString(3, f.nuit); ps.setString(4, f.telefone);
                            ps.setString(5, f.email); ps.setString(6, f.endereco); ps.setString(7, f.cidade); ps.setBoolean(8, f.ativo); ps.setBigDecimal(9, f.saldo); ps.setLong(10, f.id); });
            return f.id;
        }
    }
}
