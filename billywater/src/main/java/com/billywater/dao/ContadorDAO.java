package com.billywater.dao;

import com.billywater.domain.Contador;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class ContadorDAO extends BaseDAO<Contador> {

    @Override protected String tabela() { return "contador"; }

    @Override protected Contador mapear(ResultSet rs) throws SQLException {
        Contador c = new Contador();
        c.id = rs.getLong("id");
        c.numero = rs.getString("numero");
        c.numeroSerie = rs.getString("numero_serie");
        c.marca = rs.getString("marca");
        c.modelo = rs.getString("modelo");
        c.calibre = rs.getString("calibre");
        c.leituraInstalacao = rs.getBigDecimal("leitura_instalacao");
        c.leituraInicial = rs.getBigDecimal("leitura_inicial");
        c.dataInstalacao = rs.getObject("data_instalacao", java.time.LocalDate.class);
        c.dataUltimaLeitura = rs.getObject("data_ultima_leitura", java.time.LocalDate.class);
        c.ultimoConsumo = rs.getBigDecimal("ultimo_consumo");
        c.ultimaLeitura = rs.getBigDecimal("ultima_leitura");
        c.estado = rs.getString("estado");
        c.clienteId = rs.getLong("cliente_id");
        c.contratoId = rs.getLong("contrato_id");
        c.rotaId = rs.getLong("rota_id");
        c.zonaId = rs.getLong("zona_id");
        c.multiplicador = rs.getInt("multiplicador");
        c.localizacao = rs.getString("localizacao");
        return c;
    }

    public List<Contador> pesquisar(String termo) throws SQLException {
        return consultar("SELECT * FROM contador WHERE (numero LIKE ? OR numero_serie LIKE ? OR marca LIKE ?) ORDER BY numero", ps -> {
            String t = "%" + termo + "%"; ps.setString(1, t); ps.setString(2, t); ps.setString(3, t);
        });
    }

    public long salvar(Contador c) throws SQLException {
        if (c.id == null) {
            return executarInsert("INSERT INTO contador (numero, numero_serie, marca, modelo, calibre, estado, multiplicador, localizacao) VALUES (?,?,?,?,?,?,?,?)",
                    ps -> { ps.setString(1, c.numero); ps.setString(2, c.numeroSerie); ps.setString(3, c.marca); ps.setString(4, c.modelo); ps.setString(5, c.calibre);
                            ps.setString(6, c.estado); ps.setInt(7, c.multiplicador); ps.setString(8, c.localizacao); });
        } else {
            executar("UPDATE contador SET marca=?, modelo=?, estado=?, ultima_leitura=?, ultimo_consumo=?, localizacao=? WHERE id=?",
                    ps -> { ps.setString(1, c.marca); ps.setString(2, c.modelo); ps.setString(3, c.estado); ps.setBigDecimal(4, c.ultimaLeitura);
                            ps.setBigDecimal(5, c.ultimoConsumo); ps.setString(6, c.localizacao); ps.setLong(7, c.id); });
            return c.id;
        }
    }
}
