package com.billywater.dao;

import com.billywater.domain.Utilizador;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class UtilizadorDAO extends BaseDAO<Utilizador> {

    @Override protected String tabela() { return "utilizador"; }

    @Override protected Utilizador mapear(ResultSet rs) throws SQLException {
        Utilizador u = new Utilizador();
        u.id = rs.getLong("id");
        u.nomeCompleto = rs.getString("nome_completo");
        u.username = rs.getString("username");
        u.senhaHash = rs.getString("senha_hash");
        u.email = rs.getString("email");
        u.perfil = rs.getString("perfil");
        u.ativo = rs.getBoolean("ativo");
        u.forcarMudancaSenha = rs.getBoolean("forcar_mudanca_senha");
        u.tentativasFalhadas = rs.getInt("tentativas_falhadas");
        u.bloqueado = rs.getBoolean("bloqueado");
        u.criadoEm = rs.getTimestamp("criado_em") == null ? null : rs.getTimestamp("criado_em").toLocalDateTime();
        return u;
    }

    public Utilizador porUsername(String username) throws SQLException {
        return consultarUm("SELECT * FROM utilizador WHERE username = ?", ps -> ps.setString(1, username));
    }

    public long salvar(Utilizador u) throws SQLException {
        if (u.id == null) {
            return executarInsert("INSERT INTO utilizador (nome_completo, username, senha_hash, email, perfil, ativo, forcar_mudanca_senha, tentativas_falhadas, bloqueado, criado_em) VALUES (?,?,?,?,?,?,?,?,?,?)",
                    ps -> { ps.setString(1, u.nomeCompleto); ps.setString(2, u.username); ps.setString(3, u.senhaHash); ps.setString(4, u.email);
                            ps.setString(5, u.perfil); ps.setBoolean(6, u.ativo); ps.setBoolean(7, u.forcarMudancaSenha);
                            ps.setInt(8, u.tentativasFalhadas); ps.setBoolean(9, u.bloqueado); ps.setTimestamp(10, java.sql.Timestamp.valueOf(java.time.LocalDateTime.now())); });
        } else {
            executar("UPDATE utilizador SET nome_completo=?, email=?, perfil=?, ativo=?, forcar_mudanca_senha=?, tentativas_falhadas=?, bloqueado=? WHERE id=?",
                    ps -> { ps.setString(1, u.nomeCompleto); ps.setString(2, u.email); ps.setString(3, u.perfil); ps.setBoolean(4, u.ativo);
                            ps.setBoolean(5, u.forcarMudancaSenha); ps.setInt(6, u.tentativasFalhadas); ps.setBoolean(7, u.bloqueado); ps.setLong(8, u.id); });
            return u.id;
        }
    }

    public void registrarFalha(Long id) throws SQLException {
        executar("UPDATE utilizador SET tentativas_falhadas = tentativas_falhadas + 1, bloqueado = CASE WHEN tentativas_falhadas + 1 >= 5 THEN TRUE ELSE bloqueado END WHERE id = ?", ps -> ps.setLong(1, id));
    }
}
