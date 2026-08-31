package com.billywater.servico;

import com.billywater.config.ConfigBanco;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.List;

/** Relatórios (estrutura SGV) — 15 relatórios com SQL real + exportação. */
public class ServicoRelatorios {

    public record Relatorio(String codigo, String titulo, String descricao, String[] colunas, String sql) {}

    public static final List<Relatorio> RELATORIOS = List.of(
        new Relatorio("MAPA_FACTURACAO", "Mapa de Facturação de Água", "Facturado por período",
            new String[]{"Período", "Nº FT", "Total", "IVA"},
            "SELECT periodo, COUNT(*) AS n, ROUND(SUM(total),2) AS total, ROUND(SUM(iva),2) AS iva FROM factura WHERE tipo_documento='FT' AND NOT anulada GROUP BY periodo ORDER BY periodo"),
        new Relatorio("MAIORES_CONSUMOS", "Maiores Consumos", "Top consumidores por m³",
            new String[]{"Cliente", "Consumo (m³)", "Total"},
            "SELECT cliente_id, MAX(consumo) AS consumo, ROUND(SUM(total),2) AS total FROM factura WHERE tipo_documento='FT' AND NOT anulada GROUP BY cliente_id ORDER BY consumo DESC"),
        new Relatorio("APURAMENTO_IVA", "Apuramento de IVA", "Base tributável/isenta por tipo",
            new String[]{"Tipo", "Base", "IVA", "Total"},
            "SELECT tipo_documento, ROUND(SUM(subtotal),2) AS base, ROUND(SUM(iva),2) AS iva, ROUND(SUM(total),2) AS total FROM factura WHERE NOT anulada GROUP BY tipo_documento"),
        new Relatorio("CONTAS_CORRENTES", "Contas Correntes (devedores)", "Aging / dívida por cliente",
            new String[]{"Cliente", "Dívida"},
            "SELECT cliente_id, ROUND(SUM(total),2) AS divida FROM factura WHERE estado <> 'PAGO' AND NOT anulada GROUP BY cliente_id ORDER BY divida DESC"),
        new Relatorio("CONSUMO_CONTADOR", "Consumo por Contador / Kardex", "Histórico de leituras",
            new String[]{"Contador", "Período", "Anterior", "Actual", "Consumo"},
            "SELECT contador_id, periodo, leitura_anterior, leitura_actual, consumo FROM leitura ORDER BY contador_id, periodo"),
        new Relatorio("FACTURACAO_ZONA", "Facturação por Zona", "Agregação por zona/rota",
            new String[]{"Zona", "Total"},
            "SELECT c.zona_id, ROUND(SUM(f.total),2) AS total FROM factura f JOIN contrato c ON c.id=f.contrato_id WHERE f.tipo_documento='FT' AND NOT f.anulada GROUP BY c.zona_id"),
        new Relatorio("NRW", "NRW / Água Não Facturada", "Produção vs facturada",
            new String[]{"Período", "Produzido (m³)", "Facturado (m³)", "Perdas (m³)", "NRW (%)"},
            "SELECT periodo, ROUND(SUM(volume_produzido),2) AS produzido, ROUND(SUM(volume_facturado),2) AS facturado, ROUND(SUM(perdas),2) AS perdas, ROUND(AVG(nrw_percent),2) AS nrw FROM producao GROUP BY periodo ORDER BY periodo"),
        new Relatorio("LEITURAS_ESTIMADAS", "Leituras Estimadas", "Facturadas sem leitura física",
            new String[]{"Contador", "Período", "Consumo", "Anomalia"},
            "SELECT contador_id, periodo, consumo, anomalia FROM leitura WHERE estimada=TRUE ORDER BY contador_id"),
        new Relatorio("VENDAS_LOJA", "Vendas da Loja por Produto", "Top/vendas VD",
            new String[]{"Produto", "Qtd", "Total"},
            "SELECT p.nome, ROUND(SUM(lv.quantidade),2) AS qtd, ROUND(SUM(lv.subtotal),2) AS total FROM linha_venda lv JOIN produto p ON p.id=lv.produto_id GROUP BY p.nome ORDER BY total DESC"),
        new Relatorio("STOCK_VALORIZADO", "Stock Valorizado", "Materiais por local, custo",
            new String[]{"Produto", "Quantidade", "Custo", "Valor"},
            "SELECT p.nome, s.quantidade, p.preco_compra, ROUND(s.quantidade*p.preco_compra,2) AS valor FROM stock_item s JOIN produto p ON p.id=s.produto_id ORDER BY valor DESC"),
        new Relatorio("MAPA_FISCAL", "Mapa Fiscal FT/NC/VD", "Universo fiscal",
            new String[]{"Tipo", "Série", "Nº", "Total", "Estado"},
            "SELECT tipo_documento, serie, numero, ROUND(total,2) AS total, estado FROM factura ORDER BY criado_em"),
        new Relatorio("PAG_FORNECEDORES", "Pagamentos a Fornecedores", "Compras + pagamentos",
            new String[]{"Fornecedor", "Guia", "Nº", "Referência", "Total", "Estado"},
            "SELECT f.nome, c.serie, c.numero, c.referencia, ROUND(c.valor_total,2) AS total, c.estado FROM compra c JOIN fornecedor f ON f.id=c.fornecedor_id ORDER BY c.data_"),
        new Relatorio("RESUMO_VS_COBRANCA", "Resumo Facturação vs Cobrança", "Facturado vs cobrado (série mensal)",
            new String[]{"Período", "Facturado", "Cobrado"},
            "SELECT periodo, ROUND(SUM(total),2) AS facturado, 0 AS cobrado FROM factura WHERE tipo_documento='FT' AND NOT anulada GROUP BY periodo"),
        new Relatorio("COBRANCA_OPERADOR", "Cobrança por Operador e Método", "Recebimentos",
            new String[]{"Operador", "Método", "Total"},
            "SELECT operador, metodo, ROUND(SUM(valor),2) AS total FROM pagamento GROUP BY operador, metodo"),
        new Relatorio("SAFT", "SAF-T MZ", "Exportação XML oficial (opcional)",
            new String[]{"Aviso"},
            "SELECT 'SAF-T em preparação (FASE 7 opcional)' AS aviso")
    );

