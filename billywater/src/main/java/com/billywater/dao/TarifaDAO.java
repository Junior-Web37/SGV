package com.billywater.dao;

import com.billywater.domain.Tarifa;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class TarifaDAO extends BaseDAO<Tarifa> {

    @Override protected String tabela() { return "tarifa"; }

    @Override protected Tarifa mapear(ResultSet rs) throws SQLException {
        Tarifa t = new Tarifa();
        t.id = rs.getLong("id");
        t.codigo = rs.getString("codigo");
        t.nome = rs.getString("nome");
        t.categoria = rs.getString("categoria");
        t.vigencia = rs.getObject("vigencia", java.time.LocalDate.class);
        t.minimoFacturavel = rs.getBigDecimal("minimo_facturavel");
        t.taxaDisponibilidade = rs.getBigDecimal("taxa_disponibilidade");
        t.taxaSaneamento = rs.getBigDecimal("taxa_saneamento");
        t.percentagemIvaBase = rs.getBigDecimal("percentagem_iva_base");
        t.isentoIvaMinimo = rs.getBoolean("isento_iva_minimo");
        t.vigenciaInicio = rs.getObject("vigencia_inicio", java.time.LocalDate.class);
        t.vigenciaFim = rs.getObject("vigencia_fim", java.time.LocalDate.class);
        t.ativo = rs.getBoolean("ativo");
        return t;
    }

    public Tarifa buscarDefault() throws SQLException {
        return consultarUm("SELECT * FROM tarifa WHERE ativo = TRUE ORDER BY id LIMIT 1", null);
    }

    public Tarifa buscarPorCategoria(String categoria) throws SQLException {
        if (categoria == null || categoria.isBlank()) return null;
        return consultarUm("SELECT * FROM tarifa WHERE categoria = ? AND ativo = TRUE ORDER BY id LIMIT 1", ps -> ps.setString(1, categoria));
    }

    public long salvar(Tarifa t) throws SQLException {
        if (t.id == null) {
            return executarInsert("INSERT INTO tarifa (codigo, nome, categoria, minimo_facturavel, taxa_disponibilidade, taxa_saneamento, percentagem_iva_base, isento_iva_minimo, ativo) VALUES (?,?,?,?,?,?,?,?,?)",
                    ps -> { ps.setString(1, t.codigo); ps.setString(2, t.nome); ps.setString(3, t.categoria); ps.setBigDecimal(4, t.minimoFacturavel);
                            ps.setBigDecimal(5, t.taxaDisponibilidade); ps.setBigDecimal(6, t.taxaSaneamento); ps.setBigDecimal(7, t.percentagemIvaBase);
                            ps.setBoolean(8, t.isentoIvaMinimo); ps.setBoolean(9, t.ativo); });
        } else {
            executar("UPDATE tarifa SET nome=?, categoria=?, ativo=? WHERE id=?", ps -> { ps.setString(1, t.nome); ps.setString(2, t.categoria); ps.setBoolean(3, t.ativo); ps.setLong(4, t.id); });
            return t.id;
        }
    }
}
