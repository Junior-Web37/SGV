package com.billywater.dao;

import com.billywater.domain.SessaoCaixa;

import java.sql.ResultSet;
import java.sql.SQLException;

public class SessaoCaixaDAO extends BaseDAO<SessaoCaixa> {

    @Override protected String tabela() { return "sessao_caixa"; }

    @Override protected SessaoCaixa mapear(ResultSet rs) throws SQLException {
        SessaoCaixa s = new SessaoCaixa();
        s.id = rs.getLong("id");
        s.abertura = rs.getTimestamp("abertura") == null ? null : rs.getTimestamp("abertura").toLocalDateTime();
        s.fecho = rs.getTimestamp("fecho") == null ? null : rs.getTimestamp("fecho").toLocalDateTime();
        s.operador = rs.getString("operador");
        s.fundoManeio = rs.getBigDecimal("fundo_maneio");
        s.valorEsperado = rs.getBigDecimal("valor_esperado");
        s.valorContado = rs.getBigDecimal("valor_contado");
        s.diferenca = rs.getBigDecimal("diferenca");
        s.estado = rs.getString("estado");
        s.operadorFecho = rs.getString("operador_fecho");
        s.criadaEm = rs.getTimestamp("criada_em") == null ? null : rs.getTimestamp("criada_em").toLocalDateTime();
        return s;
    }

    public SessaoCaixa buscarSessaoAberta(String operador) throws SQLException {
        return consultarUm("SELECT * FROM sessao_caixa WHERE operador = ? AND estado = 'ABERTA' ORDER BY id DESC", ps -> ps.setString(1, operador));
    }

    public long inserir(SessaoCaixa s) throws SQLException {
        return executarInsert("INSERT INTO sessao_caixa (abertura, operador, fundo_maneio, estado, criada_em) VALUES (?,?,?,?,?)",
                ps -> { ps.setTimestamp(1, java.sql.Timestamp.valueOf(java.time.LocalDateTime.now())); ps.setString(2, s.operador);
                        ps.setBigDecimal(3, s.fundoManeio); ps.setString(4, s.estado); ps.setTimestamp(5, java.sql.Timestamp.valueOf(java.time.LocalDateTime.now())); });
    }

    public void actualizar(SessaoCaixa s) throws SQLException {
        executar("UPDATE sessao_caixa SET fecho=?, valor_esperado=?, valor_contado=?, diferenca=?, estado=?, operador_fecho=? WHERE id=?",
                ps -> { ps.setTimestamp(1, java.sql.Timestamp.valueOf(java.time.LocalDateTime.now())); ps.setBigDecimal(2, s.valorEsperado); ps.setBigDecimal(3, s.valorContado);
                        ps.setBigDecimal(4, s.diferenca); ps.setString(5, s.estado); ps.setString(6, s.operadorFecho); ps.setLong(7, s.id); });
    }
}
