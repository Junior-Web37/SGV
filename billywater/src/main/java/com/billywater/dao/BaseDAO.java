package com.billywater.dao;

import com.billywater.config.ConfigBanco;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Base comum dos DAOs: conexão, DML, projecção de ResultSet e paginação. */
public abstract class BaseDAO<T> {

    protected Connection conn() throws SQLException { return ConfigBanco.get(); }

    protected abstract String tabela();
    protected abstract T mapear(ResultSet rs) throws SQLException;

    protected Long key(ResultSet rs) throws SQLException { return rs.getLong("id"); }

    protected long executarInsert(String sql, SqlBinder binder) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            binder.bind(ps);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) { if (keys.next()) return keys.getLong(1); }
        }
        return -1;
    }

    protected int executar(String sql, SqlBinder binder) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            binder.bind(ps);
            return ps.executeUpdate();
        }
    }

    protected List<T> consultar(String sql, SqlBinder binder) throws SQLException {
        List<T> out = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            if (binder != null) binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(mapear(rs)); }
        }
        return out;
    }

    protected T consultarUm(String sql, SqlBinder binder) throws SQLException {
        List<T> lista = consultar(sql, binder);
        return lista.isEmpty() ? null : lista.get(0);
    }

    public List<T> todos() throws SQLException {
        return consultar("SELECT * FROM " + tabela(), null);
    }

    public T buscarPorId(Long id) throws SQLException {
        return consultarUm("SELECT * FROM " + tabela() + " WHERE id = ?", ps -> ps.setLong(1, id));
    }

    public List<T> pagina(int offset, int limite) throws SQLException {
        return consultar("SELECT * FROM " + tabela() + " ORDER BY id LIMIT ? OFFSET ?",
                ps -> { ps.setInt(1, Math.max(1, limite)); ps.setInt(2, Math.max(0, offset)); });
    }

    public long contar() throws SQLException {
        try (Statement st = conn().createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) AS c FROM " + tabela())) {
            return rs.next() ? rs.getLong("c") : 0;
        }
    }

    public int eliminarPorId(Long id) throws SQLException {
        return executar("DELETE FROM " + tabela() + " WHERE id = ?", ps -> ps.setLong(1, id));
    }

    @FunctionalInterface
    public interface SqlBinder {
        void bind(PreparedStatement ps) throws SQLException;
    }
}
