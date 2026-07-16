package com.sgv.service;

import com.sgv.entity.Sale;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

@Service
public class FiscalService {

    private final SaleNumberingService saleNumberingService;

    public FiscalService(SaleNumberingService saleNumberingService) {
        this.saleNumberingService = saleNumberingService;
    }

    /**
     * Gera o "hashHash" MD5 simples do documento — placeholder para o cálculo AT.
     */
    public String generateHashHash(Sale sale) {
        String base = (sale.getBranch() != null ? String.valueOf(sale.getBranch().getId()) : "")
                + "|" + (sale.getSeries() != null ? sale.getSeries() : "")
                + "|" + (sale.getDocumentNumber() != null ? sale.getDocumentNumber() : "")
                + "|" + String.format("%.2f", sale.getTotal() != null ? sale.getTotal() : 0.0);
        return md5(base);
    }

    /**
     * Gera assinatura SHA-256 do documento — placeholder.
     */
    public String generateSignatureHash(Sale sale) {
        String base = sale.getCustomerNuit() + "|" + sale.getCreatedAt();
        return sha256(base);
    }

    /**
     * Atribui e retorna um novo hashControl sequencial por filial.
     */
    public Long assignHashControlForBranch(Long branchId) {
        return saleNumberingService.nextHashControl(branchId);
    }

    private String md5(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
            return toHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
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
