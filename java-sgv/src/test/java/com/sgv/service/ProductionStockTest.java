package com.sgv.service;

import com.sgv.entity.*;
import com.sgv.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * §19.10 — Produção/BOM: consumo de matérias-primas fiel e stock de MP
 * nunca negativo.
 *
 * Estado actual (antes das correções da auditoria):
 *  - production_consumesIngredientsCorrectly: PASSA (com MP suficiente o
 *    consumo é lançado no kardex).
 *  - production_insufficientMp_neverNegativeStock: FALHA (BUG-002 — o
 *    consumo corre dentro de um catch genérico ("Log and continue", na
 *    prática sem log) sem verificação de saldo: MP insuficiente → stock
 *    negativo gravado, produção "bem-sucedida" em silêncio).
 */
// Não é @Transactional de propósito: production_insufficientMp_neverNegativeStock
// captura a exception lançada pelo service e continua (verifica o estado pós-falha).
// Numa classe @Transactional essa exception marcava a transação partilhada como
// rollback-only e o commit final do framework lançava UnexpectedRollbackException,
// escondendo o resultado real do teste. Cada chamada de service/repositório commita
// sozinha; os códigos de entidade são únicos por teste (tags "Ok"/"Falta").
@SpringBootTest
@ActiveProfiles("test")
class ProductionStockTest {

    @Autowired private StockBranchService stockBranchService;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private BranchRepository branchRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private MetricUnitRepository metricUnitRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ProductRecipeRepository productRecipeRepository;
    @Autowired private StockBranchRepository stockBranchRepository;
    @Autowired private StockMovementRepository stockMovementRepository;

    private Branch b;
    private User op;

    private void setup(String tag) {
        Role r = new Role();
        r.setName("PROD-" + tag.toUpperCase());
        r.setDescription("Role produção");
        r.getPermissions().add("STOCK:CREATE");
        roleRepository.save(r);

        b = new Branch();
        b.setName("Filial PROD " + tag);
        b.setNuit("110" + tag.length() + "1101");
        b.setAddress("Rua PROD " + tag);
        b.setContact("841100000");
        branchRepository.save(b);

        op = new User();
        op.setUsername("prod-" + tag.toLowerCase().replace(" ", "-") + "-op");
        op.setPasswordHash("x");
        op.setRoles(java.util.Set.of(r));
        op.setBranch(b);
        userRepository.save(op);
    }

    private MetricUnit unit() {
        MetricUnit u = new MetricUnit();
        u.setAbbreviation("U" + (System.nanoTime() % 1_000_000));
        u.setDescription("Unidade produção");
        return metricUnitRepository.save(u);
    }

    private Product product(String code, String name, MetricUnit u) {
        Category c = new Category();
        c.setName("Cat PROD " + code);
        categoryRepository.save(c);
        Product p = new Product();
        p.setCode(code);
        p.setName(name);
        p.setCategory(c);
        p.setUnit(u);
        p.setPriceCost(10.0);
        p.setPriceSale(20.0);
        p.setService(false);
        p.setIsActive(true);
        return productRepository.save(p);
    }

    @Test
    void production_consumesIngredientsCorrectly() {
        setup("Ok");
        MetricUnit u = unit();
        Product finished = product("PRDOKF", "Acabado PROD Ok", u);
        Product mp = product("PRDOKM", "Matéria Prima Ok", u);

        // BOM: 1 unidade acabada consome 0.05 de MP
        ProductRecipe recipe = new ProductRecipe();
        recipe.setParentProduct(finished);
        recipe.setIngredientProduct(mp);
        recipe.setQuantityRequired(new BigDecimal("0.05"));
        recipe.setUnit("UN");
        productRecipeRepository.save(recipe);

        stockBranchService.initializeStock(b, mp, new BigDecimal("10"), BigDecimal.ZERO, BigDecimal.ZERO, "INIT", op);

        // produzir 100 → consumir 5
        stockBranchService.consumeIngredientsForProduction(b, finished, new BigDecimal("100"), "PROD-1", op);

        StockBranch after = stockBranchRepository.findByProductAndBranch(mp, b).orElseThrow();
        assertEquals(0, new BigDecimal("5.00").compareTo(after.getStockCurrentAmount()),
                "consumo correcto: 10 − (0.05×100) = 5");

        List<StockMovement> movements = stockMovementRepository
                .findByProductIdOrderByCreatedAtDesc(mp.getId()).stream()
                .filter(m -> b.getId().equals(m.getBranch() != null ? m.getBranch().getId() : -1L))
                .toList();
        assertTrue(movements.stream().anyMatch(m ->
                "SAIDA".equals(m.getType()) && "PROD_CONSUMO".equals(m.getSubtype())),
                "o consumo de MP deve deixar movimento SAIDA/PROD_CONSUMO no kardex");
    }

    @Test
    void production_insufficientMp_neverNegativeStock() {
        // BUG-002 — FALHA AGORA: MP=1, produção de 100 precisa de 5 →
        // hoje o método deixa stock = −4 sem lançar erro (catch vazio).
        // Comportamento correcto: bloquear/falhar atomicamente — MP nunca < 0.
        setup("Falta");
        MetricUnit u = unit();
        Product finished = product("PRDFALF", "Acabado PROD Falta", u);
        Product mp = product("PRDFALM", "Matéria Prima Falta", u);

        ProductRecipe recipe = new ProductRecipe();
        recipe.setParentProduct(finished);
        recipe.setIngredientProduct(mp);
        recipe.setQuantityRequired(new BigDecimal("0.05"));
        recipe.setUnit("UN");
        productRecipeRepository.save(recipe);

        stockBranchService.initializeStock(b, mp, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO, "INIT", op);

        try {
            stockBranchService.consumeIngredientsForProduction(b, finished, new BigDecimal("100"), "PROD-2", op);
        } catch (RuntimeException e) {
            // comportamento correcto após a correção: falha atómica com mensagem
            assertTrue(e.getMessage() != null && !e.getMessage().isBlank(),
                    "a falha por MP insuficiente deve ter mensagem explicativa");
        }

        StockBranch after = stockBranchRepository.findByProductAndBranch(mp, b).orElseThrow();
        assertTrue(after.getStockCurrentAmount().compareTo(BigDecimal.ZERO) >= 0,
                "BUG-002: stock de matéria-prima nunca pode ficar negativo — ficou "
                        + after.getStockCurrentAmount() + " (MP insuficiente, consumo engolido)");
    }
}
