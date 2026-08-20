package com.sgv.service;

import com.sgv.entity.Customer;
import com.sgv.entity.Payment;
import com.sgv.entity.Sale;
import com.sgv.entity.User;
import com.sgv.repository.CustomerRepository;
import com.sgv.repository.PaymentAllocationRepository;
import com.sgv.repository.PaymentRepository;
import com.sgv.repository.SaleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class CustomerAccountService {

    private final CustomerRepository customerRepository;
    private final SaleRepository saleRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentAllocationRepository paymentAllocationRepository;
    private final SystemLogService systemLogService;

    public CustomerAccountService(CustomerRepository customerRepository,
                                  SaleRepository saleRepository,
                                  PaymentRepository paymentRepository,
                                  PaymentAllocationRepository paymentAllocationRepository,
                                  SystemLogService systemLogService) {
        this.customerRepository = customerRepository;
        this.saleRepository = saleRepository;
        this.paymentRepository = paymentRepository;
        this.paymentAllocationRepository = paymentAllocationRepository;
        this.systemLogService = systemLogService;
    }

    @Transactional
    public Payment recordReceipt(Customer customer, Sale sale, BigDecimal amount, String method, User currentUser) {
        if (customer == null || customer.getId() == null) {
            throw new IllegalArgumentException("Cliente inválido.");
        }
        if (sale != null && sale.getId() != null) {
            Sale managedSale = saleRepository.findById(sale.getId()).orElse(null);
            if (managedSale != null) {
                sale = managedSale;
            }
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor do recebimento deve ser maior que zero.");
        }

        Customer managedCustomer = customerRepository.findById(customer.getId()).orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado."));
        managedCustomer.setBalanceAmount(managedCustomer.getBalanceAmount().subtract(amount));
        customerRepository.save(managedCustomer);

        Payment payment = new Payment();
        payment.setAmountValue(amount);
        payment.setMethod(method);
        payment.setCreatedAt(LocalDateTime.now());
        payment.setSale(sale);
        paymentRepository.save(payment);

        if (sale != null) {
            BigDecimal saleTotal = sale.getTotalAmount() != null ? sale.getTotalAmount() : BigDecimal.ZERO;
            BigDecimal paidAmount = sale.getPaidAmountValue() != null ? sale.getPaidAmountValue() : BigDecimal.ZERO;
            BigDecimal nextPaidAmount = paidAmount.add(amount);
            sale.setPaidAmountValue(nextPaidAmount);
            if (nextPaidAmount.compareTo(saleTotal) >= 0) {
                sale.setState("PAGO");
            }
            saleRepository.save(sale);
        }

        systemLogService.logUserAction(currentUser != null ? currentUser.getUsername() : "Sistema", "CUSTOMER_RECEIPT", "Recebimento registado para o cliente.");
        return payment;
    }

    @Transactional
    public ReconciliationResult reconcileCustomerCredits(Customer customer, BigDecimal amount, User currentUser) {
        if (customer == null || customer.getId() == null) {
            throw new IllegalArgumentException("Cliente inválido.");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor a reconciliar deve ser maior que zero.");
        }

        Customer managedCustomer = customerRepository.findById(customer.getId())
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado."));
        BigDecimal paymentAmount = amount;
        LocalDateTime now = LocalDateTime.now();

        Payment payment = new Payment();
        payment.setAmountValue(paymentAmount);
        payment.setMethod("RECONCILIACAO");
        payment.setCreatedAt(now);
        paymentRepository.save(payment);

        BigDecimal remaining = paymentAmount;
        java.util.List<Sale> pending = saleRepository.findPendingByCustomerId(managedCustomer.getId());
        java.util.List<com.sgv.entity.PaymentAllocation> allocations = new java.util.ArrayList<>();

        if (pending != null && !pending.isEmpty()) {
            for (Sale s : pending.stream().sorted(java.util.Comparator.comparing(Sale::getCreatedAt)).toList()) {
                if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
                BigDecimal saleTotal = s.getTotalAmount() != null ? s.getTotalAmount() : BigDecimal.ZERO;
                BigDecimal paid = s.getPaidAmountValue() != null ? s.getPaidAmountValue() : BigDecimal.ZERO;
                BigDecimal due = saleTotal.subtract(paid);
                if (due.compareTo(BigDecimal.ZERO) <= 0) continue;

                BigDecimal toAllocate = due.min(remaining);
                com.sgv.entity.PaymentAllocation allocation = new com.sgv.entity.PaymentAllocation();
                allocation.setPayment(payment);
                allocation.setSale(s);
                allocation.setAmountValue(toAllocate);
                allocation.setCreatedAt(now);
                paymentAllocationRepository.save(allocation);
                allocations.add(allocation);

                s.setPaidAmountValue(paid.add(toAllocate));
                if (s.getPaidAmountValue().compareTo(saleTotal) >= 0) {
                    s.setState("PAGO");
                }
                saleRepository.save(s);

                remaining = remaining.subtract(toAllocate);
            }
        }

        managedCustomer.setBalanceAmount(managedCustomer.getBalanceAmount().subtract(paymentAmount));
        customerRepository.save(managedCustomer);

        systemLogService.logUserAction(currentUser != null ? currentUser.getUsername() : "Sistema", "CUSTOMER_RECONCILE", "Reconciliação de crédito registada para o cliente.");
        return new ReconciliationResult(allocations, remaining);
    }

    /**
     * Gera o Extrato de Conta Corrente cronológico com saldo progressivo para o cliente.
     */
    @Transactional(readOnly = true)
    public List<CustomerStatementEntry> getCustomerStatement(Long customerId) {
        if (customerId == null) return List.of();
        Customer customer = customerRepository.findById(customerId).orElse(null);
        if (customer == null) return List.of();

        List<CustomerStatementEntry> statement = new java.util.ArrayList<>();
        List<Sale> sales = saleRepository.findAllByCustomerId(customerId);

        BigDecimal runningBalance = BigDecimal.ZERO;
        // Ordenar por data cronológica crescente
        var sortedSales = sales.stream().sorted(java.util.Comparator.comparing(Sale::getCreatedAt)).toList();

        for (Sale s : sortedSales) {
            String doc = (s.getDocumentType() != null ? s.getDocumentType() : "FT") + " "
                    + (s.getSeries() != null ? s.getSeries() : "A") + "/" + s.getDocumentNumber();
            BigDecimal total = s.getTotalAmount() != null ? s.getTotalAmount() : BigDecimal.ZERO;

            if ("ANULADA".equals(s.getState())) {
                continue; // Anuladas não movimentam saldo
            }

            if ("NC".equals(s.getDocumentType())) {
                // Nota de crédito diminui a dívida
                runningBalance = runningBalance.subtract(total.abs());
                statement.add(new CustomerStatementEntry(
                        s.getCreatedAt(), "NC", doc, BigDecimal.ZERO, total.abs(), runningBalance, "Nota de Crédito / Devolução"
                ));
            } else if ("CREDITO".equalsIgnoreCase(s.getPaymentMethod()) || "EMITIDA".equals(s.getState()) || "PAGO_PARCIAL".equals(s.getState())) {
                // Fatura a crédito aumenta a dívida
                runningBalance = runningBalance.add(total);
                statement.add(new CustomerStatementEntry(
                        s.getCreatedAt(), "FT", doc, total, BigDecimal.ZERO, runningBalance, "Factura a Crédito"
                ));

                // Se houver valor já pago
                BigDecimal paid = s.getPaidAmountValue() != null ? s.getPaidAmountValue() : BigDecimal.ZERO;
                if (paid.compareTo(BigDecimal.ZERO) > 0) {
                    runningBalance = runningBalance.subtract(paid);
                    statement.add(new CustomerStatementEntry(
                            s.getCreatedAt(), "RC", "Recibo " + doc, BigDecimal.ZERO, paid, runningBalance, "Amortização / Liquidação"
                    ));
                }
            } else {
                // Venda a pronto (Débito e Crédito imediatos)
                statement.add(new CustomerStatementEntry(
                        s.getCreatedAt(), "VD", doc, total, total, runningBalance, "Venda a Pronto (" + s.getPaymentMethod() + ")"
                ));
            }
        }
        return statement;
    }

    public static class CustomerStatementEntry {
        private final LocalDateTime date;
        private final String type;
        private final String document;
        private final BigDecimal debit;
        private final BigDecimal credit;
        private final BigDecimal runningBalance;
        private final String description;

        public CustomerStatementEntry(LocalDateTime date, String type, String document, BigDecimal debit, BigDecimal credit, BigDecimal runningBalance, String description) {
            this.date = date;
            this.type = type;
            this.document = document;
            this.debit = debit;
            this.credit = credit;
            this.runningBalance = runningBalance;
            this.description = description;
        }

        public LocalDateTime getDate() { return date; }
        public String getType() { return type; }
        public String getDocument() { return document; }
        public BigDecimal getDebit() { return debit; }
        public BigDecimal getCredit() { return credit; }
        public BigDecimal getRunningBalance() { return runningBalance; }
        public String getDescription() { return description; }
    }
}
