package com.sgv.repository;

import com.sgv.entity.SystemBackup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SystemBackupRepository extends JpaRepository<SystemBackup, Long> {
    
    Optional<SystemBackup> findByFileName(String fileName);
    
    List<SystemBackup> findAllByOrderByCreatedAtDesc();
    
    @Query("SELECT b FROM SystemBackup b WHERE b.createdAt >= :start AND b.createdAt <= :end ORDER BY b.createdAt DESC")
    List<SystemBackup> findByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    @Query("SELECT b FROM SystemBackup b WHERE b.backupType = :type ORDER BY b.createdAt DESC")
    List<SystemBackup> findByBackupType(@Param("type") String type);
    
    @Query("SELECT b FROM SystemBackup b WHERE b.status = :status ORDER BY b.createdAt DESC")
    List<SystemBackup> findByStatus(@Param("status") String status);
    
    @Query("SELECT COUNT(b) FROM SystemBackup b")
    long countAll();
    
    @Query("SELECT COUNT(b) FROM SystemBackup b WHERE b.backupType = :type")
    long countByType(@Param("type") String type);
    
    @Query("SELECT b FROM SystemBackup b ORDER BY b.createdAt DESC LIMIT 1")
    Optional<SystemBackup> findLatest();
}
