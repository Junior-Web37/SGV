package com.sgv.repository;

import com.sgv.entity.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransferRepository extends JpaRepository<Transfer, Long> {

    @org.springframework.data.jpa.repository.Query("SELECT t FROM Transfer t LEFT JOIN FETCH t.items WHERE t.id = :id")
    Optional<Transfer> findByIdWithItems(@Param("id") Long id);

    List<Transfer> findByStatus(String status);

    @Query("SELECT t FROM Transfer t WHERE t.createdAt BETWEEN :start AND :end")
    List<Transfer> findByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT t FROM Transfer t WHERE t.createdAt BETWEEN :start AND :end AND (:status IS NULL OR t.status = :status)")
    List<Transfer> findByDateRangeAndStatus(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("status") String status);

    @Query("SELECT t FROM Transfer t WHERE " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(:branchId IS NULL OR t.sourceBranch.id = :branchId OR t.destinationBranch.id = :branchId)")
    List<Transfer> findByStatusAndBranch(@Param("status") String status, @Param("branchId") Long branchId);

    @Query("SELECT COALESCE(MAX(t.documentNumber), 0) FROM Transfer t " +
           "WHERE t.series = :series AND t.documentYear = :year AND t.sourceBranch.id = :branchId")
    Long findMaxDocumentNumberBySeriesAndYearAndBranch(
            @Param("series") String series,
            @Param("year") int year,
            @Param("branchId") Long branchId);
}
