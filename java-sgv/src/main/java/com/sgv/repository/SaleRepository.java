package com.sgv.repository;

import com.sgv.entity.Sale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    @Query("SELECT s FROM Sale s LEFT JOIN FETCH s.items WHERE s.id = :id")
    Sale findByIdWithItems(@Param("id") Long id);

    @Query("SELECT DISTINCT s FROM Sale s LEFT JOIN FETCH s.customer LEFT JOIN FETCH s.items ORDER BY s.createdAt DESC")
    List<Sale> findAllWithCustomerAndItems();

    @Query("SELECT s FROM Sale s LEFT JOIN FETCH s.customer LEFT JOIN FETCH s.branch WHERE "
            + "(:branchId IS NULL OR s.branch.id = :branchId) AND "
            + "(:state IS NULL OR :state = 'TODOS' OR s.state = :state) AND "
            + "(:docType IS NULL OR :docType = 'TODOS' OR s.documentType = :docType) AND "
            + "(:startDate IS NULL OR s.createdAt >= :startDate) AND "
            + "(:endDate IS NULL OR s.createdAt <= :endDate) AND "
            + "(:search IS NULL OR :search = '' OR "
            + " LOWER(COALESCE(s.customerName, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR "
            + " LOWER(COALESCE(s.customerNuit, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR "
            + " LOWER(COALESCE(s.series, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR "
            + " LOWER(COALESCE(s.documentType, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR "
            + " CAST(s.documentNumber AS string) LIKE CONCAT('%', :search, '%') OR "
            + " (s.customer IS NOT NULL AND LOWER(s.customer.name) LIKE LOWER(CONCAT('%', :search, '%')))) "
            + "ORDER BY s.createdAt DESC")
    List<Sale> searchForList(@Param("search") String search,
                             @Param("state") String state,
                             @Param("docType") String docType,
                             @Param("startDate") LocalDateTime startDate,
                             @Param("endDate") LocalDateTime endDate,
                             @Param("branchId") Long branchId,
                             Pageable pageable);

    @Query("select max(s.documentNumber) from Sale s where s.series = :series and s.documentType = :documentType and s.branch.id = :branchId and s.documentYear = :year")
    Long findMaxDocumentNumberBySeriesAndDocumentTypeAndBranchIdAndYear(
            @Param("series") String series,
            @Param("documentType") String documentType,
            @Param("branchId") Long branchId,
            @Param("year") int year);

    @Query("select s from Sale s where (:year is null or s.documentYear = :year) and (:month is null or MONTH(s.createdAt) = :month)")
    List<Sale> findAllByYearAndMonth(@Param("year") Integer year, @Param("month") Integer month);

    List<Sale> findAllByPendingSync(Boolean pendingSync);
    
    @Query("SELECT s FROM Sale s LEFT JOIN s.customer c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(s.customerNuit) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<Sale> searchByCustomerName(@Param("search") String search);
    
    @Query("SELECT s FROM Sale s WHERE s.state = :state")
    List<Sale> findByState(@Param("state") String state);
    
    @Query("SELECT s FROM Sale s WHERE " +
           "s.createdAt BETWEEN :startDate AND :endDate AND " +
           "(:state IS NULL OR s.state = :state)")
    List<Sale> findByDateRangeAndState(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("state") String state);

    @Query(value = "SELECT s FROM Sale s WHERE s.createdAt BETWEEN :startDate AND :endDate AND (:state IS NULL OR s.state = :state)",
           countQuery = "SELECT COUNT(s) FROM Sale s WHERE s.createdAt BETWEEN :startDate AND :endDate AND (:state IS NULL OR s.state = :state)")
    Page<Sale> findByDateRangeAndState(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("state") String state,
            Pageable pageable);

    @Query("SELECT s FROM Sale s WHERE s.state <> 'CANCELLED' ORDER BY s.createdAt DESC")
    List<Sale> findRecentNonCancelled(Pageable pageable);

    @Query("SELECT COALESCE(SUM(s.subtotal), 0.0) FROM Sale s WHERE s.createdAt IS NOT NULL")
    BigDecimal sumSubtotalAll();

    @Query("SELECT COALESCE(SUM(s.totalTax), 0.0) FROM Sale s WHERE s.createdAt IS NOT NULL")
    BigDecimal sumTotalTaxAll();

    /**
     * Correção do BUG-004: apenas o universo reportável — VENDA/FACTURA/NC
     * (NC entra com total negativo), não anulados e não demo. Cotações e
     * encomendas são documentos pré-venda e não podem somar nas receitas.
     */
    @Query("SELECT COALESCE(SUM(s.total), 0.0) FROM Sale s WHERE s.createdAt IS NOT NULL AND s.documentType IN ('VENDA', 'FACTURA', 'NC') AND s.state <> 'ANULADA' AND (s.demoFlag IS NULL OR s.demoFlag = false)")
    BigDecimal sumTotalAll();
    
    @Query("SELECT s FROM Sale s LEFT JOIN s.customer c WHERE " +
           "(LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(s.customerNuit) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "s.createdAt BETWEEN :startDate AND :endDate AND " +
           "(:state IS NULL OR s.state = :state)")
    List<Sale> findByCustomerAndDateRangeAndState(
            @Param("search") String search,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("state") String state);

    @Query("SELECT s FROM Sale s WHERE s.customer.id = :customerId ORDER BY s.createdAt DESC")
    List<Sale> findAllByCustomerId(@Param("customerId") Long customerId);

    /**
     * Pendências do cliente para a reconciliação FIFO. Correção do BUG-015/007:
     * apenas VENDA/FACTURA (antes a FIFO alocava pagamentos a cotações e
     * encomendas, que não são dívidas).
     */
    @Query("SELECT s FROM Sale s WHERE s.customer.id = :customerId AND s.documentType IN ('VENDA', 'FACTURA') AND s.state <> 'ANULADA' AND (COALESCE(s.total, 0.0) - COALESCE(s.paidAmount, 0.0)) > 0.01 ORDER BY s.createdAt ASC")
    List<Sale> findPendingByCustomerId(@Param("customerId") Long customerId);

    @Query("SELECT COUNT(s) FROM Sale s WHERE s.createdAt >= :start AND s.createdAt <= :end")
    long countByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(s) FROM Sale s WHERE s.createdAt >= :start AND s.createdAt <= :end AND s.state = :state")
    long countByDateRangeAndState(@Param("start") LocalDateTime start,
                                  @Param("end") LocalDateTime end,
                                  @Param("state") String state);

    @Query("SELECT COALESCE(SUM(s.total), 0.0) FROM Sale s WHERE s.createdAt >= :start AND s.createdAt <= :end AND s.state <> 'ANULADA'")
    BigDecimal sumTotalByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(s.total), 0.0) FROM Sale s WHERE s.createdAt >= :start AND s.createdAt <= :end AND s.state = :state")
    BigDecimal sumTotalByDateRangeAndState(@Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end,
                                       @Param("state") String state);

    /** Sequência controlada por filial para o hashHash AT */
    @Query("SELECT MAX(s.hashControl) FROM Sale s WHERE s.branch.id = :branchId")
    Long findMaxHashControlByBranchId(@Param("branchId") Long branchId);

    /**
     * Documentos com dívida pendente. Correção do BUG-007: apenas VENDA e
     * FACTURA — cotações/encomendas abertas são documentos pré-venda e NUNCA
     * devem aparecer na fila de recebimentos (antes eram "pagáveis" e
     * inflavam o KPI "Contas a Receber" e o aging de devedores).
     */
    @Query("SELECT s FROM Sale s WHERE s.documentType IN ('VENDA', 'FACTURA') AND s.state <> 'ANULADA' AND (COALESCE(s.total, 0.0) - COALESCE(s.paidAmount, 0.0)) > 0.01 ORDER BY s.createdAt DESC")
    List<Sale> findPendingSales();

    /**
     * Universo reportável (correção do BUG-004): VENDA/FACTURA/NC (NC entra
     * com total negativo), não anulados, não demo. Antes somava cotações,
     * encomendas e vendas demo, inflando o KPI de "Vendas de hoje" e o gráfico.
     */
    @Query("SELECT COALESCE(SUM(s.total), 0.0) FROM Sale s WHERE s.createdAt >= :startOfDay AND s.createdAt <= :endOfDay AND (:branchId IS NULL OR s.branch.id = :branchId) AND s.documentType IN ('VENDA', 'FACTURA', 'NC') AND s.state <> 'ANULADA' AND (s.demoFlag IS NULL OR s.demoFlag = false)")
    BigDecimal sumTotalByDateRangeAndBranch(@Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay, @Param("branchId") Long branchId);

    /** Universo reportável (correção do BUG-004) — ver sumTotalByDateRangeAndBranch. */
    @Query("SELECT COUNT(s) FROM Sale s WHERE s.createdAt >= :startOfDay AND s.createdAt <= :endOfDay AND (:branchId IS NULL OR s.branch.id = :branchId) AND s.documentType IN ('VENDA', 'FACTURA', 'NC') AND s.state <> 'ANULADA' AND (s.demoFlag IS NULL OR s.demoFlag = false)")
    long countByDateRangeAndBranch(@Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay, @Param("branchId") Long branchId);

    /**
     * Contas a receber pendentes (correção do BUG-004): apenas VENDA/FACTURA —
     * cotações/encomendas abertas não são dívidas e não podiam inflar o KPI
     * nem divergir da lista de pendências (findPendingSales, BUG-007).
     */
    @Query("SELECT COALESCE(SUM(s.total - COALESCE(s.paidAmount, 0.0)), 0.0) FROM Sale s WHERE s.documentType IN ('VENDA', 'FACTURA') AND s.state <> 'ANULADA' AND (s.demoFlag IS NULL OR s.demoFlag = false) AND (s.total - COALESCE(s.paidAmount, 0.0)) > 0.01 AND (:branchId IS NULL OR s.branch.id = :branchId)")
    BigDecimal sumTotalPendingCreditsByBranch(@Param("branchId") Long branchId);

    /** Universo reportável (correção do BUG-004) — ver sumTotalByDateRangeAndBranch. */
    @Query("SELECT s.paymentMethod, COALESCE(SUM(s.total), 0.0) FROM Sale s WHERE s.createdAt >= :startOfDay AND s.createdAt <= :endOfDay AND (:branchId IS NULL OR s.branch.id = :branchId) AND s.documentType IN ('VENDA', 'FACTURA', 'NC') AND s.state <> 'ANULADA' AND (s.demoFlag IS NULL OR s.demoFlag = false) GROUP BY s.paymentMethod")
    List<Object[]> sumTotalByPaymentMethodToday(@Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay, @Param("branchId") Long branchId);

    @Query("SELECT s FROM Sale s WHERE (:branchId IS NULL OR s.branch.id = :branchId) ORDER BY s.createdAt DESC")
    Page<Sale> findByBranchIdOrderByCreatedAtDesc(@Param("branchId") Long branchId, Pageable pageable);

    /**
     * Detecta possíveis duplicações: mesma filial, tipo, série, NUIT, nº de itens,
     * criadas dentro da janela de tempo (em segundos atrás).
     */
    @Query("SELECT s FROM Sale s WHERE s.branch.id = :branchId " +
           "AND s.documentType = :documentType " +
           "AND s.series = :series " +
           "AND s.customerNuit = :customerNuit " +
           "AND SIZE(s.items) = :itemCount " +
           "AND s.createdAt >= :since " +
           "AND s.state <> 'ANULADA' " +
           "ORDER BY s.createdAt DESC")
    List<Sale> findRecentDuplicates(@Param("branchId") Long branchId,
                                    @Param("documentType") String documentType,
                                    @Param("series") String series,
                                    @Param("customerNuit") String customerNuit,
                                    @Param("itemCount") int itemCount,
                                    @Param("since") LocalDateTime since);

    /** Universo reportável (correção do BUG-004) — ver sumTotalByDateRangeAndBranch. */
    @Query("SELECT MONTH(s.createdAt) as m, COALESCE(SUM(s.total), 0.0) " +
           "FROM Sale s WHERE s.documentYear = :year AND s.documentType IN ('VENDA', 'FACTURA', 'NC') AND s.state <> 'ANULADA' AND (s.demoFlag IS NULL OR s.demoFlag = false) " +
           "GROUP BY MONTH(s.createdAt)")
    List<Object[]> sumTotalByMonthAndYear(@Param("year") int year);
}
