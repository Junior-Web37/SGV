package com.sgv.repository;

import com.sgv.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BranchRepository extends JpaRepository<Branch, Long> {
    Optional<Branch> findByNameIgnoreCase(String name);
}
