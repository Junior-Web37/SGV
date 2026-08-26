package com.sgv.service;

import com.sgv.entity.Branch;
import com.sgv.entity.Product;
import com.sgv.entity.ProductionOrder;
import com.sgv.entity.User;
import com.sgv.repository.ProductionOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Orquestração atómica de produção (correção do BUG-002 / BUG-041 parcial).
 *
 * Antes: o controller chamava 3 operações em 3 transacções separadas
 * (repo.save → increaseStock → consumeIngredientsForProduction) e o consumo
 * engolia todas as excepções → a ordem ficava COMPLETED com stock do
 * acabado entrado e matérias-primas sem consumir (ou negativas), em silêncio.
 *
 * Agora: consumo de MP (com bloqueio por MP insuficiente) + entrada do
 * acabado + gravação da ordem correm num ÚNICO transaction boundary —
 * qualquer falha reverte tudo.
 */
@Service
public class ProductionService {

    private static final Logger log = LoggerFactory.getLogger(ProductionService.class);

    private final ProductionOrderRepository productionOrderRepository;
    private final StockBranchService stockBranchService;

    public ProductionService(ProductionOrderRepository productionOrderRepository,
                             StockBranchService stockBranchService) {
        this.productionOrderRepository = productionOrderRepository;
        this.stockBranchService = stockBranchService;
    }

    /**
     * Executa a produção completa de uma ordem nova:
     *  1. valida a disponibilidade de matérias-primas (lança com a lista de faltas);
     *  2. consome as MP (kardex SAIDA/PROD_CONSUMO);
     *  3. entra o produto acabado (kardex ENTRADA/PRODUCAO);
     *  4. grava a ordem COMPLETED.
     *
     * @param order  ordem nova (id == null) com product/quantity/orderNumber preenchidos
     * @param branch filial onde a produção acontece
     * @param user   operador responsável
     * @return ordem gravada
     * @throws IllegalStateException se as matérias-primas forem insuficientes
     */
    @Transactional
    public ProductionOrder produce(ProductionOrder order, Branch branch, User user) {
        if (order == null) throw new IllegalArgumentException("Ordem de produção é obrigatória.");
        if (order.getId() != null) throw new IllegalArgumentException("produce() é exclusiva para ordens novas; use save() para editar.");
        Product finished = order.getProduct();
        BigDecimal qty = order.getQuantityAmount();
        if (finished == null) throw new IllegalStateException("Produto acabado é obrigatório.");
        if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Quantidade a produzir deve ser maior que zero.");
        }
        if (branch == null || branch.getId() == null) {
            throw new IllegalStateException("Filial de produção é obrigatória (operador sem filial afecta).");
        }
        if (Boolean.TRUE.equals(finished.getService())) {
            throw new IllegalStateException("Serviços não podem ser produzidos: " + finished.getCode());
        }

        String ref = "PROD-" + (order.getOrderNumber() != null ? order.getOrderNumber() : finished.getCode());

        // 1) + 2) consumo de MP — valida e consome; lança (com lista de faltas)
        // se não houver matéria-prima suficiente. Sem catch: falha reverte tudo.
        stockBranchService.consumeIngredientsForProduction(branch, finished, qty, ref, user);

        // 3) entrada do produto acabado
        stockBranchService.increaseStock(branch, finished, qty, ref, "PRODUCAO", user);

        // 4) ordem COMPLETED
        order.setState("COMPLETED");
        order.setCompletedAt(LocalDateTime.now());
        if (order.getCreatedAt() == null) {
            order.setCreatedAt(LocalDateTime.now());
        }
        ProductionOrder saved = productionOrderRepository.save(order);
        log.info("Produção {} concluída: {} de {} na filial {}",
                saved.getOrderNumber(), qty, finished.getName(), branch.getName());
        return saved;
    }

    /**
     * Lista (com quantidades) as faltas de matéria-prima para produzir
     * {@code producedQty} unidades — usada pela UI para avisar antes de gravar.
     */
    public List<String> listIngredientShortages(Branch branch, Product finishedProduct, BigDecimal producedQty) {
        return stockBranchService.listIngredientShortages(branch, finishedProduct, producedQty);
    }
}
