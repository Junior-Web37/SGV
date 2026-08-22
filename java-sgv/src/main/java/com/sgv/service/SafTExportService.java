package com.sgv.service;

import com.sgv.entity.*;
import com.sgv.repository.CustomerRepository;
import com.sgv.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Serviço de exportação do ficheiro oficial SAF-T Moçambique (Standard Audit File for Tax).
 * Estrutura em conformidade com as diretrizes da Autoridade Tributária (AT / CIVA / DGI).
 */
@Service
public class SafTExportService {

    private final AppConfigService appConfigService;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    public SafTExportService(AppConfigService appConfigService,
                             CustomerRepository customerRepository,
                             ProductRepository productRepository) {
        this.appConfigService = appConfigService;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
    }

    public Path exportSalesToXml(List<Sale> sales, Path outputPath) throws IOException {
        AppConfig cfg = appConfigService.get();
        String companyName = cfg.getCompanyName() != null ? cfg.getCompanyName() : "Empresa Comercial SGV";
        String companyNuit = cfg.getCompanyNuit() != null ? cfg.getCompanyNuit() : "400123456";
        String companyAddress = cfg.getCompanyAddress() != null ? cfg.getCompanyAddress() : "Av. 24 de Julho, Maputo";
        String certNum = cfg.getSoftwareCertNumber() != null ? cfg.getSoftwareCertNumber() : "CERT-AT-2026/0042";

        int year = LocalDate.now().getYear();
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);
        if (sales != null && !sales.isEmpty()) {
            startDate = sales.stream().filter(s -> s.getCreatedAt() != null).map(s -> s.getCreatedAt().toLocalDate()).min(LocalDate::compareTo).orElse(startDate);
            endDate = sales.stream().filter(s -> s.getCreatedAt() != null).map(s -> s.getCreatedAt().toLocalDate()).max(LocalDate::compareTo).orElse(endDate);
        }

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<AuditFile xmlns=\"urn:OECD:StandardAuditFile-Tax:MZ_1.01\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n");

        // ─── 1. HEADER ──────────────────────────────────────────────────────────
        xml.append("  <Header>\n");
        xml.append("    <AuditFileVersion>1.01_01</AuditFileVersion>\n");
        xml.append("    <CompanyID>").append(escape(companyNuit)).append("</CompanyID>\n");
        xml.append("    <TaxRegistrationNumber>").append(escape(companyNuit)).append("</TaxRegistrationNumber>\n");
        xml.append("    <TaxAccountingBasis>F</TaxAccountingBasis>\n"); // Faturação
        xml.append("    <CompanyName>").append(escape(companyName)).append("</CompanyName>\n");
        xml.append("    <CompanyAddress>\n");
        xml.append("      <AddressDetail>").append(escape(companyAddress)).append("</AddressDetail>\n");
        xml.append("      <City>Maputo</City>\n");
        xml.append("      <Country>MZ</Country>\n");
        xml.append("    </CompanyAddress>\n");
        xml.append("    <FiscalYear>").append(year).append("</FiscalYear>\n");
        xml.append("    <StartDate>").append(startDate).append("</StartDate>\n");
        xml.append("    <EndDate>").append(endDate).append("</EndDate>\n");
        xml.append("    <CurrencyCode>MZN</CurrencyCode>\n");
        xml.append("    <DateCreated>").append(LocalDate.now()).append("</DateCreated>\n");
        xml.append("    <TaxEntity>Global</TaxEntity>\n");
        xml.append("    <ProductCompanyTaxID>").append(escape(companyNuit)).append("</ProductCompanyTaxID>\n");
        xml.append("    <SoftwareCertificateNumber>").append(escape(certNum)).append("</SoftwareCertificateNumber>\n");
        xml.append("    <ProductID>SGV Desktop / Facturacao</ProductID>\n");
        xml.append("    <ProductVersion>1.0.0</ProductVersion>\n");
        xml.append("  </Header>\n");

        // ─── 2. MASTER FILES ────────────────────────────────────────────────────
        xml.append("  <MasterFiles>\n");

        // Clientes
        Set<Customer> customers = new HashSet<>(customerRepository.findAll());
        for (Customer c : customers) {
            xml.append("    <Customer>\n");
            xml.append("      <CustomerID>").append(escape(c.getCode() != null ? c.getCode() : "CLI-" + c.getId())).append("</CustomerID>\n");
            xml.append("      <AccountID>Desconhecido</AccountID>\n");
            xml.append("      <CustomerTaxID>").append(escape(c.getNuit() != null ? c.getNuit() : "999999999")).append("</CustomerTaxID>\n");
            xml.append("      <CompanyName>").append(escape(c.getName() != null ? c.getName() : "Consumidor Final")).append("</CompanyName>\n");
            xml.append("      <BillingAddress>\n");
            xml.append("        <AddressDetail>").append(escape(c.getAddress() != null ? c.getAddress() : "Maputo")).append("</AddressDetail>\n");
            xml.append("        <City>Maputo</City>\n");
            xml.append("        <Country>MZ</Country>\n");
            xml.append("      </BillingAddress>\n");
            xml.append("      <SelfBillingIndicator>0</SelfBillingIndicator>\n");
            xml.append("    </Customer>\n");
        }

        // Artigos
        Set<Product> products = new HashSet<>(productRepository.findAll());
        for (Product p : products) {
            xml.append("    <Product>\n");
            xml.append("      <ProductType>").append(Boolean.TRUE.equals(p.getService()) ? "S" : "P").append("</ProductType>\n");
            xml.append("      <ProductCode>").append(escape(p.getCode() != null ? p.getCode() : "ART-" + p.getId())).append("</ProductCode>\n");
            xml.append("      <ProductDescription>").append(escape(p.getName() != null ? p.getName() : "Artigo")).append("</ProductDescription>\n");
            xml.append("      <ProductNumberCode>").append(escape(p.getCode())).append("</ProductNumberCode>\n");
            xml.append("    </Product>\n");
        }

        // Tabela de Impostos (TaxTable)
        xml.append("    <TaxTable>\n");
        xml.append("      <TaxTableEntry>\n");
        xml.append("        <TaxType>IVA</TaxType>\n");
        xml.append("        <TaxCountryRegion>MZ</TaxCountryRegion>\n");
        xml.append("        <TaxCode>NOR</TaxCode>\n");
        xml.append("        <Description>Taxa Normal</Description>\n");
        xml.append("        <TaxPercentage>16.00</TaxPercentage>\n");
        xml.append("      </TaxTableEntry>\n");
        xml.append("      <TaxTableEntry>\n");
        xml.append("        <TaxType>IVA</TaxType>\n");
        xml.append("        <TaxCountryRegion>MZ</TaxCountryRegion>\n");
        xml.append("        <TaxCode>ISE</TaxCode>\n");
        xml.append("        <Description>Isento Artigo 9 CIVA</Description>\n");
        xml.append("        <TaxPercentage>0.00</TaxPercentage>\n");
        xml.append("      </TaxTableEntry>\n");
        xml.append("    </TaxTable>\n");

        xml.append("  </MasterFiles>\n");

        // ─── 3. SOURCE DOCUMENTS ────────────────────────────────────────────────
        xml.append("  <SourceDocuments>\n");
        xml.append("    <SalesInvoices>\n");

        List<Sale> validSales = sales != null ? sales : List.of();
        double totalCredit = validSales.stream().mapToDouble(s -> s.getTotal() != null ? s.getTotal() : 0.0).sum();

        xml.append("      <NumberOfEntries>").append(validSales.size()).append("</NumberOfEntries>\n");
        xml.append("      <TotalDebit>0.00</TotalDebit>\n");
        xml.append("      <TotalCredit>").append(String.format(Locale.US, "%.2f", totalCredit)).append("</TotalCredit>\n");

        for (Sale s : validSales) {
            String docType = s.getDocumentType() != null ? s.getDocumentType() : "FT";
            String series = s.getSeries() != null ? s.getSeries() : "A";
            long num = s.getDocumentNumber() != null ? s.getDocumentNumber() : (s.getId() != null ? s.getId() : 1L);
            String invoiceNo = docType + " " + series + "/" + num;
            String status = "ANULADA".equals(s.getState()) ? "A" : "N";
            String custId = s.getCustomer() != null && s.getCustomer().getCode() != null
                    ? s.getCustomer().getCode() : "CLI-0001";

            String createdDate = s.getCreatedAt() != null ? s.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : LocalDate.now().toString();
            String createdDateTime = s.getCreatedAt() != null ? s.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")) : LocalDateTime.now().toString();

            xml.append("      <Invoice>\n");
            xml.append("        <InvoiceNo>").append(escape(invoiceNo)).append("</InvoiceNo>\n");
            xml.append("        <DocumentStatus>\n");
            xml.append("          <InvoiceStatus>").append(status).append("</InvoiceStatus>\n");
            xml.append("          <InvoiceStatusDate>").append(createdDateTime).append("</InvoiceStatusDate>\n");
            xml.append("          <SourceID>admin</SourceID>\n");
            xml.append("          <SourceBilling>P</SourceBilling>\n");
            xml.append("        </DocumentStatus>\n");
            xml.append("        <Hash>").append(escape(s.getSignatureHash() != null ? s.getSignatureHash() : "0")).append("</Hash>\n");
            xml.append("        <HashControl>").append(s.getHashControl() != null ? s.getHashControl() : 1).append("</HashControl>\n");
            xml.append("        <Period>").append(s.getCreatedAt() != null ? s.getCreatedAt().getMonthValue() : 1).append("</Period>\n");
            xml.append("        <InvoiceDate>").append(createdDate).append("</InvoiceDate>\n");
            xml.append("        <InvoiceType>").append(escape(docType)).append("</InvoiceType>\n");
            xml.append("        <SpecialRegimes>\n");
            xml.append("          <SelfBillingIndicator>0</SelfBillingIndicator>\n");
            xml.append("          <CashVATSchemeIndicator>0</CashVATSchemeIndicator>\n");
            xml.append("          <ThirdPartiesBillingIndicator>0</ThirdPartiesBillingIndicator>\n");
            xml.append("        </SpecialRegimes>\n");
            xml.append("        <SourceID>admin</SourceID>\n");
            xml.append("        <SystemEntryDate>").append(createdDateTime).append("</SystemEntryDate>\n");
            xml.append("        <CustomerID>").append(escape(custId)).append("</CustomerID>\n");

            // Linhas de Artigos
            int lineIdx = 1;
            if (s.getItems() != null && !s.getItems().isEmpty()) {
                for (SaleItem it : s.getItems()) {
                    String pCode = it.getProductCode() != null ? it.getProductCode() : (it.getProduct() != null ? it.getProduct().getCode() : "ART-01");
                    String pDesc = it.getDescription() != null ? it.getDescription() : (it.getProduct() != null ? it.getProduct().getName() : "Artigo");
                    double qty = it.getQty() != null ? it.getQty() : 1.0;
                    double unitPrice = it.getUnitPrice() != null ? it.getUnitPrice() : 0.0;
                    double lineBase = it.getLineBase() != null ? it.getLineBase() : (qty * unitPrice);
                    double taxRate = it.getTaxRate() != null ? it.getTaxRate() : 16.0;

                    xml.append("        <Line>\n");
                    xml.append("          <LineNumber>").append(lineIdx++).append("</LineNumber>\n");
                    xml.append("          <ProductCode>").append(escape(pCode)).append("</ProductCode>\n");
                    xml.append("          <ProductDescription>").append(escape(pDesc)).append("</ProductDescription>\n");
                    xml.append("          <Quantity>").append(String.format(Locale.US, "%.4f", qty)).append("</Quantity>\n");
                    xml.append("          <UnitOfMeasure>").append(escape(it.getUnit() != null ? it.getUnit() : "UN")).append("</UnitOfMeasure>\n");
                    xml.append("          <UnitPrice>").append(String.format(Locale.US, "%.4f", unitPrice)).append("</UnitPrice>\n");
                    xml.append("          <TaxPointDate>").append(createdDate).append("</TaxPointDate>\n");
                    xml.append("          <Description>").append(escape(pDesc)).append("</Description>\n");
                    xml.append("          <CreditAmount>").append(String.format(Locale.US, "%.2f", lineBase)).append("</CreditAmount>\n");
                    xml.append("          <Tax>\n");
                    xml.append("            <TaxType>IVA</TaxType>\n");
                    xml.append("            <TaxCountryRegion>MZ</TaxCountryRegion>\n");
                    xml.append("            <TaxCode>").append(taxRate > 0 ? "NOR" : "ISE").append("</TaxCode>\n");
                    xml.append("            <TaxPercentage>").append(String.format(Locale.US, "%.2f", taxRate)).append("</TaxPercentage>\n");
                    xml.append("          </Tax>\n");
                    if (taxRate == 0.0) {
                        xml.append("          <TaxExemptionReason>Artigo 9 do CIVA</TaxExemptionReason>\n");
                        xml.append("          <TaxExemptionCode>M02</TaxExemptionCode>\n");
                    }
                    xml.append("          <SettlementAmount>0.00</SettlementAmount>\n");
                    xml.append("        </Line>\n");
                }
            }

            // Totais da Factura
            double sub = s.getSubtotal() != null ? s.getSubtotal() : 0.0;
            double tax = s.getTotalTax() != null ? s.getTotalTax() : 0.0;
            double tot = s.getTotal() != null ? s.getTotal() : 0.0;

            xml.append("        <DocumentTotals>\n");
            xml.append("          <TaxPayable>").append(String.format(Locale.US, "%.2f", tax)).append("</TaxPayable>\n");
            xml.append("          <NetTotal>").append(String.format(Locale.US, "%.2f", sub)).append("</NetTotal>\n");
            xml.append("          <GrossTotal>").append(String.format(Locale.US, "%.2f", tot)).append("</GrossTotal>\n");
            xml.append("          <Settlement>\n");
            xml.append("            <SettlementAmount>0.00</SettlementAmount>\n");
            xml.append("          </Settlement>\n");
            xml.append("          <Payment>\n");
            xml.append("            <PaymentMechanism>").append(escape(s.getPaymentMethod() != null ? s.getPaymentMethod() : "NU")).append("</PaymentMechanism>\n");
            xml.append("            <PaymentAmount>").append(String.format(Locale.US, "%.2f", tot)).append("</PaymentAmount>\n");
            xml.append("            <PaymentDate>").append(createdDate).append("</PaymentDate>\n");
            xml.append("          </Payment>\n");
            xml.append("        </DocumentTotals>\n");
            xml.append("      </Invoice>\n");
        }

        xml.append("    </SalesInvoices>\n");
        xml.append("  </SourceDocuments>\n");
        xml.append("</AuditFile>\n");

        if (outputPath.getParent() != null) {
            Files.createDirectories(outputPath.toAbsolutePath().getParent());
        }
        Files.writeString(outputPath, xml.toString(), StandardCharsets.UTF_8);
        return outputPath;
    }

    private String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
