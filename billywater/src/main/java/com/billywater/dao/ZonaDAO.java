package com.billywater.dao;

import com.billywater.domain.Zona;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class ZonaDAO extends BaseDAO<Zona> {

    @Override protected String tabela() { return "zona"; }

    @Override protected Zona mapear(ResultSet rs) throws SQLException {
        Zona z = new Zona();
        z.id = rs.getLong("id");
        z.codigo = rs.getString("codigo");
        z.nome = rs.getString("nome");
        z.municipio = rs.getString("municipio");
        z.bairro = rs.getString("bairro");
        z.rota = rs.getString("rota");
        z.descricao = rs.getString("descricao");
        z.ativo = rs.getBoolean("ativo");
        z.ativa = rs.getBoolean("ativa");
        z.totalClientes = rs.getInt("total_clientes");
        z.mediaHistorica = rs.getBigDecimal("media_historica");
        return z;
    }

    public List<Zona> pesquisar(String termo) throws SQLException {
        return consultar("SELECT * FROM zona WHERE (nome LIKE ? OR codigo LIKE ?) ORDER BY nome", ps -> {
            String t = "%" + termo + "%"; ps.setString(1, t); ps.setString(2, t);
        });
    }

    public long salvar(Zona z) throws SQLException {
        if (z.id == null) {
            return executarInsert("INSERT INTO zona (codigo, nome, municipio, bairro, rota, descricao, ativo, ativa, total_clientes, media_historica) VALUES (?,?,?,?,?,?,?,?,?,?)",
                    ps -> { ps.setString(1, z.codigo); ps.setString(2, z.nome); ps.setString(3, z.municipio); ps.setString(4, z.bairro); ps.setString(5, z.rota);
                            ps.setString(6, z.descricao); ps.setBoolean(7, z.ativo); ps.setBoolean(8, z.ativa); ps.setInt(9, z.totalClientes); ps.setBigDecimal(10, z.mediaHistorica); });
        } else {
            executar("UPDATE zona SET nome=?, bairro=?, rota=?, ativa=? WHERE id=?", ps -> { ps.setString(1, z.nome); ps.setString(2, z.bairro); ps.setString(3, z.rota); ps.setBoolean(4, z.ativa); ps.setLong(5, z.id); });
            return z.id;
        }
    }
}
