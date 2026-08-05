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

    @Query("SELECT s FROM Sale s WHERE s.state != 'CANCELLED' ORDER BY s.createdAt DESC")
    List<Sale> findRecentNonCancelled(Pageable pageable);

    @Query("SELECT COALESCE(SUM(s.subtotal), 0) FROM Sale s WHERE s.createdAt IS NOT NULL")
    BigDecimal sumSubtotalAll();

    @Query("SELECT COALESCE(SUM(s.totalTax), 0) FROM Sale s WHERE s.createdAt IS NOT NULL")
    BigDecimal sumTotalTaxAll();

    @Query("SELECT COALESCE(SUM(s.total), 0) FROM Sale s WHERE s.createdAt IS NOT NULL")
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

    @Query("SELECT s FROM Sale s WHERE s.customer.id = :customerId AND s.state = 'EMITIDA' ORDER BY s.createdAt ASC")
    List<Sale> findPendingByCustomerId(@Param("customerId") Long customerId);

    @Query("SELECT COUNT(s) FROM Sale s WHERE s.createdAt >= :start AND s.createdAt <= :end")
    long countByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(s) FROM Sale s WHERE s.createdAt >= :start AND s.createdAt <= :end AND s.state = :state")
    long countByDateRangeAndState(@Param("start") LocalDateTime start,
                                  @Param("end") LocalDateTime end,
                                  @Param("state") String state);

    @Query("SELECT COALESCE(SUM(s.total), 0) FROM Sale s WHERE s.createdAt >= :start AND s.createdAt <= :end AND s.state != 'ANULADA'")
    BigDecimal sumTotalByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(s.total), 0) FROM Sale s WHERE s.createdAt >= :start AND s.createdAt <= :end AND s.state = :state")
    BigDecimal sumTotalByDateRangeAndState(@Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end,
                                       @Param("state") String state);

    /** Sequência controlada por filial para o hashHash AT */
    @Query("SELECT MAX(s.hashControl) FROM Sale s WHERE s.branch.id = :branchId")
    Long findMaxHashControlByBranchId(@Param("branchId") Long branchId);

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
           "AND s.state != 'ANULADA' " +
           "ORDER BY s.createdAt DESC")
    List<Sale> findRecentDuplicates(@Param("branchId") Long branchId,
                                    @Param("documentType") String documentType,
                                    @Param("series") String series,
                                    @Param("customerNuit") String customerNuit,
                                    @Param("itemCount") int itemCount,
                                    @Param("since") LocalDateTime since);

    @Query("SELECT MONTH(s.createdAt) as m, COALESCE(SUM(s.total), 0) " +
           "FROM Sale s WHERE s.documentYear = :year AND s.state != 'ANULADA' " +
           "GROUP BY MONTH(s.createdAt)")
    List<Object[]> sumTotalByMonthAndYear(@Param("year") int year);
}
