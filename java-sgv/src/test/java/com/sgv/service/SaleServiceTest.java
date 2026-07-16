package com.sgv.service;

import com.sgv.entity.Branch;
import com.sgv.entity.Product;
import com.sgv.entity.Role;
import com.sgv.entity.Sale;
import com.sgv.entity.SaleItem;
import com.sgv.entity.StockBranch;
import com.sgv.entity.User;
import com.sgv.repository.SaleRepository;
import com.sgv.repository.SaleItemRepository;
import com.sgv.service.StockBranchService;
import org.junit.jupiter.api.Test;

import java.util.Set;

import java.io.File;
import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SaleServiceTest {

    @Test
    void processAndSave_assignsDocumentNumber_decrementsStock_andGeneratesDocument() throws Exception {
        SaleRepository saleRepository = mock(SaleRepository.class);
        SaleItemRepository saleItemRepository = mock(SaleItemRepository.class);
        StockBranchService stockBranchService = mock(StockBranchService.class);
        SaleNumberingService numberingService = mock(SaleNumberingService.class);
        SaleDocumentService saleDocumentService = mock(SaleDocumentService.class);
        ThermalPrintService thermalPrintService = mock(ThermalPrintService.class);

        SaleService saleService = new SaleService(
                saleRepository,
                saleItemRepository,
                stockBranchService,
                numberingService,
                saleDocumentService,
                thermalPrintService
        );

        Branch branch = new Branch();
        branch.setId(1L);
        Product product = new Product();
        product.setId(2L);
        product.setCode("P001");
        product.setService(false);

        SaleItem item = new SaleItem();
        item.setProduct(product);
        item.setProductCode("P001");
        item.setQtyAmount(BigDecimal.valueOf(2.0));
        item.setUnitPriceAmount(BigDecimal.valueOf(10.0));
        item.setLineBaseAmount(BigDecimal.valueOf(20.0));
        item.setLineIceAmount(BigDecimal.ZERO);
        item.setLineTaxAmount(BigDecimal.ZERO);
        item.setLineTotalAmount(BigDecimal.valueOf(20.0));

        Sale sale = new Sale();
        sale.setBranch(branch);
        sale.setSeries("A");
        sale.setDocumentType("VENDA");
        sale.setItems(java.util.List.of(item));

        StockBranch stockBranch = new StockBranch();
        stockBranch.setBranch(branch);
        stockBranch.setProduct(product);
        stockBranch.setStockCurrentAmount(BigDecimal.valueOf(5.0));

        when(numberingService.nextDocumentNumber(eq(1L), eq("A"), eq("VENDA"))).thenReturn(100L);
        when(stockBranchService.getCurrentStock(eq(branch), eq(product))).thenReturn(BigDecimal.valueOf(5.0));
        when(stockBranchService.decreaseStock(eq(branch), eq(product), eq(BigDecimal.valueOf(2.0)), anyString(), eq("VENDA"), any(User.class)))
                .thenAnswer(invocation -> {
                    stockBranch.setStockCurrentAmount(BigDecimal.valueOf(3.0));
                    return stockBranch;
                });
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(thermalPrintService.printReceipt(any(Sale.class))).thenReturn(new File("target/test-output.pdf"));

        User currentUser = new User();
        Role salesRole = new Role();
        salesRole.setName("CAIXA");
        salesRole.setPermissions(Set.of("VENDAS:CREATE"));
        currentUser.setRoles(Set.of(salesRole));

        File document = saleService.processAndSave(sale, currentUser);

        assertThat(document).isNotNull();
        assertThat(sale.getDocumentNumber()).isEqualTo(100L);
        assertThat(stockBranch.getStockCurrent()).isEqualTo(3.0);
        verify(saleRepository).save(any(Sale.class));
        verify(stockBranchService).decreaseStock(eq(branch), eq(product), eq(BigDecimal.valueOf(2.0)), anyString(), eq("VENDA"), eq(currentUser));
    }
}
