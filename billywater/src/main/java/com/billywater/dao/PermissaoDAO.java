package com.billywater.dao;

import com.billywater.domain.Permissao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PermissaoDAO extends BaseDAO<Permissao> {

    @Override protected String tabela() { return "permissao"; }

    @Override protected Permissao mapear(ResultSet rs) throws SQLException {
        Permissao p = new Permissao();
        p.id = rs.getLong("id");
        p.perfil = rs.getString("perfil");
        p.pagina = rs.getString("pagina");
        p.acao = rs.getString("acao");
        p.permitir = rs.getBoolean("permitir");
        return p;
    }

    public List<Permissao> porPerfil(String perfil) throws SQLException {
        return consultar("SELECT * FROM permissao WHERE perfil = ?", ps -> ps.setString(1, perfil));
    }

    public long salvar(Permissao p) throws SQLException {
        if (p.id == null) {
            return executarInsert("INSERT INTO permissao (perfil, pagina, acao, permitir) VALUES (?,?,?,?)",
                    ps -> { ps.setString(1, p.perfil); ps.setString(2, p.pagina); ps.setString(3, p.acao); ps.setBoolean(4, p.permitir); });
        } else {
            executar("UPDATE permissao SET permitir=? WHERE id=?", ps -> { ps.setBoolean(1, p.permitir); ps.setLong(2, p.id); });
            return p.id;
        }
    }

    public Map<String, Map<String, Boolean>> mapaPorPerfil() throws SQLException {
        if (todos().isEmpty()) return null;
        return todos().stream().collect(Collectors.groupingBy(p -> p.perfil,
                Collectors.toMap(p -> p.pagina + ":" + p.acao, p -> p.permitir, (a, b) -> b, java.util.LinkedHashMap::new)));
    }
}
