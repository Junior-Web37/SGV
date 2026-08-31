package com.billywater.dao;

import com.billywater.domain.Cliente;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class ClienteDAO extends BaseDAO<Cliente> {

    @Override protected String tabela() { return "cliente"; }

    @Override protected Cliente mapear(ResultSet rs) throws SQLException {
        Cliente c = new Cliente();
        c.id = rs.getLong("id");
        c.codigo = rs.getString("codigo");
        c.nomeCompleto = rs.getString("nome_completo");
        c.nuit = rs.getString("nuit");
        c.nif = rs.getString("nif");
        c.email = rs.getString("email");
        c.telefone = rs.getString("telefone");
        c.endereco = rs.getString("endereco");
        c.cidade = rs.getString("cidade");
        c.bairro = rs.getString("bairro");
        c.quarteirao = rs.getString("quarteirao");
        c.casa = rs.getString("casa");
        c.tipo = rs.getString("tipo");
        c.ativo = rs.getBoolean("ativo");
        c.limiteCredito = rs.getBigDecimal("limite_credito");
        c.saldo = rs.getBigDecimal("saldo");
        c.contratoId = rs.getLong("contrato_id");
        c.rotaId = rs.getLong("rota_id");
        c.contadorId = rs.getLong("contador_id");
        c.categoriaTarifaria = rs.getString("categoria_tarifaria");
        c.criadoEm = rs.getTimestamp("criado_em") == null ? null : rs.getTimestamp("criado_em").toLocalDateTime();
        return c;
    }

    public List<Cliente> pesquisar(String termo) throws SQLException {
        return consultar("SELECT * FROM cliente WHERE (nome_completo LIKE ? OR codigo LIKE ? OR nuit LIKE ?) AND ativo = TRUE ORDER BY nome_completo", ps -> {
            String t = "%" + termo + "%"; ps.setString(1, t); ps.setString(2, t); ps.setString(3, t);
        });
    }

    public long salvar(Cliente c) throws SQLException {
        if (c.id == null) {
            return executarInsert("INSERT INTO cliente (codigo, nome_completo, nuit, nif, email, telefone, endereco, cidade, bairro, quarteirao, casa, tipo, ativo, limite_credito, saldo, categoria_tarifaria, criado_em) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,NOW())",
                    ps -> { ps.setString(1, c.codigo); ps.setString(2, c.nomeCompleto); ps.setString(3, c.nuit); ps.setString(4, c.nif); ps.setString(5, c.email);
                            ps.setString(6, c.telefone); ps.setString(7, c.endereco); ps.setString(8, c.cidade); ps.setString(9, c.bairro); ps.setString(10, c.quarteirao);
                            ps.setString(11, c.casa); ps.setString(12, c.tipo); ps.setBoolean(13, c.ativo); ps.setBigDecimal(14, c.limiteCredito);
                            ps.setBigDecimal(15, c.saldo); ps.setString(16, c.categoriaTarifaria); });
        } else {
            executar("UPDATE cliente SET nome_completo=?, telefone=?, email=?, endereco=?, bairro=?, ativo=?, limite_credito=?, saldo=? WHERE id=?",
                    ps -> { ps.setString(1, c.nomeCompleto); ps.setString(2, c.telefone); ps.setString(3, c.email); ps.setString(4, c.endereco); ps.setString(5, c.bairro);
                            ps.setBoolean(6, c.ativo); ps.setBigDecimal(7, c.limiteCredito); ps.setBigDecimal(8, c.saldo); ps.setLong(9, c.id); });
            return c.id;
        }
    }
}
