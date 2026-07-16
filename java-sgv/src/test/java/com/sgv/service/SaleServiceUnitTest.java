package com.sgv.service;

import com.sgv.entity.*;
import com.sgv.entity.Role;
import com.sgv.entity.User;
import com.sgv.repository.SaleItemRepository;
import com.sgv.repository.SaleRepository;
import com.sgv.service.StockBranchService;
import org.junit.jupiter.api.BeforeEach;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class SaleServiceUnitTest {

    private SaleRepository saleRepository;
    private SaleItemRepository saleItemRepository;
    private StockBranchService stockBranchService;
    private SaleNumberingService saleNumberingService;
    private SaleDocumentService saleDocumentService;
    private ThermalPrintService thermalPrintService;
    private SaleService saleService;

    @BeforeEach
    void setUp() {
        saleRepository = mock(SaleRepository.class);
        saleItemRepository = mock(SaleItemRepository.class);
        stockBranchService = mock(StockBranchService.class);
        saleNumberingService = mock(SaleNumberingService.class);
        saleDocumentService = mock(SaleDocumentService.class);
        thermalPrintService = mock(ThermalPrintService.class);

        saleService = new SaleService(saleRepository, saleItemRepository, stockBranchService,
                saleNumberingService, saleDocumentService, thermalPrintService);
    }

    @Test
    void processAndSave_success_decrementsStock_andGeneratesPdf() throws Exception {
        Branch branch = new Branch(); branch.setId(10L); branch.setName("B");
        Product product = new Product(); product.setId(5L); product.setCode("P01"); product.setService(false);
        SaleItem item = new SaleItem(); item.setProduct(product); item.setProductCode("P01"); item.setQtyAmount(BigDecimal.valueOf(2.0));
        item.setLineBaseAmount(BigDecimal.valueOf(200.0)); item.setLineIceAmount(BigDecimal.ZERO); item.setLineTaxAmount(BigDecimal.valueOf(34.0)); item.setLineTotalAmount(BigDecimal.valueOf(234.0));

        Sale sale = new Sale();
        sale.setBranch(branch);
        sale.setItems(new ArrayList<>());
        sale.getItems().add(item);

        StockBranch sb = new StockBranch(); sb.setId(1L); sb.setBranch(branch); sb.setProduct(product); sb.setStockCurrentAmount(BigDecimal.valueOf(10.0));

        when(saleNumberingService.nextDocumentNumber(any(), anyString(), anyString())).thenReturn(123L);
        when(saleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(stockBranchService.getCurrentStock(eq(branch), eq(product))).thenReturn(BigDecimal.valueOf(10.0));
        when(stockBranchService.decreaseStock(eq(branch), eq(product), eq(BigDecimal.valueOf(2.0)), anyString(), eq("VENDA"), any(User.class)))
                .thenReturn(sb);
        File tmp = File.createTempFile("test-sale", ".pdf");
        when(thermalPrintService.printReceipt(any())).thenReturn(tmp);

        User currentUser = new User();
        Role salesRole = new Role();
        salesRole.setName("CAIXA");
        salesRole.setPermissions(Set.of("VENDAS:CREATE"));
        currentUser.setRoles(Set.of(salesRole));

        File out = saleService.processAndSave(sale, currentUser);

        assertNotNull(out);

        verify(stockBranchService).decreaseStock(eq(branch), eq(product), eq(BigDecimal.valueOf(2.0)), anyString(), eq("VENDA"), any(User.class));
    }

    @Test
    void processAndSave_insufficientStock_throws() {
        Branch branch = new Branch(); branch.setId(3L);
        Product product = new Product(); product.setId(9L); product.setService(false);
        SaleItem item = new SaleItem(); item.setProduct(product); item.setQtyAmount(BigDecimal.valueOf(5.0));
        Sale sale = new Sale(); sale.setBranch(branch); sale.setItems(List.of(item));

        StockBranch sb = new StockBranch(); sb.setProduct(product); sb.setBranch(branch); sb.setStockCurrentAmount(BigDecimal.valueOf(2.0));
        when(stockBranchService.getCurrentStock(eq(branch), eq(product))).thenReturn(BigDecimal.valueOf(2.0));

        when(saleNumberingService.nextDocumentNumber(any(), anyString(), anyString())).thenReturn(1L);

        User currentUser = new User();
        Role salesRole = new Role();
        salesRole.setName("CAIXA");
        salesRole.setPermissions(Set.of("VENDAS:CREATE"));
        currentUser.setRoles(Set.of(salesRole));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> saleService.processAndSave(sale, currentUser));
        assertTrue(ex.getMessage().contains("Estoque insuficiente"));
    }
}