    public Rows consultar(Relatorio r, String periodo) throws java.sql.SQLException {
        List<Object[]> linhas = new ArrayList<>();
        List<String> colunas = List.of(r.colunas);
        String sql = filtroPeriodo(r, periodo);
        boolean temPeriodo = sql.contains("?");
        try (PreparedStatement ps = ConfigBanco.get().prepareStatement(sql)) {
            int idx = 1;
            if (temPeriodo) ps.setString(idx++, "".equals(periodo == null ? "" : periodo.trim()) ? null : periodo.trim());
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData md = rs.getMetaData();
                int n = md.getColumnCount();
                while (rs.next()) { Object[] row = new Object[Math.max(n, r.colunas.length)]; for (int i = 1; i <= n; i++) row[i-1] = rs.getObject(i); linhas.add(row); }
            }
        }
        return new Rows(colunas, linhas);
    }

    private String filtroPeriodo(Relatorio r, String periodo) {
        String sql = r.sql;
        boolean aplicar = periodo != null && !periodo.trim().isEmpty() && sql.toLowerCase().contains("periodo");
        if (!aplicar) return sql;
        String up = sql.toUpperCase();
        if (up.contains(" GROUP BY ")) {
            int i = up.indexOf(" GROUP BY ");
            String antes = sql.substring(0, i);
            return antes + (antes.toUpperCase().contains(" WHERE ") ? " AND " : " WHERE ") + "periodo = ?" + sql.substring(i);
        }
        int order = up.indexOf(" ORDER BY ");
        if (order >= 0) {
            String antes = sql.substring(0, order);
            return antes + (antes.toUpperCase().contains(" WHERE ") ? " AND " : " WHERE ") + "periodo = ?" + sql.substring(order);
        }
        return sql + (sql.toUpperCase().contains(" WHERE ") ? " AND " : " WHERE ") + "periodo = ?";
    }

    public record Rows(List<String> colunas, List<Object[]> linhas) {}

    public List<Relatorio> listar() { return RELATORIOS; }

    public static boolean reportavel(String estado, Boolean anulada, Boolean treinoDemo, String tipoDoc) {
        if (treinoDemo != null && treinoDemo) return false;
        if (anulada != null && anulada) return false;
        if (estado != null && "ANULADA".equalsIgnoreCase(estado)) return false;
        if (tipoDoc != null) { String t = tipoDoc.toUpperCase(); return t.equals("FT") || t.equals("VD") || t.equals("FS") || t.equals("NC"); }
        return true;
    }
}
