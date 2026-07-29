package com.sgv.repository;

import com.sgv.entity.Expense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    Page<Expense> findAll(Pageable pageable);

    List<Expense> findByBranchIdOrderByCreatedAtDesc(Long branchId);
    List<Expense> findByStateOrderByDueDateAsc(String state);
    List<Expense> findAllByOrderByCreatedAtDesc();

    @Query("SELECT e FROM Expense e WHERE " +
           "LOWER(e.description) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(e.category) LIKE LOWER(CONCAT('%',:q,'%'))")
    List<Expense> searchByDescriptionOrCategory(@Param("q") String query);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e " +
           "WHERE e.dueDate >= :from AND e.dueDate < :to AND e.state IN :states")
    Double sumAmountByDateRangeAndStates(@Param("from") LocalDate from,
                                        @Param("to") LocalDate to,
                                        @Param("states") List<String> states);
}
