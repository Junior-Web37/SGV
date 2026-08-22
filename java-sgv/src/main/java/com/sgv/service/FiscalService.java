package com.sgv.service;

import com.sgv.entity.Sale;
import com.sgv.repository.SaleRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.format.DateTimeFormatter;
import java.util.Formatter;

@Service
public class FiscalService {

    private final SaleNumberingService saleNumberingService;
    private final SaleRepository saleRepository;

    public FiscalService(SaleNumberingService saleNumberingService,
                         SaleRepository saleRepository) {
        this.saleNumberingService = saleNumberingService;
        this.saleRepository = saleRepository;
    }

    /**
     * Gera o "hashHash" SHA-256 do documento fiscal.
     * Input: branchId|series|documentNumber|total (formato AT Moçambique/CIVA).
     */
    public String generateHashHash(Sale sale) {
        String base = (sale.getBranch() != null ? String.valueOf(sale.getBranch().getId()) : "")
                + "|" + (sale.getSeries() != null ? sale.getSeries() : "A")
                + "|" + (sale.getDocumentNumber() != null ? sale.getDocumentNumber() : "1")
                + "|" + String.format(java.util.Locale.US, "%.2f", sale.getTotal() != null ? sale.getTotal() : 0.0);
        return sha256(base);
    }

    /**
     * Gera a assinatura digital fiscal encadeada (Previous Hash Chaining) conforme padrão AT:
     * InvoiceDate;SystemEntryDate;InvoiceNo;GrossTotal;PreviousHash
     */
    public String generateSignatureHash(Sale sale) {
        String invoiceDate = sale.getCreatedAt() != null
                ? sale.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                : java.time.LocalDate.now().toString();
        String systemEntryDate = sale.getCreatedAt() != null
                ? sale.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                : java.time.LocalDateTime.now().toString();
        String invoiceNo = (sale.getDocumentType() != null ? sale.getDocumentType() : "FT")
                + " " + (sale.getSeries() != null ? sale.getSeries() : "A")
                + "/" + (sale.getDocumentNumber() != null ? sale.getDocumentNumber() : "1");
        String grossTotal = String.format(java.util.Locale.US, "%.2f", sale.getTotal() != null ? sale.getTotal() : 0.0);

        // Obter hash do documento anterior para a mesma série e filial
        String previousHash = "";
        try {
            Long branchId = sale.getBranch() != null ? sale.getBranch().getId() : null;
            String series = sale.getSeries() != null ? sale.getSeries() : "A";
            var prevSales = saleRepository.findByBranchIdOrderByCreatedAtDesc(branchId, org.springframework.data.domain.PageRequest.of(0, 1));
            if (prevSales != null && !prevSales.isEmpty() && prevSales.getContent().get(0).getSignatureHash() != null) {
                previousHash = prevSales.getContent().get(0).getSignatureHash();
            }
        } catch (Exception ignored) {}

        String chainingInput = invoiceDate + ";" + systemEntryDate + ";" + invoiceNo + ";" + grossTotal + ";" + previousHash;
        return sha256(chainingInput);
    }

    /**
     * Atribui e retorna um novo hashControl sequencial por filial.
     */
    public Long assignHashControlForBranch(Long branchId) {
        return saleNumberingService.nextHashControl(branchId);
    }

    private String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
            return toHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private String toHex(byte[] bytes) {
        try (Formatter fmt = new Formatter()) {
            for (byte b : bytes) fmt.format("%02x", b);
            return fmt.toString();
        }
    }
}
