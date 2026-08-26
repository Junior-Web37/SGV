package com.sgv.service;

import com.sgv.entity.Customer;
import com.sgv.entity.CustomerAccountEntry;
import com.sgv.entity.Payment;
import com.sgv.entity.PaymentAllocation;
import com.sgv.entity.Sale;
import com.sgv.entity.User;
import com.sgv.repository.CustomerAccountEntryRepository;
import com.sgv.repository.CustomerRepository;
import com.sgv.repository.PaymentAllocationRepository;
import com.sgv.repository.PaymentRepository;
import com.sgv.repository.SaleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CustomerAccountService {

    private final CustomerRepository customerRepository;
    private final SaleRepository saleRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentAllocationRepository paymentAllocationRepository;
    private final SystemLogService systemLogService;
    private final CashSessionService cashSessionService;
    private final CustomerAccountEntryRepository customerAccountEntryRepository;
    private final CustomerAccountLedger customerAccountLedger;

    public CustomerAccountService(CustomerRepository customerRepository,
                                  SaleRepository saleRepository,
                                  PaymentRepository paymentRepository,
                                  PaymentAllocationRepository paymentAllocationRepository,
                                  SystemLogService systemLogService,
                                  CashSessionService cashSessionService,
                                  CustomerAccountEntryRepository customerAccountEntryRepository,
                                  CustomerAccountLedger customerAccountLedger) {
        this.customerRepository = customerRepository;
        this.saleRepository = saleRepository;
        this.paymentRepository = paymentRepository;
        this.paymentAllocationRepository = paymentAllocationRepository;
        this.systemLogService = systemLogService;
        this.cashSessionService = cashSessionService;
        this.customerAccountEntryRepository = customerAccountEntryRepository;
        this.customerAccountLedger = customerAccountLedger;
    }

    @Transactional
    public Payment recordReceipt(Customer customer, Sale sale, BigDecimal amount, String method, User currentUser) {
        if (customer == null || customer.getId() == null) {
            throw new IllegalArgumentException("Cliente inválido.");
        }
        if (customerRepository.findById(customer.getId()).isEmpty()) {
            throw new IllegalArgumentException("Cliente não encontrado.");
        }
        Sale managedSale = null;
        if (sale != null && sale.getId() != null) {
            managedSale = saleRepository.findById(sale.getId()).orElse(null);
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor do recebimento deve ser maior que zero.");
        }

        if (managedSale != null) {
            BigDecimal saleTotal = managedSale.getTotalAmount() != null ? managedSale.getTotalAmount() : BigDecimal.ZERO;
            BigDecimal currentPaid = managedSale.getPaidAmountValue() != null ? managedSale.getPaidAmountValue() : BigDecimal.ZERO;
            BigDecimal pendingBalance = saleTotal.subtract(currentPaid);

            if (pendingBalance.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalStateException("Esta factura já se encontra totalmente liquidada.");
            }
            if (amount.compareTo(pendingBalance) > 0) {
                throw new IllegalArgumentException(String.format("Valor a pagar (%.2f MT) excede o saldo pendente da factura (%.2f MT).", amount, pendingBalance));
            }
        }

        Payment payment = new Payment();
        payment.setAmountValue(amount);
        payment.setMethod(method != null ? method : "Numerário");
        payment.setCreatedAt(LocalDateTime.now());
        payment.setSale(managedSale);
        Payment savedPayment = paymentRepository.save(payment);

        // Correção do BUG-005/BUG-014: o recebimento abre LANÇAMENTO no livro de
        // conta corrente (saldo e lançamento na mesma transação). Sem factura
        // associada o lançamento é RECEIPT_WITHOUT_SALE — antes baixava o saldo
        // sem deixar qualquer rastro no extrato do cliente.
        String docRef = managedSale != null
                ? (managedSale.getSeries() != null ? managedSale.getSeries() : "A")
                        + "/" + (managedSale.getDocumentNumber() != null ? managedSale.getDocumentNumber() : managedSale.getId())
                : "S/DOC";
        String entryType = managedSale != null
                ? CustomerAccountLedger.TYPE_PAYMENT
                : CustomerAccountLedger.TYPE_RECEIPT_WITHOUT_SALE;
        String description = managedSale != null
                ? "Recebimento sobre " + docRef
                : "Recebimento sem factura (adiantação)";
        customerAccountLedger.recordEntry(customer.getId(),
                managedSale != null ? managedSale.getId() : null,
                savedPayment.getId(), entryType, amount.negate(), docRef, description);

        if (managedSale != null) {
            BigDecimal saleTotal = managedSale.getTotalAmount() != null ? managedSale.getTotalAmount() : BigDecimal.ZERO;
            BigDecimal paidAmount = managedSale.getPaidAmountValue() != null ? managedSale.getPaidAmountValue() : BigDecimal.ZERO;
            BigDecimal nextPaidAmount = paidAmount.add(amount);
            managedSale.setPaidAmountValue(nextPaidAmount);
            if (nextPaidAmount.compareTo(saleTotal) >= 0) {
                managedSale.setState("PAGO");
            } else {
                managedSale.setState("PAGO_PARCIAL");
            }
            saleRepository.save(managedSale);

            PaymentAllocation alloc = new PaymentAllocation();
            alloc.setPayment(savedPayment);
            alloc.setSale(managedSale);
            alloc.setAmountValue(amount);
            alloc.setCreatedAt(LocalDateTime.now());
            paymentAllocationRepository.save(alloc);

            // Correção do BUG-003: só numerário move a gaveta (recebimentos
            // electrónicos não são entradas de caixa física).
            if (cashSessionService != null && currentUser != null
                    && com.sgv.model.PaymentMethod.movesCashDrawer(method)) {
                try {
                    String docRef2 = "Recibo FT " + (managedSale.getSeries() != null ? managedSale.getSeries() : "A")
                            + "/" + (managedSale.getDocumentNumber() != null ? managedSale.getDocumentNumber() : managedSale.getId());
                    cashSessionService.registerMovement(currentUser, "IN", amount, docRef2, method != null ? method : "RECEBIMENTO");
                } catch (Exception ignored) {}
            }
        }

        systemLogService.logUserAction(currentUser != null ? currentUser.getUsername() : "Sistema", "CUSTOMER_RECEIPT", "Recebimento registado para o cliente.");
        return savedPayment;
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

        // Correção do BUG-005/010: a reconciliação abre LANÇAMENTO no livro de
        // conta corrente (saldo e lançamento na mesma transação).
        customerAccountLedger.recordEntry(customer.getId(), null, payment.getId(),
                CustomerAccountLedger.TYPE_RECONCILIATION, paymentAmount.negate(),
                "REC", "Reconciliação de conta corrente");

        // Registo de entrada em caixa na sessão do operador (mantido
        // incondicional: a reconciliação representa caixa física recebida,
        // independentemente do método nominal).
        if (cashSessionService != null && currentUser != null) {
            try {
                String docRef = "Reconciliação CC — " + (managedCustomer.getName() != null ? managedCustomer.getName() : "Cliente");
                cashSessionService.registerMovement(currentUser, "IN", paymentAmount, docRef, "RECONCILIACAO");
            } catch (Exception ignored) {
                // Se a sessão estiver fechada, não trava a reconciliação de conta corrente
            }
        }

        systemLogService.logUserAction(currentUser != null ? currentUser.getUsername() : "Sistema", "CUSTOMER_RECONCILE", "Reconciliação de crédito registada para o cliente.");
        return new ReconciliationResult(allocations, remaining);
    }

    /**
     * Gera o Extrato de Conta Corrente cronológico com saldo progressivo para o cliente.
     *
     * <p>Correcção do BUG-005/BUG-010: o extrato é agora construído a partir do
     * LIVRO de conta corrente ({@code customer_account_entries}) — a mesma
     * origem do saldo — e não mais derivado das vendas. Notas de crédito,
     * recibos sem factura, reconciliações e anulações aparecem com o valor
     * real, e o saldo progressivo termina exactamente no saldo do cliente
     * (invariante verificada por {@code ReportParityTest}).</p>
     *
     * <p>Clientes sem lançamentos (base de dados criada antes da migração V28)
     * continuam a ver a visão legada derivada das vendas.</p>
     */
    @Transactional(readOnly = true)
    public List<CustomerStatementEntry> getCustomerStatement(Long customerId) {
        if (customerId == null) return List.of();
        Customer customer = customerRepository.findById(customerId).orElse(null);
        if (customer == null) return List.of();

        List<CustomerAccountEntry> entries = customerAccountEntryRepository.findByCustomerIdOrderByCreatedAtAsc(customerId);
        if (entries == null || entries.isEmpty()) {
            return legacyStatementFromSales(customerId);
        }

        // Saldo de abertura = saldo actual − Σ(lançamentos): cobre saldos que
        // existiam antes da migração V28 (o livro começou a meio do histórico).
        BigDecimal sumEntries = BigDecimal.ZERO;
        for (CustomerAccountEntry e : entries) {
            if (e.getAmount() != null) sumEntries = sumEntries.add(e.getAmount());
        }
        BigDecimal opening = customer.getBalanceAmount().subtract(sumEntries);

        List<CustomerStatementEntry> statement = new java.util.ArrayList<>();
        if (opening.signum() != 0) {
            statement.add(new CustomerStatementEntry(
                    LocalDateTime.now(), "AB", "—",
                    opening.signum() > 0 ? opening : BigDecimal.ZERO,
                    opening.signum() < 0 ? opening.negate() : BigDecimal.ZERO,
                    opening, "Saldo de abertura (antes do livro de conta corrente)"
            ));
        }

        BigDecimal running = opening;
        for (CustomerAccountEntry e : entries) {
            BigDecimal amount = e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO;
            running = running.add(amount);
            String ref = e.getReference() != null && !e.getReference().isBlank() ? e.getReference() : "—";
            String description = e.getDescription() != null ? e.getDescription() : e.getEntryType();
            String type;
            String document;
            BigDecimal debit = BigDecimal.ZERO;
            BigDecimal credit = BigDecimal.ZERO;
            switch (e.getEntryType() != null ? e.getEntryType() : "") {
                case CustomerAccountLedger.TYPE_CREDITO_SALE:
                    type = "FT";
                    document = "FT " + ref;
                    debit = amount.signum() > 0 ? amount : BigDecimal.ZERO;
                    credit = amount.signum() < 0 ? amount.negate() : BigDecimal.ZERO;
                    break;
                case CustomerAccountLedger.TYPE_CREDIT_NOTE:
                    type = "NC";
                    document = "NC " + ref;
                    credit = amount.abs();
                    description = description != null ? description : "Nota de Crédito / Devolução";
                    break;
                case CustomerAccountLedger.TYPE_ANNULMENT:
                    type = "ANUL";
                    document = "ANUL " + ref;
                    credit = amount.abs();
                    break;
                case CustomerAccountLedger.TYPE_RECEIPT_WITHOUT_SALE:
                    type = "RC";
                    document = "RC (adiantação)";
                    credit = amount.abs();
                    break;
                case CustomerAccountLedger.TYPE_RECONCILIATION:
                    type = "RC";
                    document = "Reconciliação";
                    credit = amount.abs();
                    break;
                case CustomerAccountLedger.TYPE_ADJUSTMENT:
                    type = "AJ";
                    document = "Ajuste manual";
                    credit = amount.abs();
                    break;
                case CustomerAccountLedger.TYPE_PAYMENT:
                default:
                    type = "RC";
                    document = "Recibo " + ref;
                    credit = amount.abs();
                    break;
            }
            statement.add(new CustomerStatementEntry(
                    e.getCreatedAt() != null ? e.getCreatedAt() : LocalDateTime.now(),
                    type, document, debit, credit, running, description
            ));
        }
        return statement;
    }

    /**
     * Visão legada (pré-V28): extrato derivado das vendas. Mantida apenas como
     * fallback para clientes que ainda não têm lançamentos no livro de conta
     * corrente. Não reflecte NC/recebimentos sem factura — ver BUG-005.
     */
    private List<CustomerStatementEntry> legacyStatementFromSales(Long customerId) {
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
