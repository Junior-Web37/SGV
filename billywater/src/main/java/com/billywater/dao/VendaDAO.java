package com.billywater.dao;

import com.billywater.domain.Venda;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class VendaDAO extends BaseDAO<Venda> {

    @Override protected String tabela() { return "venda"; }

    @Override protected Venda mapear(ResultSet rs) throws SQLException {
        Venda v = new Venda();
        v.id = rs.getLong("id");
        v.serie = rs.getString("serie");
        v.numero = rs.getLong("numero");
        v.clienteId = rs.getLong("cliente_id");
        v.facturaAguaId = rs.getLong("factura_agua_id");
        v.subtotal = rs.getBigDecimal("subtotal");
        v.desconto = rs.getBigDecimal("desconto");
        v.iva = rs.getBigDecimal("iva");
        v.total = rs.getBigDecimal("total");
        v.formaPagamento = rs.getString("forma_pagamento");
        v.devolvida = rs.getBoolean("devolvida");
        v.anulada = rs.getBoolean("anulada");
        v.criadoEm = rs.getTimestamp("criado_em") == null ? null : rs.getTimestamp("criado_em").toLocalDateTime();
        v.operador = rs.getString("operador");
        return v;
    }

    public Long proximoNumero(String serie) throws SQLException {
        try (var ps = conn().prepareStatement("SELECT COALESCE(MAX(numero),0)+1 FROM venda WHERE serie = ?")) {
            ps.setString(1, serie);
            try (var rs = ps.executeQuery()) { return rs.next() ? rs.getLong(1) : 1L; }
        }
    }

    public long inserir(Venda v) throws SQLException {
        return executarInsert("INSERT INTO venda (serie, numero, cliente_id, subtotal, desconto, iva, total, forma_pagamento, devolvida, anulada, criado_em, operador) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                ps -> { ps.setString(1, v.serie); ps.setLong(2, v.numero); ps.setLong(3, v.clienteId == null ? 0 : v.clienteId); ps.setBigDecimal(4, v.subtotal);
                        ps.setBigDecimal(5, v.desconto); ps.setBigDecimal(6, v.iva); ps.setBigDecimal(7, v.total); ps.setString(8, v.formaPagamento);
                        ps.setBoolean(9, v.devolvida); ps.setBoolean(10, v.anulada); ps.setTimestamp(11, java.sql.Timestamp.valueOf(v.criadoEm == null ? java.time.LocalDateTime.now() : v.criadoEm)); ps.setString(12, v.operador); });
    }

    public void actualizar(Venda v) throws SQLException {
        executar("UPDATE venda SET devolvida=?, anulada=? WHERE id=?", ps -> { ps.setBoolean(1, v.devolvida); ps.setBoolean(2, v.anulada); ps.setLong(3, v.id); });
    }

    public List<Venda> hoje() throws SQLException {
        return consultar("SELECT * FROM venda WHERE criado_em >= CURRENT_DATE ORDER BY criado_em", null);
    }

    public void actualizarCliente(Venda v) throws SQLException {
        executar("UPDATE venda SET cliente_id=? WHERE id=?", ps -> { ps.setLong(1, v.clienteId); ps.setLong(2, v.id); });
    }
}
