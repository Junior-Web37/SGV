package com.billywater.dao;

import com.billywater.domain.OrdemServico;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class OrdemServicoDAO extends BaseDAO<OrdemServico> {

    @Override protected String tabela() { return "ordem_servico"; }

    @Override protected OrdemServico mapear(ResultSet rs) throws SQLException {
        OrdemServico o = new OrdemServico();
        o.id = rs.getLong("id");
        o.tipo = rs.getString("tipo");
        o.clienteId = rs.getLong("cliente_id");
        o.contadorId = rs.getLong("contador_id");
        o.estado = rs.getString("estado");
        o.criadaEm = rs.getObject("criada_em", java.time.LocalDate.class);
        o.tecnico = rs.getString("tecnico");
        o.taxaCobrada = rs.getBigDecimal("taxa_cobrada");
        o.motivo = rs.getString("motivo");
        return o;
    }

    public List<OrdemServico> pendentes() throws SQLException {
        return consultar("SELECT * FROM ordem_servico WHERE estado <> 'CONCLUIDA' ORDER BY criada_em", null);
    }

    public long salvar(OrdemServico o) throws SQLException {
        return executarInsert("INSERT INTO ordem_servico (tipo, cliente_id, contador_id, estado, criada_em, tecnico, taxa_cobrada, motivo) VALUES (?,?,?,?,?,?,?,?)",
                ps -> { ps.setString(1, o.tipo); ps.setLong(2, o.clienteId == null ? 0 : o.clienteId); ps.setLong(3, o.contadorId == null ? 0 : o.contadorId);
                        ps.setString(4, o.estado); ps.setObject(5, o.criadaEm == null ? java.time.LocalDate.now() : o.criadaEm); ps.setString(6, o.tecnico);
                        ps.setBigDecimal(7, o.taxaCobrada); ps.setString(8, o.motivo); });
    }
}
