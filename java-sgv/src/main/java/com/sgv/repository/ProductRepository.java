package com.sgv.repository;

import com.sgv.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByCode(String code);

    @Query("SELECT p FROM Product p WHERE "
           + "LOWER(p.code) LIKE LOWER(CONCAT('%', :search, '%')) OR "
           + "LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<Product> searchByCodeOrName(@Param("search") String search);

    @Query("SELECT p FROM Product p WHERE "
           + "LOWER(p.code) LIKE LOWER(CONCAT('%', :search, '%')) OR "
           + "LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR "
           + "LOWER(p.category.name) LIKE LOWER(CONCAT('%', :category, '%'))")
    List<Product> searchByCodeOrNameAndCategory(@Param("search") String search, @Param("category") String category);

    // Active = product has been set up (name is not blank) — no dedicated 'active' column in DB
    @Query("SELECT p FROM Product p WHERE p.name IS NOT NULL AND p.name <> ''")
    List<Product> findAllActive();
}
