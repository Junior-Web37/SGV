package com.sgv.service;

import com.sgv.entity.Customer;
import com.sgv.entity.CustomerAccountEntry;
import com.sgv.repository.CustomerAccountEntryRepository;
import com.sgv.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Livro de conta corrente do cliente — PUNTO ÚNICO DE ESCRITA do saldo
 * {@link Customer#getBalanceAmount()}.
 *
 * <p>Correcção do BUG-005/BUG-010: antes, cinco sítios de código alteravam
 * {@code customer.balance} de forma independente (venda a crédito, anulação,
 * pagamento, recibo sem factura e reconciliação) sem deixar rastro auditável —
 * o extrato era <i>derivado</i> das vendas e divergia do saldo real (notas de
 * crédito e recibos sem factura nem apareciam no extrato).</p>
 *
 * <p>Agora, cada variação de saldo grava, na mesma transação, um lançamento
 * imutável em {@code customer_account_entries} com valor sinalizado
 * (positivo = aumenta a dívida; negativo = diminui). A invariante
 * Σ(lançamentos) == saldo fica garantida estruturalmente: não é possível
 * alterar o saldo sem passar por {@link #recordEntry}.</p>
 *
 * <p>Chamar sempre dentro de uma transação em curso (os métodos dos serviços
 * que o usam já são {@code @Transactional}); em caso contrário, abre uma
 * transação própria.</p>
 */
@Service
public class CustomerAccountLedger {

    public static final String TYPE_CREDITO_SALE = "CREDITO_SALE";
    public static final String TYPE_PAYMENT = "PAYMENT";
    public static final String TYPE_RECEIPT_WITHOUT_SALE = "RECEIPT_WITHOUT_SALE";
    public static final String TYPE_RECONCILIATION = "RECONCILIATION";
    public static final String TYPE_CREDIT_NOTE = "CREDIT_NOTE";
    public static final String TYPE_ANNULMENT = "ANNULMENT";
    /** Ajuste manual de saldo feito pelo operador (crédito em conta corrente). */
    public static final String TYPE_ADJUSTMENT = "ADJUSTMENT";

    private final CustomerAccountEntryRepository entryRepository;
    private final CustomerRepository customerRepository;

    public CustomerAccountLedger(CustomerAccountEntryRepository entryRepository,
                                 CustomerRepository customerRepository) {
        this.entryRepository = entryRepository;
        this.customerRepository = customerRepository;
    }

    /**
     * Regista um lançamento no livro e actualiza o saldo do cliente na mesma
     * transação. Se alguma das duas escritas falhar, ambas revertem.
     *
     * @param customerId  cliente do lançamento
     * @param saleId      venda/factura de origem (pode ser null)
     * @param paymentId   pagamento/recebimento de origem (pode ser null)
     * @param entryType   um dos {@code TYPE_*} desta classe
     * @param signedAmount valor SINALIZADO: positivo aumenta a dívida,
     *                     negativo diminui; zero é rejeitado
     * @param reference   referência curta do documento (ex.: "FT A/12")
     * @param description descrição legível para o extrato
     * @return o lançamento gravado
     */
    @Transactional
    public CustomerAccountEntry recordEntry(Long customerId, Long saleId, Long paymentId,
                                            String entryType, BigDecimal signedAmount,
                                            String reference, String description) {
        if (customerId == null) {
            throw new IllegalArgumentException("Cliente inválido para lançamento de conta corrente.");
        }
        if (entryType == null || entryType.isBlank()) {
            throw new IllegalArgumentException("Tipo de lançamento inválido.");
        }
        BigDecimal amount = signedAmount != null ? signedAmount : BigDecimal.ZERO;
        if (amount.signum() == 0) {
            throw new IllegalArgumentException("Lançamento de valor zero não é permitido no livro de conta corrente.");
        }

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalStateException(
                        "Cliente inexistente (" + customerId + ") — o lançamento de conta corrente não pode ser gravado."));

        BigDecimal newBalance = customer.getBalanceAmount().add(amount);
        customer.setBalanceAmount(newBalance);
        customerRepository.save(customer);

        CustomerAccountEntry entry = new CustomerAccountEntry();
        entry.setCustomerId(customerId);
        entry.setSaleId(saleId);
        entry.setPaymentId(paymentId);
        entry.setEntryType(entryType);
        entry.setAmount(amount);
        entry.setReference(reference != null && reference.length() > 100 ? reference.substring(0, 100) : reference);
        entry.setDescription(description != null && description.length() > 500 ? description.substring(0, 500) : description);
        entry.setCreatedAt(LocalDateTime.now());
        return entryRepository.save(entry);
    }

    /** True quando o cliente já tem lançamentos no livro (post-migração). */
    public boolean hasEntries(Long customerId) {
        if (customerId == null) return false;
        return entryRepository.countByCustomerId(customerId) > 0;
    }
}
