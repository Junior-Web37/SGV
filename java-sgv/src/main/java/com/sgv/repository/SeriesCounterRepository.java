package com.sgv.repository;

import com.sgv.entity.SeriesCounter;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SeriesCounterRepository extends JpaRepository<SeriesCounter, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT sc FROM SeriesCounter sc WHERE (:branchId IS NULL AND sc.branchId IS NULL OR sc.branchId = :branchId) AND sc.series = :series AND sc.documentType = :documentType")
    Optional<SeriesCounter> findForUpdate(@Param("branchId") Long branchId, @Param("series") String series, @Param("documentType") String documentType);

    Optional<SeriesCounter> findByBranchIdAndSeriesAndDocumentType(Long branchId, String series, String documentType);
}
