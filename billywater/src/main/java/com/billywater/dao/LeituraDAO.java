package com.billywater.dao;

import com.billywater.domain.Leitura;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class LeituraDAO extends BaseDAO<Leitura> {

    @Override protected String tabela() { return "leitura"; }

    @Override protected Leitura mapear(ResultSet rs) throws SQLException {
        Leitura l = new Leitura();
        l.id = rs.getLong("id");
        l.contratoId = rs.getLong("contrato_id");
        l.contadorId = rs.getLong("contador_id");
        l.rotaId = rs.getLong("rota_id");
        l.clienteId = rs.getLong("cliente_id");
        l.periodo = rs.getString("periodo");
        l.leituraAnterior = rs.getBigDecimal("leitura_anterior");
        l.leituraActual = rs.getBigDecimal("leitura_actual");
        l.consumo = rs.getBigDecimal("consumo");
        l.tipo = rs.getString("tipo");
        l.dataLeitura = rs.getObject("data_leitura", java.time.LocalDate.class);
        l.leitor = rs.getString("leitor");
        l.estimada = rs.getBoolean("estimada");
        l.anomalia = rs.getString("anomalia");
        l.facturada = rs.getBoolean("facturada");
        l.estado = rs.getString("estado");
        l.criadoEm = rs.getTimestamp("criado_em") == null ? null : rs.getTimestamp("criado_em").toLocalDateTime();
        return l;
    }

    public List<Leitura> paginaFolha(Long rotaId, String periodo, int offset, int limite) throws SQLException {
        return consultar("SELECT * FROM leitura WHERE rota_id = ? AND periodo = ? ORDER BY contador_id LIMIT ? OFFSET ?",
                ps -> { ps.setLong(1, rotaId); ps.setString(2, periodo); ps.setInt(3, limite); ps.setInt(4, offset); });
    }

    public List<Leitura> porPeriodo(String periodo) throws SQLException {
        return consultar("SELECT * FROM leitura WHERE periodo = ? ORDER BY contador_id", ps -> ps.setString(1, periodo));
    }

    /** Leituras validadas, não facturadas — candidatas a FT. */
    public List<Leitura> pendentesFacturar(String periodo) throws SQLException {
        return consultar("SELECT * FROM leitura WHERE periodo = ? AND estado = 'VALIDADA' AND facturada = FALSE ORDER BY contador_id", ps -> ps.setString(1, periodo));
    }

    public long inserir(Leitura l) throws SQLException {
        return executarInsert("INSERT INTO leitura (contrato_id, contador_id, cliente_id, rota_id, periodo, leitura_anterior, leitura_actual, consumo, tipo, data_leitura, leitor, estimada, anomalia, facturada, estado, criado_em) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,NOW())",
                ps -> { ps.setLong(1, l.contratoId); ps.setLong(2, l.contadorId); ps.setLong(3, l.clienteId); ps.setLong(4, l.rotaId); ps.setString(5, l.periodo);
                        ps.setBigDecimal(6, l.leituraAnterior); ps.setBigDecimal(7, l.leituraActual); ps.setBigDecimal(8, l.consumo); ps.setString(9, l.tipo);
                        ps.setObject(10, l.dataLeitura); ps.setString(11, l.leitor); ps.setBoolean(12, l.estimada); ps.setString(13, l.anomalia);
                        ps.setBoolean(14, l.facturada); ps.setString(15, l.estado); });
    }

    public void actualizar(Leitura l) throws SQLException {
        executar("UPDATE leitura SET leitura_anterior=?, leitura_actual=?, consumo=?, tipo=?, data_leitura=?, leitor=?, estimada=?, anomalia=?, facturada=?, estado=? WHERE id=?",
                ps -> { ps.setBigDecimal(1, l.leituraAnterior); ps.setBigDecimal(2, l.leituraActual); ps.setBigDecimal(3, l.consumo); ps.setString(4, l.tipo);
                        ps.setObject(5, l.dataLeitura); ps.setString(6, l.leitor); ps.setBoolean(7, l.estimada); ps.setString(8, l.anomalia);
                        ps.setBoolean(9, l.facturada); ps.setString(10, l.estado); ps.setLong(11, l.id); });
    }

    public List<java.math.BigDecimal> consumosAnteriores(Long contadorId, int n) throws SQLException {
        List<java.math.BigDecimal> out = new java.util.ArrayList<>();
        try (var ps = conn().prepareStatement("SELECT consumo FROM leitura WHERE contador_id = ? AND consumo IS NOT NULL ORDER BY periodo DESC LIMIT ?")) {
            ps.setLong(1, contadorId); ps.setInt(2, n);
            try (var rs = ps.executeQuery()) { while (rs.next()) out.add(rs.getBigDecimal("consumo")); }
        }
        return out;
    }
}
