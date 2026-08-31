package com.billywater.servico;

import com.billywater.dao.LinhaVendaDAO;
import com.billywater.dao.VendaDAO;
import com.billywater.domain.LinhaVenda;
import com.billywater.domain.Venda;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Vendas POS (VD) da loja. */
public class ServicoLoja {

    private final VendaDAO vendaDAO;
    private final LinhaVendaDAO linhaDAO;
    private final ServicoArmazens armazens;

    public ServicoLoja(VendaDAO vendaDAO, LinhaVendaDAO linhaDAO, ServicoArmazens armazens) {
        this.vendaDAO = vendaDAO;
        this.linhaDAO = linhaDAO;
        this.armazens = armazens;
    }

    public Venda registarVenda(Venda v, java.util.List<LinhaVenda> linhas, Long lojaId) {
        try {
            BigDecimal sub = BigDecimal.ZERO;
            for (LinhaVenda l : linhas) {
                BigDecimal total = nz(l.quantidade).multiply(nz(l.preco)).setScale(2, RoundingMode.HALF_UP);
                l.subtotal = total;
                sub = sub.add(total);
            }
            v.subtotal = sub;
            v.total = sub.subtract(nz(v.desconto)).setScale(2, RoundingMode.HALF_UP);
            if (v.numero == null) v.numero = vendaDAO.proximoNumero(v.serie);
            v.criadoEm = java.time.LocalDateTime.now();
            v.id = vendaDAO.inserir(v);
            for (LinhaVenda l : linhas) {
                l.vendaId = v.id;
                linhaDAO.inserir(l);
                // baixa de stock na loja (só materiais)
                armazens.baixarStock(l.produtoId, lojaId, l.quantidade, "SAIDA_LOJA", "VD #" + v.numero);
            }
            return v;
        } catch (Exception e) { throw new IllegalStateException(e.getMessage(), e); }
    }

    private BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
}
