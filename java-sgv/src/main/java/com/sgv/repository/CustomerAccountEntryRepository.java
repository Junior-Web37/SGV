package com.sgv.repository;

import com.sgv.entity.CustomerAccountEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface CustomerAccountEntryRepository extends JpaRepository<CustomerAccountEntry, Long> {

    List<CustomerAccountEntry> findByCustomerIdOrderByCreatedAtAsc(Long customerId);

    long countByCustomerId(Long customerId);

    /** Σ dos lançamentos do cliente (equivale ao saldo corrente, pela invariante do livro). */
    @Query("select coalesce(sum(e.amount), 0) from CustomerAccountEntry e where e.customerId = :customerId")
    BigDecimal sumAmountByCustomerId(@Param("customerId") Long customerId);
}
