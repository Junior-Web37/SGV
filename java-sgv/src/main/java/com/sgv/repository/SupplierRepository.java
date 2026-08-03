package com.sgv.repository;

import com.sgv.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    Optional<Supplier> findByNameIgnoreCase(String name);
    List<Supplier> findAllByOrderByNameAsc();
    List<Supplier> findByNameContainingIgnoreCaseOrNuitContainingOrContactContaining(String name, String nuit, String contact);
}
