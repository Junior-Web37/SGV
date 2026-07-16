package com.sgv.service;

import com.sgv.entity.Role;
import com.sgv.entity.Sale;
import com.sgv.entity.User;
import com.sgv.repository.CashMovementRepository;
import com.sgv.repository.CashSessionRepository;
import com.sgv.repository.SaleItemRepository;
import com.sgv.repository.SaleRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class BusinessPermissionEnforcementTest {

    @Test
    void shouldRejectSaleCreationWithoutVendasCreatePermission() {
        SaleRepository saleRepository = mock(SaleRepository.class);
        SaleItemRepository saleItemRepository = mock(SaleItemRepository.class);
        StockBranchService stockBranchService = mock(StockBranchService.class);
        SaleNumberingService saleNumberingService = mock(SaleNumberingService.class);
        SaleDocumentService saleDocumentService = mock(SaleDocumentService.class);
        ThermalPrintService thermalPrintService = mock(ThermalPrintService.class);

        SaleService service = new SaleService(
                saleRepository,
                saleItemRepository,
                stockBranchService,
                saleNumberingService,
                saleDocumentService,
                thermalPrintService
        );

        User user = new User();
        user.setUsername("caixa");
        Role role = new Role();
        role.setName("CAIXA");
        role.setPermissions(Set.of("VENDAS:VIEW"));
        user.setRoles(Set.of(role));

        Sale sale = new Sale();
        sale.setItems(new ArrayList<>());

        assertThrows(IllegalStateException.class, () -> service.processAndSave(sale, user));
    }

    @Test
    void shouldRejectCashSessionOpeningWithoutCaixaPermission() {
        CashSessionRepository repository = mock(CashSessionRepository.class);
        CashMovementRepository movementRepository = mock(CashMovementRepository.class);
        CashSessionService service = new CashSessionService(repository, movementRepository);

        User user = new User();
        user.setUsername("gestor");
        Role role = new Role();
        role.setName("GESTOR");
        role.setPermissions(Set.of("RESUMO:VIEW"));
        user.setRoles(Set.of(role));

        assertThrows(IllegalStateException.class, () -> service.openSession(user, BigDecimal.TEN));
    }
}
