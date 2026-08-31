package com.billywater.dao;

import com.billywater.domain.Contrato;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class ContratoDAO extends BaseDAO<Contrato> {

    @Override protected String tabela() { return "contrato"; }

    @Override protected Contrato mapear(ResultSet rs) throws SQLException {
        Contrato c = new Contrato();
        c.id = rs.getLong("id");
        c.codigo = rs.getString("codigo");
        c.numeroConta = rs.getString("numero_conta");
        c.clienteId = rs.getLong("cliente_id");
        c.contadorId = rs.getLong("contador_id");
        c.tipo = rs.getString("tipo");
        c.categoria = rs.getString("categoria");
        c.categoriaTarifaria = rs.getString("categoria_tarifaria");
        c.zonaId = rs.getLong("zona_id");
        c.tarifaId = rs.getLong("tarifa_id");
        c.rotaId = rs.getLong("rota_id");
        c.dataInicio = rs.getObject("data_inicio", java.time.LocalDate.class);
        c.dataFim = rs.getObject("data_fim", java.time.LocalDate.class);
        c.estado = rs.getString("estado");
        c.limiteCredito = rs.getBigDecimal("limite_credito");
        c.numeroCliente = rs.getString("numero_cliente");
        c.cicloFacturacao = rs.getString("ciclo_facturacao");
        c.diaLeitura = rs.getInt("dia_leitura");
        return c;
    }

    public List<Contrato> activos() throws SQLException {
        return consultar("SELECT * FROM contrato WHERE estado = 'ACTIVO' ORDER BY numero_conta", null);
    }

    public List<Contrato> porCliente(Long clienteId) throws SQLException {
        return consultar("SELECT * FROM contrato WHERE cliente_id = ? ORDER BY numero_conta", ps -> ps.setLong(1, clienteId));
    }

    public Contrato porContador(Long contadorId) throws SQLException {
        return consultarUm("SELECT * FROM contrato WHERE contador_id = ? AND estado = 'ACTIVO' ORDER BY id", ps -> ps.setLong(1, contadorId));
    }

    public long salvar(Contrato c) throws SQLException {
        if (c.id == null) {
            return executarInsert("INSERT INTO contrato (codigo, numero_conta, cliente_id, contador_id, tipo, categoria, categoria_tarifaria, zona_id, tarifa_id, rota_id, data_inicio, data_fim, estado, limite_credito, numero_cliente, ciclo_facturacao, dia_leitura) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    ps -> { ps.setString(1, c.codigo); ps.setString(2, c.numeroConta); ps.setLong(3, c.clienteId); ps.setLong(4, c.contadorId);
                            ps.setString(5, c.tipo); ps.setString(6, c.categoria); ps.setString(7, c.categoriaTarifaria); ps.setLong(8, c.zonaId);
                            ps.setLong(9, c.tarifaId); ps.setLong(10, c.rotaId); ps.setObject(11, c.dataInicio); ps.setObject(12, c.dataFim);
                            ps.setString(13, c.estado); ps.setBigDecimal(14, c.limiteCredito); ps.setString(15, c.numeroCliente);
                            ps.setString(16, c.cicloFacturacao); ps.setInt(17, c.diaLeitura); });
        } else {
            executar("UPDATE contrato SET estado=?, categoria_tarifaria=?, tarifa_id=?, limite_credito=?, dia_leitura=? WHERE id=?",
                    ps -> { ps.setString(1, c.estado); ps.setString(2, c.categoriaTarifaria); ps.setLong(3, c.tarifaId); ps.setBigDecimal(4, c.limiteCredito); ps.setInt(5, c.diaLeitura); ps.setLong(6, c.id); });
            return c.id;
        }
    }
}
