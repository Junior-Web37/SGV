package com.sgv.service;

import com.sgv.entity.*;
import com.sgv.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Teste de integração do fluxo completo de stock:
 *   Compra (fornecedor) → entrada no Armazém central → transferência para a Loja (filial) → consumo na venda.
 * Usa H2 in-memory (perfil "test"), contexto Spring real, dados rolados para trás por transação.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class StockFlowIntegrationTest {

    @Autowired private WarehouseService warehouseService;
    @Autowired private WarehouseTransferService warehouseTransferService;
    @Autowired private StockBranchService stockBranchService;
    @Autowired private WarehouseRepository warehouseRepository;
    @Autowired private BranchRepository branchRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private MetricUnitRepository metricUnitRepository;
    @Autowired private StockWarehouseRepository stockWarehouseRepository;
    @Autowired private StockBranchRepository stockBranchRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;

    @Test
    void comprasEntramArmazem_transferenciaMoveParaLoja_vendaConsome() {
        // -- Dados base ------------------------------------------------
        Role role = new Role();
        role.setName("TEST");
        role.setDescription("Role de teste do fluxo de stock");
        role.getPermissions().add("TRANSFERENCIAS:CREATE");
        role.getPermissions().add("TRANSFERENCIAS:VIEW");
        roleRepository.save(role);

        User user = new User();
        user.setUsername("flow-user");
        user.setPasswordHash("x");
        user.getRoles().add(role);
        userRepository.save(user);

        Branch branch = new Branch();
        branch.setName("Filial Teste");
        branch.setNuit("123456789");
        branch.setAddress("Av. Teste");
        branch.setContact("841234567");
        branchRepository.save(branch);

        Category category = new Category();
        category.setName("Categoria Teste");
        categoryRepository.save(category);

        MetricUnit unit = new MetricUnit();
        unit.setAbbreviation("UN");
        unit.setDescription("Unidade");
        metricUnitRepository.save(unit);

        Product product = new Product();
        product.setCode("T001");
        product.setName("Produto Teste");
        product.setCategory(category);
        product.setUnit(unit);
        product.setPriceCost(100.0);
        product.setPriceSale(150.0);
        product.setService(false);
        product.setIsActive(true);
        productRepository.save(product);

        Warehouse warehouse = new Warehouse();
        warehouse.setCode("WH1");
        warehouse.setName("Armazém Central");
        warehouse.setAddress("Zona Industrial");
        warehouse.setActive(true);
        warehouseRepository.save(warehouse);

        // -- 1. COMPRA: entrada de 100 unidades no armazém central ------
        warehouseService.addStock(warehouse.getId(), product.getId(), 100, "FAC-001", user);

        StockWarehouse sw = stockWarehouseRepository
                .findByWarehouseIdAndProductId(warehouse.getId(), product.getId())
                .orElseThrow(() -> new AssertionError("Stock do armazém não criado"));
        assertEquals(0, new BigDecimal("100").compareTo(sw.getStockCurrentAmount()),
                "Após a compra o armazém deve ter 100 unidades");

        // -- 2. TRANSFERÊNCIA: 40 unidades do armazém para a loja -------
        WarehouseTransfer draft = new WarehouseTransfer();
        draft.setWarehouse(warehouse);
        draft.setBranch(branch);
        WarehouseTransferItem item = new WarehouseTransferItem();
        item.setProduct(product);
        item.setQuantityAmount(new BigDecimal("40"));
        draft.getItems().add(item);

        WarehouseTransfer created = warehouseTransferService.create(draft, user);
        assertNotNull(created.getId());
        assertEquals("PENDING", created.getStatus());

        warehouseTransferService.approve(created.getId(), user);
        WarehouseTransfer completed = warehouseTransferService.complete(created.getId(), user);
        assertEquals("COMPLETED", completed.getStatus());

        StockWarehouse afterTransfer = stockWarehouseRepository
                .findByWarehouseIdAndProductId(warehouse.getId(), product.getId())
                .orElseThrow();
        assertEquals(0, new BigDecimal("60").compareTo(afterTransfer.getStockCurrentAmount()),
                "Após transferência o armazém deve ter 60 unidades (100-40)");

        StockBranch sb = stockBranchRepository.findByProductAndBranch(product, branch)
                .orElseThrow(() -> new AssertionError("Stock da filial não criado"));
        assertEquals(0, new BigDecimal("40").compareTo(sb.getStockCurrentAmount()),
                "Após transferência a loja deve ter 40 unidades");

        // -- 3. VENDA: consumir 10 unidades na loja ----------------------
        stockBranchService.decreaseStock(branch, product, new BigDecimal("10"), "VENDA-001", "VENDA", user);

        StockBranch afterSale = stockBranchRepository.findByProductAndBranch(product, branch)
                .orElseThrow();
        assertEquals(0, new BigDecimal("30").compareTo(afterSale.getStockCurrentAmount()),
                "Após a venda a loja deve ter 30 unidades (40-10)");
        assertEquals(0, new BigDecimal("60").compareTo(afterTransfer.getStockCurrentAmount()),
                "A venda não pode afetar o stock do armazém");
    }

    @Test
    void transferenciaSemStockNoArmazem_naoPodeSerCriada() {
        Role role = new Role();
        role.setName("TEST2");
        role.getPermissions().add("TRANSFERENCIAS:CREATE");
        role.getPermissions().add("TRANSFERENCIAS:VIEW");
        roleRepository.save(role);

        User user = new User();
        user.setUsername("flow-user-2");
        user.setPasswordHash("x");
        user.getRoles().add(role);
        userRepository.save(user);

        Branch branch = new Branch();
        branch.setName("Filial 2");
        branch.setNuit("987654321");
        branch.setContact("841111111");
        branchRepository.save(branch);

        Warehouse warehouse = new Warehouse();
        warehouse.setCode("WH2");
        warehouse.setName("Armazém Vazio");
        warehouse.setActive(true);
        warehouseRepository.save(warehouse);

        Category category = new Category();
        category.setName("Categoria 2");
        categoryRepository.save(category);

        MetricUnit unit = new MetricUnit();
        unit.setAbbreviation("PC");
        unit.setDescription("Peça");
        metricUnitRepository.save(unit);

        Product product = new Product();
        product.setCode("T002");
        product.setName("Produto Sem Stock");
        product.setCategory(category);
        product.setUnit(unit);
        product.setPriceCost(50.0);
        product.setPriceSale(80.0);
        product.setService(false);
        product.setIsActive(true);
        productRepository.save(product);

        WarehouseTransfer draft = new WarehouseTransfer();
        draft.setWarehouse(warehouse);
        draft.setBranch(branch);
        WarehouseTransferItem item = new WarehouseTransferItem();
        item.setProduct(product);
        item.setQuantityAmount(new BigDecimal("5"));
        draft.getItems().add(item);

        assertThrows(IllegalStateException.class,
                () -> warehouseTransferService.create(draft, user),
                "Não pode criar transferência sem stock no armazém");
    }
}
