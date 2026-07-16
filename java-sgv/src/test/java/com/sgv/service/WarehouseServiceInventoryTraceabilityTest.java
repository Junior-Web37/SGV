package com.sgv.service;

import com.sgv.entity.Product;
import com.sgv.entity.StockMovement;
import com.sgv.entity.StockWarehouse;
import com.sgv.entity.Warehouse;
import com.sgv.repository.ProductRepository;
import com.sgv.repository.StockMovementRepository;
import com.sgv.repository.StockWarehouseRepository;
import com.sgv.repository.WarehouseRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:inventory-traceability-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class WarehouseServiceInventoryTraceabilityTest {

    @Autowired
    private WarehouseService warehouseService;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StockMovementRepository stockMovementRepository;

    @Autowired
    private StockWarehouseRepository stockWarehouseRepository;

    @Test
    void addAndRemoveStockShouldCreateTraceableMovementsWithPricing() {
        Warehouse warehouse = new Warehouse();
        warehouse.setCode("WH-TRC");
        warehouse.setName("Armazém de rastreio");
        warehouse.setActive(true);
        warehouse = warehouseRepository.save(warehouse);

        Product product = new Product();
        product.setCode("P-TRACE");
        product.setName("Produto de rastreio");
        product.setPriceCost(10.5);
        product.setPriceSale(15.75);
        product = productRepository.save(product);

        StockWarehouse created = warehouseService.addStock(warehouse.getId(), product.getId(), 5.0, "REF-ENTRY", null);
        assertThat(created.getStockCurrent()).isEqualTo(5.0);

        List<StockMovement> movements = stockMovementRepository.findByProductIdOrderByCreatedAtDesc(product.getId());
        assertThat(movements).hasSize(1);
        assertThat(movements.get(0).getUnitCostPrice()).isEqualTo(10.5);
        assertThat(movements.get(0).getUnitSalePrice()).isEqualTo(15.75);
        assertThat(movements.get(0).getNotes()).isEqualTo("Entrada de stock");

        warehouseService.removeStock(warehouse.getId(), product.getId(), 2.0, "REF-EXIT", null);

        movements = stockMovementRepository.findByProductIdOrderByCreatedAtDesc(product.getId());
        assertThat(movements).hasSize(2);
        assertThat(movements.get(0).getQty()).isEqualTo(-2.0);
        assertThat(movements.get(0).getNotes()).isEqualTo("Saída de stock");
        assertThat(stockWarehouseRepository.findByWarehouseIdAndProductId(warehouse.getId(), product.getId()).orElseThrow().getStockCurrent()).isEqualTo(3.0);
    }
}
