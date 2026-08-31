package com.billywater.dao;

import com.billywater.domain.Auditoria;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class AuditoriaDAO extends BaseDAO<Auditoria> {

    @Override protected String tabela() { return "auditoria"; }

    @Override protected Auditoria mapear(ResultSet rs) throws SQLException {
        Auditoria a = new Auditoria();
        a.id = rs.getLong("id");
        a.data = rs.getTimestamp("data_") == null ? null : rs.getTimestamp("data_").toLocalDateTime();
        a.utilizador = rs.getString("utilizador");
        a.categoria = rs.getString("categoria");
        a.acao = rs.getString("acao");
        a.entidade = rs.getString("entidade");
        a.detalhe = rs.getString("detalhe");
        return a;
    }

    public List<Auditoria> todos() throws SQLException {
        return consultar("SELECT * FROM auditoria ORDER BY data_ DESC", null);
    }

    /** Registos por categoria/combinação, com paginação. */
    public List<Auditoria> filtrar(String categoria, String utilizador, int offset, int limite) throws SQLException {
        return consultar("SELECT * FROM auditoria WHERE (? IS NULL OR categoria = ?) AND (? IS NULL OR utilizador = ?) ORDER BY data_ DESC LIMIT ? OFFSET ?",
                ps -> { ps.setString(1, categoria); ps.setString(2, categoria); ps.setString(3, utilizador); ps.setString(4, utilizador); ps.setInt(5, limite); ps.setInt(6, offset); });
    }

    /** Últimos {@code n} registos, mais recentes primeiro. */
    public List<Auditoria> recentes(int n) throws SQLException {
        return consultar("SELECT * FROM auditoria ORDER BY data_ DESC LIMIT ?", ps -> ps.setInt(1, n));
    }

    public long inserir(Auditoria a) throws SQLException {
        return executarInsert("INSERT INTO auditoria (data_, utilizador, categoria, acao, entidade, detalhe) VALUES (NOW(),?,?,?,?,?)",
                ps -> { ps.setString(1, a.utilizador); ps.setString(2, a.categoria); ps.setString(3, a.acao); ps.setString(4, a.entidade); ps.setString(5, a.detalhe); });
    }
}
