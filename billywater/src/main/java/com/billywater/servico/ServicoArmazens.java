package com.billywater.servico;

import com.billywater.dao.MovimentoStockDAO;
import com.billywater.dao.StockDAO;
import com.billywater.dao.TransferenciaArmazemDAO;
import com.billywater.domain.MovimentoStock;
import com.billywater.domain.StockItem;
import com.billywater.domain.TransferenciaArmazem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Stock por local + transferência atómica + kardex. */
public class ServicoArmazens {

    private final StockDAO stockDAO;
    private final MovimentoStockDAO movimentoDAO;
    private final TransferenciaArmazemDAO transferenciaDAO;

    public ServicoArmazens(StockDAO stockDAO, MovimentoStockDAO movimentoDAO, TransferenciaArmazemDAO transferenciaDAO) {
        this.stockDAO = stockDAO;
        this.movimentoDAO = movimentoDAO;
        this.transferenciaDAO = transferenciaDAO;
    }

    public void entrarStock(Long produtoId, Long localId, BigDecimal qtd, String tipo, String referencia) {
        if (qtd == null || qtd.signum() <= 0) throw new IllegalArgumentException("Quantidade deve ser > 0.");
        try {
            StockItem item = stockDAO.buscarPorProdutoELocal(produtoId, localId);
            BigDecimal antes = item == null ? BigDecimal.ZERO : nz(item.quantidade);
            if (item == null) { item = new StockItem(); item.produtoId = produtoId; item.armazemId = localId; item.quantidade = BigDecimal.ZERO; }
            BigDecimal depois = antes.add(nz(qtd));
            item.quantidade = depois;
            stockDAO.salvar(item);
            registarMovimento(produtoId, localId, qtd, antes, depois, tipo, referencia);
        } catch (Exception e) { throw new IllegalStateException(e.getMessage(), e); }
    }

    public void baixarStock(Long produtoId, Long localId, BigDecimal qtd, String tipo, String referencia) {
        if (qtd == null || qtd.signum() <= 0) throw new IllegalArgumentException("Quantidade deve ser > 0.");
        try {
            StockItem item = stockDAO.buscarPorProdutoELocal(produtoId, localId);
            BigDecimal antes = item == null ? BigDecimal.ZERO : nz(item.quantidade);
            if (antes.compareTo(qtd) < 0) throw new IllegalArgumentException("Stock insuficiente no local.");
            BigDecimal depois = antes.subtract(nz(qtd));
            item.quantidade = depois;
            stockDAO.salvar(item);
            registarMovimento(produtoId, localId, qtd.negate(), antes, depois, tipo, referencia);
        } catch (IllegalArgumentException e) { throw e; }
        catch (Exception e) { throw new IllegalStateException(e.getMessage(), e); }
    }

    public TransferenciaArmazem transferir(TransferenciaArmazem t, Long produtoId, BigDecimal qtd) {
        if (t.origemId.equals(t.destinoId)) throw new IllegalArgumentException("Origem e destino devem ser diferentes.");
        if (qtd == null || qtd.signum() <= 0) throw new IllegalArgumentException("Quantidade deve ser > 0.");
        try {
            StockItem origem = stockDAO.buscarPorProdutoELocal(produtoId, t.origemId);
            if (origem == null || nz(origem.quantidade).compareTo(qtd) < 0)
                throw new IllegalArgumentException("Stock disponível insuficiente na origem.");
            baixarStock(produtoId, t.origemId, qtd, "TRANSFERENCIA_SAIDA", t.referencia);
            entrarStock(produtoId, t.destinoId, qtd, "TRANSFERENCIA_ENTRADA", t.referencia);
            t.estado = "COMPLETED";
            transferenciaDAO.salvar(t);
            return t;
        } catch (IllegalArgumentException e) { throw e; }
        catch (Exception e) { throw new IllegalStateException(e.getMessage(), e); }
    }

    public List<Object[]> porTransferir(Long armazemId, Long lojaId) {
        try { return stockDAO.porTransferir(armazemId, lojaId); }
        catch (Exception e) { throw new IllegalStateException(e.getMessage(), e); }
    }

    private void registarMovimento(Long produtoId, Long localId, BigDecimal qtd, BigDecimal antes, BigDecimal depois, String tipo, String referencia) {
        try {
            MovimentoStock m = new MovimentoStock();
            m.produtoId = produtoId; m.armazemId = localId; m.tipo = tipo;
            m.quantidade = qtd.setScale(2, RoundingMode.HALF_UP);
            m.stockAntes = antes; m.stockDepois = depois; m.referencia = referencia;
            m.criadoEm = java.time.LocalDateTime.now();
            m.operador = com.billywater.ui.Sessao.atual() != null ? com.billywater.ui.Sessao.atual().username : null;
            movimentoDAO.inserir(m);
        } catch (Exception ignored) {}
    }

    private BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
}
