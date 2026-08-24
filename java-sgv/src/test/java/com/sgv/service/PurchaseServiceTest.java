package com.sgv.service;

import com.sgv.entity.Purchase;
import com.sgv.entity.Warehouse;
import com.sgv.repository.ProductRepository;
import com.sgv.repository.PurchaseRepository;
import com.sgv.repository.WarehouseRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PurchaseServiceTest {

    @Test
    void savePurchase_withNullItems_savesPurchaseWithoutThrowing() {
        PurchaseRepository purchaseRepository = mock(PurchaseRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        WarehouseRepository warehouseRepository = mock(WarehouseRepository.class);
        WarehouseService warehouseService = mock(WarehouseService.class);
        SystemLogService systemLogService = mock(SystemLogService.class);

        PurchaseService service = new PurchaseService(
                purchaseRepository,
                productRepository,
                warehouseRepository,
                warehouseService,
                systemLogService
        );

        Purchase purchase = new Purchase();
        purchase.setItems(null);

        Warehouse warehouse = new Warehouse();
        warehouse.setId(10L);
        warehouse.setName("Armazém Principal");
        when(warehouseRepository.findByIsActiveTrueOrderByNameAsc()).thenReturn(List.of(warehouse));
        when(purchaseRepository.save(any(Purchase.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Purchase saved = service.savePurchase(purchase, null, false, null);

        assertNotNull(saved);
        assertNotNull(saved.getItems());
        assertTrue(saved.getItems().isEmpty());
        assertEquals(0.0, saved.getSubtotal());
        assertEquals(0.0, saved.getTotal());
        assertNotNull(saved.getTargetWarehouse());
        assertEquals(10L, saved.getTargetWarehouse().getId());
    }
}
