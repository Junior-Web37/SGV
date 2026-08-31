package com.billywater.servico;

import com.billywater.dao.FacturaDAO;
import com.billywater.dao.VendaDAO;
import com.billywater.domain.Factura;
import com.billywater.domain.Venda;

/** Transferência de contrato/dívida cliente→cliente (FT/VD/FS/COT). */
public class ServicoTransferencias {

    private final FacturaDAO facturaDAO;
    private final VendaDAO vendaDAO;

    public ServicoTransferencias(FacturaDAO facturaDAO, VendaDAO vendaDAO) {
        this.facturaDAO = facturaDAO;
        this.vendaDAO = vendaDAO;
    }

    public boolean transferirFactura(Long facturaId, Long deCliente, Long paraCliente, String motivo) {
        try {
            Factura f = facturaDAO.buscarPorId(facturaId);
            if (f == null) return false;
            f.clienteId = paraCliente;
            facturaDAO.actualizarFaturaCliente(f);
            return true;
        } catch (Exception e) { throw new IllegalStateException(e.getMessage(), e); }
    }

    public boolean transferirVenda(Long vendaId, Long deCliente, Long paraCliente, String motivo) {
        try {
            Venda v = vendaDAO.buscarPorId(vendaId);
            if (v == null) return false;
            v.clienteId = paraCliente;
            vendaDAO.actualizarCliente(v);
            return true;
        } catch (Exception e) { throw new IllegalStateException(e.getMessage(), e); }
    }
}
