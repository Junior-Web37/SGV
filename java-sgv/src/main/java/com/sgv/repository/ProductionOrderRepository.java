package com.sgv.repository;

import com.sgv.entity.ProductionOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductionOrderRepository extends JpaRepository<ProductionOrder, Long> {

    @Query("SELECT o FROM ProductionOrder o WHERE " +
           "LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(o.product.name) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<ProductionOrder> searchByNumberOrProduct(@Param("q") String query);

    List<ProductionOrder> findAllByOrderByCreatedAtDesc();
}
