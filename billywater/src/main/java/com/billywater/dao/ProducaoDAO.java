package com.billywater.dao;

import com.billywater.domain.Producao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class ProducaoDAO extends BaseDAO<Producao> {

    @Override protected String tabela() { return "producao"; }

    @Override protected Producao mapear(ResultSet rs) throws SQLException {
        Producao p = new Producao();
        p.id = rs.getLong("id");
        p.data = rs.getObject("data_", java.time.LocalDate.class);
        p.periodo = rs.getString("periodo");
        p.volumeProduzido = rs.getBigDecimal("volume_produzido");
        p.volumeFacturado = rs.getBigDecimal("volume_facturado");
        p.perdas = rs.getBigDecimal("perdas");
        p.nrwPercent = rs.getBigDecimal("nrw_percent");
        p.fonte = rs.getString("fonte");
        return p;
    }

    public List<Producao> porPeriodo(String periodo) throws SQLException {
        return consultar("SELECT * FROM producao WHERE periodo = ? ORDER BY data_", ps -> ps.setString(1, periodo));
    }

    public long salvar(Producao p) throws SQLException {
        if (p.id == null) {
            return executarInsert("INSERT INTO producao (data_, periodo, volume_produzido, volume_facturado, perdas, nrw_percent, fonte) VALUES (?,?,?,?,?,?,?)",
                    ps -> { ps.setObject(1, p.data); ps.setString(2, p.periodo); ps.setBigDecimal(3, p.volumeProduzido); ps.setBigDecimal(4, p.volumeFacturado);
                            ps.setBigDecimal(5, p.perdas); ps.setBigDecimal(6, p.nrwPercent); ps.setString(7, p.fonte); });
        } else {
            executar("UPDATE producao SET volume_produzido=?, volume_facturado=?, perdas=?, nrw_percent=? WHERE id=?",
                    ps -> { ps.setBigDecimal(1, p.volumeProduzido); ps.setBigDecimal(2, p.volumeFacturado); ps.setBigDecimal(3, p.perdas); ps.setBigDecimal(4, p.nrwPercent); ps.setLong(5, p.id); });
            return p.id;
        }
    }
}
