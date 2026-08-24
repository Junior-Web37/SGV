package com.sgv.repository;

import com.sgv.entity.CashMovement;
import com.sgv.entity.CashSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CashMovementRepository extends JpaRepository<CashMovement, Long> {

    List<CashMovement> findBySessionOrderByCreatedAtAsc(CashSession session);
    List<CashMovement> findBySessionOrderByCreatedAtDesc(CashSession session);
}
