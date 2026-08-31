package com.billywater.dao;

import com.billywater.domain.Armazem;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class ArmazemDAO extends BaseDAO<Armazem> {

    @Override protected String tabela() { return "armazem"; }

    @Override protected Armazem mapear(ResultSet rs) throws SQLException {
        Armazem a = new Armazem();
        a.id = rs.getLong("id");
        a.codigo = rs.getString("codigo");
        a.nome = rs.getString("nome");
        a.localizacao = rs.getString("localizacao");
        a.tipo = rs.getString("tipo");
        a.ativo = rs.getBoolean("ativo");
        return a;
    }

    public List<Armazem> activos() throws SQLException {
        return consultar("SELECT * FROM armazem WHERE ativo = TRUE ORDER BY nome", null);
    }

    public Armazem buscarPorTipo(String tipo) throws SQLException {
        return consultarUm("SELECT * FROM armazem WHERE tipo = ? ORDER BY id", ps -> ps.setString(1, tipo));
    }

    public long salvar(Armazem a) throws SQLException {
        if (a.id == null) {
            return executarInsert("INSERT INTO armazem (codigo, nome, localizacao, tipo, ativo) VALUES (?,?,?,?,?)",
                    ps -> { ps.setString(1, a.codigo); ps.setString(2, a.nome); ps.setString(3, a.localizacao); ps.setString(4, a.tipo); ps.setBoolean(5, a.ativo); });
        } else {
            executar("UPDATE armazem SET nome=?, localizacao=?, ativo=? WHERE id=?", ps -> { ps.setString(1, a.nome); ps.setString(2, a.localizacao); ps.setBoolean(3, a.ativo); ps.setLong(4, a.id); });
            return a.id;
        }
    }
}
