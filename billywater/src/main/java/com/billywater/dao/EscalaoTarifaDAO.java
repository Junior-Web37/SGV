package com.billywater.dao;

import com.billywater.domain.EscalaoTarifa;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class EscalaoTarifaDAO extends BaseDAO<EscalaoTarifa> {

    @Override protected String tabela() { return "escalao_tarifa"; }

    @Override protected EscalaoTarifa mapear(ResultSet rs) throws SQLException {
        EscalaoTarifa e = new EscalaoTarifa();
        e.id = rs.getLong("id");
        e.tarifaId = rs.getLong("tarifa_id");
        e.limiteInferior = rs.getBigDecimal("limite_inferior");
        e.limiteSuperior = rs.getBigDecimal("limite_superior");
        e.precoPorM3 = rs.getBigDecimal("preco_por_m3");
        e.taxaIva = rs.getBigDecimal("taxa_iva");
        return e;
    }

    public List<EscalaoTarifa> buscarPorTarifa(Long tarifaId) throws SQLException {
        return consultar("SELECT * FROM escalao_tarifa WHERE tarifa_id = ? ORDER BY limite_inferior", ps -> ps.setLong(1, tarifaId));
    }

    public long salvar(EscalaoTarifa e) throws SQLException {
        return executarInsert("INSERT INTO escalao_tarifa (tarifa_id, limite_inferior, limite_superior, preco_por_m3, taxa_iva) VALUES (?,?,?,?,?)",
                ps -> { ps.setLong(1, e.tarifaId); ps.setBigDecimal(2, e.limiteInferior); ps.setBigDecimal(3, e.limiteSuperior); ps.setBigDecimal(4, e.precoPorM3); ps.setBigDecimal(5, e.taxaIva); });
    }
}
