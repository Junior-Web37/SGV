package com.billywater.dao;

import com.billywater.domain.TransferenciaArmazem;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class TransferenciaArmazemDAO extends BaseDAO<TransferenciaArmazem> {

    @Override protected String tabela() { return "transferencia_armazem"; }

    @Override protected TransferenciaArmazem mapear(ResultSet rs) throws SQLException {
        TransferenciaArmazem t = new TransferenciaArmazem();
        t.id = rs.getLong("id");
        t.serie = rs.getString("serie");
        t.numero = rs.getLong("numero");
        t.origemId = rs.getLong("origem_id");
        t.destinoId = rs.getLong("destino_id");
        t.estado = rs.getString("estado");
        t.referencia = rs.getString("referencia");
        t.criadoEm = rs.getTimestamp("criado_em") == null ? null : rs.getTimestamp("criado_em").toLocalDateTime();
        t.operador = rs.getString("operador");
        return t;
    }

    public List<TransferenciaArmazem> todos() throws SQLException {
        return consultar("SELECT * FROM transferencia_armazem ORDER BY criado_em DESC", null);
    }

    public long salvar(TransferenciaArmazem t) throws SQLException {
        return executarInsert("INSERT INTO transferencia_armazem (serie, numero, origem_id, destino_id, estado, referencia, criado_em, operador) VALUES (?,?,?,?,?,?,?,?)",
                ps -> { ps.setString(1, t.serie); ps.setLong(2, t.numero); ps.setLong(3, t.origemId); ps.setLong(4, t.destinoId);
                        ps.setString(5, t.estado); ps.setString(6, t.referencia); ps.setTimestamp(7, java.sql.Timestamp.valueOf(t.criadoEm == null ? java.time.LocalDateTime.now() : t.criadoEm)); ps.setString(8, t.operador); });
    }
}
