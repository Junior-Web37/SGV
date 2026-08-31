package com.billywater.dao;

import com.billywater.domain.Parametro;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ParametroDAO extends BaseDAO<Parametro> {

    @Override protected String tabela() { return "parametro"; }

    @Override protected Parametro mapear(ResultSet rs) throws SQLException {
        Parametro p = new Parametro();
        p.id = rs.getLong("id");
        p.nomeEmpresa = rs.getString("nome_empresa");
        p.nuit = rs.getString("nuit");
        p.alvara = rs.getString("alvara");
        p.endereco = rs.getString("endereco");
        p.cidade = rs.getString("cidade");
        p.telefone = rs.getString("telefone");
        p.email = rs.getString("email");
        p.rodapeDocumental = rs.getString("rodape_documental");
        p.ivaGeral = rs.getBigDecimal("iva_geral");
        p.percentagemIvaAgua = rs.getBigDecimal("percentagem_iva_agua");
        p.taxaSaneamento = rs.getBigDecimal("taxa_saneamento");
        p.taxaReligacao = rs.getBigDecimal("taxa_religacao");
        p.diaLimitePagamento = rs.getInt("dia_limite_pagamento");
        p.serieFt = rs.getString("serie_ft");
        p.serieVd = rs.getString("serie_vd");
        p.pastaBackups = rs.getString("pasta_backups");
        p.impressoraTermica = rs.getString("impressora_termica");
        p.impressaoTermicaAtiva = rs.getBoolean("impressao_termica_ativa");
        return p;
    }

    public Parametro unico() throws SQLException { return buscarPorId(1L); }

    public void salvar(Parametro p) throws SQLException {
        executar("UPDATE parametro SET nome_empresa=?, nuit=?, alvara=?, endereco=?, cidade=?, telefone=?, email=?, rodape_documental=?, iva_geral=?, percentagem_iva_agua=?, taxa_saneamento=?, taxa_religacao=?, dia_limite_pagamento=?, serie_ft=?, serie_vd=?, pasta_backups=?, impressora_termica=?, impressao_termica_ativa=? WHERE id=?",
                ps -> { ps.setString(1, p.nomeEmpresa); ps.setString(2, p.nuit); ps.setString(3, p.alvara); ps.setString(4, p.endereco);
                        ps.setString(5, p.cidade); ps.setString(6, p.telefone); ps.setString(7, p.email); ps.setString(8, p.rodapeDocumental);
                        ps.setBigDecimal(9, p.ivaGeral); ps.setBigDecimal(10, p.percentagemIvaAgua); ps.setBigDecimal(11, p.taxaSaneamento);
                        ps.setBigDecimal(12, p.taxaReligacao); ps.setInt(13, p.diaLimitePagamento); ps.setString(14, p.serieFt);
                        ps.setString(15, p.serieVd); ps.setString(16, p.pastaBackups); ps.setString(17, p.impressoraTermica);
                        ps.setBoolean(18, p.impressaoTermicaAtiva); ps.setLong(19, p.id); });
    }
}
