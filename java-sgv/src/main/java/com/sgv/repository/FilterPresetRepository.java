package com.sgv.repository;

import com.sgv.entity.FilterPreset;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FilterPresetRepository extends JpaRepository<FilterPreset, Long> {
    List<FilterPreset> findByUserIdAndTypeOrderByCreatedAtDesc(Long userId, String type);
    Optional<FilterPreset> findByUserIdAndTypeAndName(Long userId, String type, String name);
}
