package com.sgv.service;

import com.sgv.entity.Customer;
import com.sgv.entity.Payment;
import com.sgv.entity.PaymentAllocation;
import com.sgv.entity.Sale;
import com.sgv.entity.User;
import com.sgv.repository.CustomerRepository;
import com.sgv.repository.PaymentAllocationRepository;
import com.sgv.repository.PaymentRepository;
import com.sgv.repository.SaleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final SaleRepository saleRepository;
    private final CustomerRepository customerRepository;
    private final PaymentAllocationRepository paymentAllocationRepository;
    private final CashSessionService cashSessionService;
    private final SaleDocumentService saleDocumentService;
    private final SystemLogService systemLogService;

    public PaymentService(PaymentRepository paymentRepository,
                          SaleRepository saleRepository,
                          CustomerRepository customerRepository,
                          PaymentAllocationRepository paymentAllocationRepository,
                          CashSessionService cashSessionService,
                          SaleDocumentService saleDocumentService,
                          SystemLogService systemLogService) {
        this.paymentRepository = paymentRepository;
        this.saleRepository = saleRepository;
        this.customerRepository = customerRepository;
        this.paymentAllocationRepository = paymentAllocationRepository;
        this.cashSessionService = cashSessionService;
        this.saleDocumentService = saleDocumentService;
        this.systemLogService = systemLogService;
    }

    public List<Sale> findPendingSales() {
        return saleRepository.findPendingSales();
    }

    public List<Sale> findRecentActiveSales(int maxResults) {
        return findPendingSales();
    }

    @Transactional
    public File createPayment(Payment payment, User currentUser) throws Exception {
        if (payment == null) throw new IllegalArgumentException("Pagamento é obrigatório.");
        BigDecimal amount = payment.getAmountValue();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor do pagamento deve ser maior que zero.");
        }
        if (payment.getSale() == null || payment.getSale().getId() == null) {
            throw new IllegalArgumentException("Factura/Venda associada ao pagamento é obrigatória.");
        }

        Sale managedSale = saleRepository.findById(payment.getSale().getId())
                .orElseThrow(() -> new IllegalArgumentException("Factura/Venda não encontrada para o pagamento."));
        payment.setSale(managedSale);

        BigDecimal total = managedSale.getTotalAmount() != null ? managedSale.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal currentPaid = managedSale.getPaidAmountValue() != null ? managedSale.getPaidAmountValue() : BigDecimal.ZERO;
        BigDecimal pendingBalance = total.subtract(currentPaid);

        if (pendingBalance.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Esta factura já se encontra totalmente liquidada.");
        }

        if (amount.compareTo(pendingBalance) > 0) {
            throw new IllegalArgumentException(String.format("Valor a pagar (%.2f MT) excede o saldo pendente da factura (%.2f MT).", amount, pendingBalance));
        }

        if (payment.getCreatedAt() == null) {
            payment.setCreatedAt(LocalDateTime.now());
        }

        Payment savedPayment = paymentRepository.save(payment);

        BigDecimal newPaid = currentPaid.add(amount);
        managedSale.setPaidAmountValue(newPaid);
        if (newPaid.compareTo(total) >= 0) {
            managedSale.setState(com.sgv.model.SaleState.PAGO.name());
        } else {
            managedSale.setState("PAGO_PARCIAL");
        }
        saleRepository.save(managedSale);

        // Abate automático na conta corrente do cliente
        if (managedSale.getCustomer() != null && managedSale.getCustomer().getId() != null) {
            customerRepository.findById(managedSale.getCustomer().getId()).ifPresent(cust -> {
                BigDecimal currentBal = cust.getBalanceAmount();
                cust.setBalanceAmount(currentBal.subtract(amount));
                customerRepository.save(cust);
            });
        }

        // Registo de entrada em caixa se o operador tiver sessão aberta
        if (cashSessionService != null && currentUser != null) {
            try {
                String method = payment.getMethod() != null ? payment.getMethod() : "RECEBIMENTO";
                String docRef = "Recibo FT " + managedSale.getSeries() + "/" + managedSale.getDocumentNumber();
                cashSessionService.registerMovement(currentUser, "IN", amount, docRef, method);
            } catch (Exception ignored) {
                // Caixa não aberto para este utilizador — ignora sem travar recebimento
            }
        }

        // Criar registo de alocação de recebimento
        PaymentAllocation alloc = new PaymentAllocation();
        alloc.setPayment(savedPayment);
        alloc.setSale(managedSale);
        alloc.setAmountValue(amount);
        alloc.setCreatedAt(LocalDateTime.now());
        paymentAllocationRepository.save(alloc);

        String origType = managedSale.getDocumentType();
        try {
            managedSale.setDocumentType(com.sgv.model.DocumentType.RECIBO.name());
            File pdf = saleDocumentService.generateDocument(managedSale);
            systemLogService.logUserAction(currentUser != null ? currentUser.getUsername() : "Sistema", "PAGAMENTO_CRIADO",
                    String.format("Recebimento de %.2f MT registado para a factura %s/%d", amount, managedSale.getSeries(), managedSale.getDocumentNumber()));
            return pdf;
        } finally {
            managedSale.setDocumentType(origType);
        }
    }
}
