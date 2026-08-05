package com.sgv.service;

import com.sgv.entity.Payment;
import com.sgv.entity.Sale;
import com.sgv.entity.User;
import com.sgv.repository.PaymentRepository;
import com.sgv.repository.SaleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final SaleRepository saleRepository;
    private final SaleDocumentService saleDocumentService;
    private final SystemLogService systemLogService;

    public PaymentService(PaymentRepository paymentRepository,
                          SaleRepository saleRepository,
                          SaleDocumentService saleDocumentService,
                          SystemLogService systemLogService) {
        this.paymentRepository = paymentRepository;
        this.saleRepository = saleRepository;
        this.saleDocumentService = saleDocumentService;
        this.systemLogService = systemLogService;
    }

    public java.util.List<Sale> findRecentActiveSales(int maxResults) {
        if (maxResults <= 0) maxResults = 50;
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, maxResults, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
        return saleRepository.findRecentNonCancelled(pageable);
    }

    @Transactional
    public File createPayment(Payment payment, User currentUser) throws Exception {
        if (payment == null) throw new IllegalArgumentException("Payment is null");
        if (payment.getAmount() == null || payment.getAmount() <= 0) {
            throw new IllegalArgumentException("Valor do pagamento deve ser maior que zero.");
        }
        if (payment.getSale() == null || payment.getSale().getId() == null) {
            throw new IllegalArgumentException("Venda associada ao pagamento é obrigatória.");
        }

        Sale managedSale = saleRepository.findById(payment.getSale().getId())
                .orElseThrow(() -> new IllegalArgumentException("Venda não encontrada para o pagamento."));
        payment.setSale(managedSale);

        if (payment.getCreatedAt() == null) {
            payment.setCreatedAt(LocalDateTime.now());
        }

        paymentRepository.save(payment);

        managedSale.setState(com.sgv.model.SaleState.PAGO.name());
        managedSale.setPaidAmount((managedSale.getPaidAmount() != null ? managedSale.getPaidAmount() : 0) + payment.getAmount());
        saleRepository.save(managedSale);

        String origType = managedSale.getDocumentType();
        try {
            managedSale.setDocumentType(com.sgv.model.DocumentType.RECIBO.name());
            File pdf = saleDocumentService.generateDocument(managedSale);
            systemLogService.logUserAction(currentUser != null ? currentUser.getUsername() : "Sistema", "PAGAMENTO_CRIADO", "Pagamento gravado com sucesso.");
            return pdf;
        } catch (Exception ex) {
            throw ex;
        } finally {
            managedSale.setDocumentType(origType);
        }
    }
}
