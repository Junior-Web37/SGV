package com.sgv.repository;

import com.sgv.entity.MetricUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MetricUnitRepository extends JpaRepository<MetricUnit, Long> {
    boolean existsByAbbreviation(String abbreviation);
    java.util.Optional<MetricUnit> findByAbbreviation(String abbreviation);
}
