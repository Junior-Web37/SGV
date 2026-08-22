package com.sgv.repository;

import com.sgv.entity.WarehouseTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WarehouseTransferRepository extends JpaRepository<WarehouseTransfer, Long> {

    @Query("SELECT MAX(t.documentNumber) FROM WarehouseTransfer t WHERE t.series = :series AND t.documentYear = :year")
    Long findMaxDocumentNumberBySeriesAndYear(@Param("series") String series, @Param("year") int year);

    @Query("SELECT DISTINCT t FROM WarehouseTransfer t LEFT JOIN FETCH t.items i LEFT JOIN FETCH i.product WHERE t.id = :id")
    java.util.Optional<WarehouseTransfer> findByIdWithItems(@Param("id") Long id);

    List<WarehouseTransfer> findAllByOrderByCreatedAtDesc();
    List<WarehouseTransfer> findByStatusOrderByCreatedAtDesc(String status);
}
