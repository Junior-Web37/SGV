package com.billywater.servico;

import com.billywater.dao.FacturaDAO;
import com.billywater.dao.MovimentoCaixaDAO;
import com.billywater.dao.PagamentoDAO;
import com.billywater.dao.SessaoCaixaDAO;
import com.billywater.domain.Factura;
import com.billywater.domain.MovimentoCaixa;
import com.billywater.domain.Pagamento;
import com.billywater.domain.SessaoCaixa;

import java.math.BigDecimal;

/** Sessões de caixa, cobranças (REC) e fecho. */
public class ServicoCaixa {

    private final SessaoCaixaDAO sessaoDAO;
    private final MovimentoCaixaDAO movimentoDAO;
    private final PagamentoDAO pagamentoDAO;

    public ServicoCaixa(SessaoCaixaDAO sessaoDAO, MovimentoCaixaDAO movimentoDAO, PagamentoDAO pagamentoDAO) {
        this.sessaoDAO = sessaoDAO;
        this.movimentoDAO = movimentoDAO;
        this.pagamentoDAO = pagamentoDAO;
    }

    public SessaoCaixa abrir(String operador, BigDecimal fundo) {
        SessaoCaixa s = new SessaoCaixa();
        s.operador = operador;
        s.fundoManeio = fundo;
        s.estado = "ABERTA";
        s.criadaEm = java.time.LocalDateTime.now();
        try { sessaoDAO.inserir(s); } catch (Exception e) { throw new IllegalStateException(e.getMessage(), e); }
        return s;
    }

    /** Regista cobrança e gera REC. Métodos não numerários não movem a gaveta. */
    public Pagamento receber(Pagamento p) {
        boolean moveGaveta = movesCashDrawer(p.metodo);
        if (moveGaveta) {
            p.valorRecebido = p.valor == null ? BigDecimal.ZERO : p.valor;
            p.troco = p.valorRecebido.subtract(p.valor).max(BigDecimal.ZERO);
        } else {
            p.valorRecebido = p.valor;
            p.troco = BigDecimal.ZERO;
        }
        p.criadoEm = java.time.LocalDateTime.now();
        try {
            if (p.recNumero == null) p.recNumero = pagamentoDAO.proximoRecNumero();
            p.id = pagamentoDAO.inserir(p);
            if (moveGaveta) {
                MovimentoCaixa m = new MovimentoCaixa();
                m.sessaoId = p.sessaoCaixaId; m.tipo = "IN"; m.valor = p.valor; m.origem = "COBRANCA";
                m.referencia = "REC #" + p.recNumero; m.criadoEm = p.criadoEm;
                movimentoDAO.inserir(m);
            }
        } catch (Exception e) { throw new IllegalStateException(e.getMessage(), e); }
        return p;
    }

    /** Fecho cego. */
    public SessaoCaixa fechar(Long sessaoId, BigDecimal valorContado) {
        try {
            SessaoCaixa s = sessaoDAO.buscarPorId(sessaoId);
            BigDecimal entradas = movimentoDAO.somarPorSessaoETipo(sessaoId, "IN");
            BigDecimal saidas = movimentoDAO.somarPorSessaoETipo(sessaoId, "OUT");
            BigDecimal esperado = nz(s.fundoManeio).add(entradas).subtract(saidas);
            s.valorEsperado = esperado; s.valorContado = valorContado; s.diferenca = valorContado.subtract(esperado);
            s.estado = "FECHADA"; s.fecho = java.time.LocalDateTime.now();
            sessaoDAO.actualizar(s);
            return s;
        } catch (Exception e) { throw new IllegalStateException(e.getMessage(), e); }
    }

    public java.util.Map<String, BigDecimal> fitaZ(Long sessaoId, String operador) {
        try { return pagamentoDAO.resumoPorMetodo(sessaoId); }
        catch (Exception e) { throw new IllegalStateException(e.getMessage(), e); }
    }

    /** Estorno de um REC como operação independente (§25 n.º 13). */
    public Pagamento estornar(Pagamento p) {
        if (p == null || p.id == null) throw new IllegalArgumentException("Seleccione um REC válido.");
        if (Boolean.TRUE.equals(p.estornado)) throw new IllegalStateException("O REC #" + p.recNumero + " já foi estornado.");
        try { pagamentoDAO.marcarEstornado(p.id); }
        catch (Exception ex) { throw new IllegalStateException("Não foi possível marcar o estorno: " + ex.getMessage(), ex); }
        if (movesCashDrawer(p.metodo) && p.sessaoCaixaId != null) {
            try {
                MovimentoCaixa m = new MovimentoCaixa();
                m.sessaoId = p.sessaoCaixaId; m.tipo = "OUT"; m.valor = p.valor; m.origem = "ESTORNO";
                m.referencia = "REC #" + p.recNumero; m.criadoEm = java.time.LocalDateTime.now();
                movimentoDAO.inserir(m);
            } catch (Exception ex) { throw new IllegalStateException("Erro ao sair da gaveta: " + ex.getMessage(), ex); }
        }
        if (p.facturaId != null) {
            try {
                Factura f = new FacturaDAO().buscarPorId(p.facturaId);
                if (f != null && "PAGO".equalsIgnoreCase(f.estado)) {
                    f.estado = "EMITIDA"; f.pagamentoId = null; f.metodoPagamento = null;
                    new FacturaDAO().actualizar(f);
                }
            } catch (Exception ignore) {}
        }
        p.estornado = true;
        return p;
    }

    public static boolean movesCashDrawer(String metodo) {
        return metodo != null && "DINHEIRO".equalsIgnoreCase(metodo.trim());
    }

    private BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
}
