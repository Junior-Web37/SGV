package com.sgv.repository;

import com.sgv.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByCategoryOrderByCreatedAtDesc(String category);
    List<AuditLog> findAllByOrderByCreatedAtDesc();
}
